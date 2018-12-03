package com.checkmobi;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.checkmobi.sdk.AsyncResponse;
import com.checkmobi.sdk.CheckMobiService;
import com.checkmobi.sdk.ErrorCode;
import com.checkmobi.sdk.ValidationType;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    final private int REQUEST_CALLING_PERMISSIONS = 123;
    
    private WeakReference<ProgressDialog> loadingDialog;
    
    private EditText phoneNumberEditText;
    private EditText pinEditText;
    private TextView chargeLabel;
    private Button validationTypeButton;
    private Button resetButton;
    private Button validateButton;
    
    private final String[] validationTypes = new String[]{"CLI", "SMS", "IVR", "Reverse CLI (Missed call)"};
    private int currentTypeIndex = 0;
    
    private String callId;
    private String dialingNumber;
    private String validationKey;
    private boolean pinStep = false;
    private ValidationType validation_type;
    
    //reverse cli validation
    
    private String callerid_hash;
    private Timer timer;
    private TimerTask timerTask;
    private final Handler handler = new Handler();
    
    private final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(CallReceiver.MSG_CALL_START))
            {
                if (!intent.getBooleanExtra("incoming", false))
                {
                    if (MainActivity.this.dialingNumber == null || MainActivity.this.validationKey == null || MainActivity.this.callId != null)
                        return;
                    
                    MainActivity.this.callId = intent.getStringExtra("number");
                }
                else
                {
                    String number = intent.getStringExtra("number");
                    
                    //on some devices the number is in local format for local number.
                    
                    boolean matching = false;
                    
                    try
                    {
                        String hash = AeSimpleSHA1.SHA1(number.substring(number.length() - 3));
                        
                        if (hash.equals(callerid_hash))
                            matching = true;
                    }
                    catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }
                    
                    System.out.println("Incoming call received: " + number + " hash: " + callerid_hash + " matching: " + matching);
                    
                    if (matching)
                    {
                        StopReverseCliTimer();
                        HangupCall();
                        
                        //check validation
                        
                        String pinNumber = number.substring(number.length() - 4);
                        
                        CheckMobiService.getInstance().VerifyPin(MainActivity.this.validation_type, MainActivity.this.validationKey, pinNumber, new AsyncResponse()
                        {
                            @Override
                            public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                            {
                                ShowLoadingMessage(false);
                                
                                if (httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                                {
                                    Boolean validated = (Boolean) result.get("validated");
                                    
                                    if (!validated)
                                    {
                                        Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Validation failed!");
                                        return;
                                    }
                                    
                                    String message = "Validation completed for: " + phoneNumberEditText.getText().toString();
                                    Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Validation completed", message);
                                    OnClickReset();
                                }
                                else
                                {
                                    HandleValidationServiceError(httpStatus, result, error);
                                }
                            }
                        });
                        
                    }
                    else if (Settings.getInstance().isCloseCallForFailedAni())
                    {
                        StopReverseCliTimer();
                        
                        if (Settings.getInstance().isUseClientHangup())
                        {
                            HangupCall();
                        }
                        else
                        {
                            CheckMobiService.getInstance().HangupCall(MainActivity.this.validationKey, new AsyncResponse()
                            {
                                @Override
                                public void OnRequestCompleted(int httpStatus, Map<String, Object> responseMap, String error)
                                {
                                    if (httpStatus != CheckMobiService.STATUS_SUCCESS_NO_CONTENT)
                                        System.out.println("Failed to close call on the server side. body= " + error + " error= " + error);
                                }
                            });
                        }
                        
                        ShowLoadingMessage(false);
                        Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Validation failed!");
                    }
                    
                }
            }
            if (intent.getAction().equals(CallReceiver.MSG_CALL_END))
            {
                if (!intent.getBooleanExtra("incoming", false))
                {
                    MainActivity pThis = MainActivity.this;
                    
                    if (pThis.dialingNumber == null || pThis.validationKey == null || pThis.callId == null)
                        return;
                    
                    if (pThis.callId.compareTo(intent.getStringExtra("number")) != 0)
                        return;
                    
                    DismissKeyboard();
                    ShowLoadingMessage(true);
                    
                    CheckMobiService.getInstance().CheckValidationStatus(pThis.validationKey, new AsyncResponse()
                    {
                        @Override
                        public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                        {
                            ShowLoadingMessage(false);
                            
                            if (httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                            {
                                Boolean validated = (Boolean) result.get("validated");
                                
                                if (!validated)
                                {
                                    Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Number not validated ! Check your phone number!");
                                    return;
                                }
                                
                                String message = "Validation completed for: " + phoneNumberEditText.getText().toString();
                                Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Validation completed", message);
                                OnClickReset();
                            }
                            else
                            {
                                HandleValidationServiceError(httpStatus, result, error);
                            }
                        }
                    });
                    
                }
            }
        }
    };
    
    private void StopReverseCliTimer()
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
            timerTask = null;
        }
    }
    
    private void HangupCall()
    {
        if (!Settings.getInstance().isUseClientHangup())
            return;
        
        try
        {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            Object telephonyService = m.invoke(tm);
            c = Class.forName(telephonyService.getClass().getName());
            m = c.getDeclaredMethod("endCall");
            m.setAccessible(true);
            m.invoke(telephonyService);
        }
        catch (Exception ex)
        {
            System.out.println("Failed to hangup the call...:" + ex.getMessage());
        }
    }
    
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    
    private void CheckPermissions()
    {
        boolean has_call_phone_permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        boolean has_call_log_permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean has_read_phone_permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        if (!has_call_phone_permission || !has_call_log_permission || !has_read_phone_permission)
        {
            boolean denied_call_phone_permission = !has_call_phone_permission && !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CALL_PHONE);
            boolean denied_call_log_permission = !has_call_log_permission && !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_CALL_LOG);
            boolean denied_read_phone_permission = !has_read_phone_permission && !ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE);
            if (denied_call_phone_permission || denied_call_log_permission || denied_read_phone_permission)
            {
                showMessageOKCancel("You need to allow access to calling permissions", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE},
                                REQUEST_CALLING_PERMISSIONS);
                    }
                });
                return;
            }
            
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE}, REQUEST_CALLING_PERMISSIONS);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        this.phoneNumberEditText = (EditText) findViewById(R.id.phoneNrTextEdit);
        this.pinEditText = (EditText) findViewById(R.id.pinTextEdit);
        this.chargeLabel = (TextView) findViewById(R.id.chargeLabel);
        this.validationTypeButton = (Button) findViewById(R.id.validationTypeBtt);
        this.resetButton = (Button) findViewById(R.id.resetBtt);
        this.validateButton = (Button) findViewById(R.id.validateBtt);
        
        this.validationTypeButton.setOnClickListener(this);
        this.resetButton.setOnClickListener(this);
        this.validateButton.setOnClickListener(this);
        
        registerReceiver(receiver, new IntentFilter(CallReceiver.MSG_CALL_START));
        registerReceiver(receiver, new IntentFilter(CallReceiver.MSG_CALL_END));
        
        CheckMobiService.getInstance().SetBaseUrl(Settings.getInstance().getBaseUrl());
        CheckMobiService.getInstance().SetSecretKey(Settings.getInstance().getSecretKey());
        CheckMobiService.getInstance().SetUseServerHangup(!Settings.getInstance().isUseClientHangup());
        
        CheckPermissions();
        
        RefreshGUI();
    }
    
    protected void onDestroy()
    {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_settings)
        {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.validationTypeBtt)
            OnClickValidationType();
        else if (view.getId() == R.id.resetBtt)
            OnClickReset();
        else if (view.getId() == R.id.validateBtt)
            OnClickValidate();
    }
    
    private void OnClickValidationType()
    {
        DialogInterface.OnClickListener actionListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int item)
            {
                currentTypeIndex = item;
                RefreshGUI();
                dialog.dismiss();
            }
        };
        
        Utils.ShowPickerDialog(new AlertDialog.Builder(this), "Validation types", validationTypes, currentTypeIndex, actionListener);
    }
    
    private void OnClickReset()
    {
        this.callId = null;
        this.callerid_hash = null;
        this.dialingNumber = null;
        this.validationKey = null;
        this.pinStep = false;
        this.pinEditText.setText("");
        this.phoneNumberEditText.setText("");
        
        RefreshGUI();
    }
    
    private ValidationType GetCurrentValidationType()
    {
        if (currentTypeIndex == 0)
            return ValidationType.CLI;
        else if (currentTypeIndex == 1)
            return ValidationType.SMS;
        else if (currentTypeIndex == 2)
            return ValidationType.IVR;
        else
            return ValidationType.REVERSE_CLI;
    }
    
    private void OnClickValidate()
    {
        if (!Utils.IsNetworkConnected(this))
        {
            Utils.ShowMessageBox(new AlertDialog.Builder(this), "Error", "No internet connection available!");
            return;
        }
        
        if (!this.pinStep)
        {
            if (this.phoneNumberEditText.getText().length() == 0)
            {
                Utils.ShowMessageBox(new AlertDialog.Builder(this), "Invalid number", "Please provide a valid number");
                return;
            }
            
            DismissKeyboard();
            ShowLoadingMessage(true);
            this.validation_type = GetCurrentValidationType();
            
            CheckMobiService.getInstance().RequestValidation(this.validation_type, this.phoneNumberEditText.getText().toString(), new AsyncResponse()
            {
                @Override
                public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                {
                    System.out.println("Status= " + httpStatus + " result= " + (result != null ? result.toString() : "null") + " error=" + error);
                    
                    if (httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                    {
                        String key = String.valueOf(result.get("id"));
                        String type = String.valueOf(result.get("type"));
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> validation_info = (Map<String, Object>) result.get("validation_info");
                        
                        phoneNumberEditText.setText(String.valueOf(validation_info.get("formatting")));
                        
                        if (type.equalsIgnoreCase(ValidationType.CLI.getValue()))
                            PerformCliValidation(key, String.valueOf(result.get("dialing_number")));
                        else if (type.equalsIgnoreCase(ValidationType.REVERSE_CLI.getValue()))
                            PerformReverseCliValidation(key, String.valueOf(result.get("pin_hash")));
                        else
                            PerformPinValidation(key);
                    }
                    else
                    {
                        ShowLoadingMessage(false);
                        HandleValidationServiceError(httpStatus, result, error);
                    }
                }
            });
            
        }
        else
        {
            if (this.pinEditText.getText().length() == 0)
            {
                Utils.ShowMessageBox(new AlertDialog.Builder(this), "Invalid pin", "Please provide a valid pin number");
                return;
            }
            
            DismissKeyboard();
            ShowLoadingMessage(true);
            
            CheckMobiService.getInstance().VerifyPin(this.validation_type, this.validationKey, this.pinEditText.getText().toString(), new AsyncResponse()
            {
                @Override
                public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                {
                    ShowLoadingMessage(false);
                    
                    if (httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                    {
                        Boolean validated = (Boolean) result.get("validated");
                        
                        if (!validated)
                        {
                            Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Invalid PIN!");
                            return;
                        }
                        
                        String message = "Validation completed for: " + phoneNumberEditText.getText().toString();
                        Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Validation completed", message);
                        OnClickReset();
                    }
                    else
                    {
                        HandleValidationServiceError(httpStatus, result, error);
                    }
                }
            });
            
        }
    }
    
    private void PerformReverseCliValidation(String key, String hash)
    {
        this.validationKey = key;
        this.callerid_hash = hash;
        
        RefreshGUI();
        
        timer = new Timer();
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(new Runnable()
                {
                    
                    @Override
                    public void run()
                    {
                        StopReverseCliTimer();
                        ShowLoadingMessage(false);
                        Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Validation failed!");
                    }
                });
                
            }
        };
        
        timer.schedule(timerTask, 15000);
    }
    
    private void StartActionCallIntent(String package_name, String number)
    {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        intent.setPackage(package_name);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
        {
            Utils.ShowMessageBox(new AlertDialog.Builder(this), "Error", "You need to provide permissions to the app to place calls");
            return;
        }
        
        this.startActivity(intent);
    }
    
    private void PerformCliValidation(String key, String destinationNr)
    {
        final String ANDROID_NEW_PACKAGE_NAME = "com.android.server.telecom";
        final String ANDROID_OLD_PACKAGE_NAME = "com.android.phone";
        
        ShowLoadingMessage(false);
        
        this.validationKey = key;
        this.dialingNumber = destinationNr;
        
        String package_name;
        
        if(Utils.isCompatible(Utils.LOLLIPOP))
            package_name = ANDROID_NEW_PACKAGE_NAME;
        else
            package_name = ANDROID_OLD_PACKAGE_NAME;
        
        try
        {
            //some android 5 devices still use the package name from android 4.x or older
            //in case we get exception we fallback on the old package name
            StartActionCallIntent(package_name, destinationNr);
        }
        catch (Exception ex)
        {
            if(package_name.equalsIgnoreCase(ANDROID_NEW_PACKAGE_NAME))
                package_name = ANDROID_OLD_PACKAGE_NAME;
            else
                package_name = ANDROID_NEW_PACKAGE_NAME;
            
            StartActionCallIntent(package_name, destinationNr);
        }
        
        RefreshGUI();
    }
    
    private void PerformPinValidation(String key)
    {
        ShowLoadingMessage(false);
        
        this.validationKey = key;
        this.pinStep = true;
        RefreshGUI();
    }
    
    private void RefreshGUI()
    {
        if(this.pinStep)
            this.validateButton.setText("Submit PIN");
        else
            this.validateButton.setText("Validate");
        
        this.validationTypeButton.setText(this.validationTypes[this.currentTypeIndex]);
        
        this.validationTypeButton.setEnabled(!this.pinStep);
        this.phoneNumberEditText.setEnabled(!this.pinStep);
        
        this.pinEditText.setVisibility(this.pinStep ? View.VISIBLE : View.GONE);
        this.resetButton.setVisibility(this.pinStep ? View.VISIBLE : View.GONE);
        this.chargeLabel.setVisibility(this.currentTypeIndex == 0 ? View.VISIBLE : View.GONE);
    }
    
    private void HandleValidationServiceError(int httpStatus, Map<String, Object> body, String error)
    {
        System.out.println("HandleValidationServiceError status= "+ httpStatus+" body= "+ (body!= null ? body.toString(): "null")+" error="+error);
        
        if(body != null)
        {
            Number err = (Number) body.get("code");
            String error_message;
            
            switch (ErrorCode.get(err.intValue()))
            {
                case ErrorCodeInvalidPhoneNumber:
                    error_message = "Invalid phone number. Please provide the number in E164 format.";
                    break;
                
                case ErrorCodeDestinationBlocked:
                    error_message = "Destination blocked.";
                    break;
                
                //@todo: REMOVE THIS IN PRODUCTION. End users shouldn't see this.
                
                case ErrorCodeInvalidApiKey:
                    error_message = "Invalid API secret key.";
                    break;
                
                case ErrorCodeInsufficientFunds:
                    error_message = "Insufficient funds. Please recharge your account or subscribe for trial credit";
                    break;
                
                case ErrorCodeInsufficientCLIValidations:
                    error_message = "No more caller id validations available. upgrade your account";
                    break;
                
                case ErrorCodeValidationMethodNotAvailableInRegion:
                    error_message = "Validation type not available for this number";
                    break;
                
                case ErrorCodeInvalidNotificationUrl:
                    error_message = "Invalid notification URL";
                    break;
                
                case ErrorCodeInvalidEventPayload:
                    error_message = "Invalid event inside payload";
                    break;
                
                case ErrorCodeAccountLimitReached:
                    error_message = "One or multiple account restrictions reached.";
                    break;
                
                case ErrorCodeRestrictedAccount:
                    error_message = "Account suspended due to suspicious traffic";
                    break;
                
                default:
                    error_message = "Service unavailable. Please try later.";
            }
            
            Utils.ShowMessageBox(new AlertDialog.Builder(this), "Error", error_message);
        }
        else
            Utils.ShowMessageBox(new AlertDialog.Builder(this), "Error", "Service unavailable. Please try later.");
    }
    
    private void ShowLoadingMessage(boolean show)
    {
        if(show)
        {
            if(this.loadingDialog == null)
                this.loadingDialog = new WeakReference<>(ProgressDialog.show(MainActivity.this, "", "Please wait...", true));
        }
        else
        {
            if(this.loadingDialog != null)
            {
                this.loadingDialog.get().dismiss();
                this.loadingDialog = null;
            }
        }
        
    }
    
    private void DismissKeyboard()
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(phoneNumberEditText.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(pinEditText.getWindowToken(), 0);
    }
    
}

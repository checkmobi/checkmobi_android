package com.checkmobi;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends ActionBarActivity implements View.OnClickListener
{
    private WeakReference<ProgressDialog> loadingDialog;

    private EditText phoneNumberEditText;
    private EditText pinEditText;
    private TextView chargeLabel;
    private Button validationTypeButton;
    private Button resetButton;
    private Button validateButton;

    private final String[] validationTypes = new String[]{"CLI", "SMS", "IVR", "Reverse CLI"};
    private int currentTypeIndex = 0;

    private String callId;
    private String dialingNumber;
    private String validationKey;
    private boolean pinStep = false;

    //reverse cli validation

    private String callerId;
    private Timer timer;
    private TimerTask timerTask;
    private final Handler handler = new Handler();

    final BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(CallReceiver.MSG_CALL_START))
            {
                if(!intent.getBooleanExtra("incoming", false))
                {
                    if(MainActivity.this.dialingNumber == null || MainActivity.this.validationKey == null || MainActivity.this.callId != null)
                        return;

                    MainActivity.this.callId = intent.getStringExtra("number");
                }
                else
                {
                    String number =  intent.getStringExtra("number");

                    if(number.startsWith("+"))
                        number = number.substring(1);

                    if(number.startsWith(callerId))
                    {
                        StopReverseCliTimer();
                        HangupCall();

                        //check validation

                        String pinNumber = number.substring(number.length() - 4);

                        CheckMobiService.getInstance().VerifyPin(MainActivity.this.validationKey, pinNumber, new AsyncResponse()
                        {
                            @Override
                            public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                            {
                                ShowLoadingMessage(false);

                                if(httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                                {
                                    Boolean validated = (Boolean) result.get("validated");

                                    if(!validated)
                                    {
                                        Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Validation failed!");
                                        return;
                                    }

                                    String message = "Validation completed for: "+ phoneNumberEditText.getText().toString();
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
            if(intent.getAction().equals(CallReceiver.MSG_CALL_END))
            {
                if(!intent.getBooleanExtra("incoming", false))
                {
                    MainActivity pThis = MainActivity.this;

                    if(pThis.dialingNumber == null || pThis.validationKey == null || pThis.callId == null)
                        return;

                    if(pThis.callId.compareTo(intent.getStringExtra("number")) != 0)
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
        if(timer != null)
        {
            timer.cancel();
            timer = null;
            timerTask = null;
        }
    }

    private void HangupCall()
    {
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
            System.out.println("Failed to hangup the call...");
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

        Utils.TrustInvalidSslCertificates();
        CheckMobiService.getInstance().SetSecretKey("secret_key_here");

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
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        if(view.getId() == R.id.validationTypeBtt)
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
        this.callerId = null;
        this.dialingNumber = null;
        this.validationKey = null;
        this.pinStep = false;
        this.pinEditText.setText("");
        this.phoneNumberEditText.setText("");

        RefreshGUI();
    }

    private ValidationType GetCurrentValidationType()
    {
        //return ValidationType.get(validationTypes[currentTypeIndex].toLowerCase())

        if(currentTypeIndex == 0)
            return ValidationType.CLI;
        else if(currentTypeIndex == 1)
            return ValidationType.SMS;
        else if(currentTypeIndex == 2)
            return ValidationType.IVR;
        else
            return ValidationType.REVERSE_CLI;
    }

    private void OnClickValidate()
    {
        if(!Utils.IsNetworkConnected(this))
        {
            Utils.ShowMessageBox(new AlertDialog.Builder(this), "Error", "No internet connection available!");
            return;
        }

        if(!this.pinStep)
        {
            if(this.phoneNumberEditText.getText().length() == 0)
            {
                Utils.ShowMessageBox(new AlertDialog.Builder(this), "Invalid number", "Please provide a valid number");
                return;
            }

            DismissKeyboard();
            ShowLoadingMessage(true);

            CheckMobiService.getInstance().RequestValidation(GetCurrentValidationType(), this.phoneNumberEditText.getText().toString(), new AsyncResponse()
            {
                @Override
                public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                {
                    System.out.println("Status= "+ httpStatus+" result= "+ (result!= null ? result.toString(): "null")+" error="+error);

                    if(httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                    {
                        String key = String.valueOf(result.get("id"));
                        String type = String.valueOf(result.get("type"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> validation_info = (Map<String, Object>) result.get("validation_info");

                        phoneNumberEditText.setText(String.valueOf(validation_info.get("formatting")));

                        if(type.equalsIgnoreCase(ValidationType.CLI.getValue()))
                            PerformCliValidation(key, String.valueOf(result.get("dialing_number")));
                        else if(type.equalsIgnoreCase(ValidationType.REVERSE_CLI.getValue()))
                            PerformReverseCliValidation(key, String.valueOf(result.get("cli_prefix")));
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
            if(this.pinEditText.getText().length() == 0)
            {
                Utils.ShowMessageBox(new AlertDialog.Builder(this), "Invalid pin", "Please provide a valid pin number");
                return;
            }

            DismissKeyboard();
            ShowLoadingMessage(true);

            CheckMobiService.getInstance().VerifyPin(this.validationKey, this.pinEditText.getText().toString(), new AsyncResponse()
            {
                @Override
                public void OnRequestCompleted(int httpStatus, Map<String, Object> result, String error)
                {
                    ShowLoadingMessage(false);

                    if(httpStatus == CheckMobiService.STATUS_SUCCESS && result != null)
                    {
                        Boolean validated = (Boolean) result.get("validated");

                        if(!validated)
                        {
                            Utils.ShowMessageBox(new AlertDialog.Builder(MainActivity.this), "Error", "Invalid PIN!");
                            return;
                        }

                        String message = "Validation completed for: "+ phoneNumberEditText.getText().toString();
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

    private void PerformReverseCliValidation(String key, String lastDigits)
    {
        this.validationKey = key;
        this.callerId = lastDigits;

        RefreshGUI();

        timer = new Timer();
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(new Runnable() {

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

    private void PerformCliValidation(String key, String destinationNr)
    {
        ShowLoadingMessage(false);

        this.validationKey = key;
        this.dialingNumber = destinationNr;

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + destinationNr));
        intent.setPackage("com.android.phone");
        this.startActivity(intent);

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

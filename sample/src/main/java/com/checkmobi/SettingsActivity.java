package com.checkmobi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.checkmobi.sdk.CheckMobiService;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener
{
    private EditText baseUrlEditText;
    private EditText secretKeyEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.baseUrlEditText = (EditText) findViewById(R.id.editBaseUrl);
        this.secretKeyEditText = (EditText) findViewById(R.id.editSecretKey);
        Button cancelButton = (Button) findViewById(R.id.buttonCancel);
        Button saveButton = (Button) findViewById(R.id.buttonSave);

        this.secretKeyEditText.setText(Settings.getInstance().getSecretKey());
        this.baseUrlEditText.setText(Settings.getInstance().getBaseUrl());

        cancelButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view)
    {
        if(view.getId() == R.id.buttonCancel)
            OnClickCancel();
        else if (view.getId() == R.id.buttonSave)
            OnClickSave();
    }

    private void OnClickCancel()
    {
        finish();
    }

    private void OnClickSave()
    {
        Settings.getInstance().setBaseUrl(this.baseUrlEditText.getText().toString());
        Settings.getInstance().setSecretKey(this.secretKeyEditText.getText().toString());
        Settings.getInstance().commit();

        CheckMobiService.getInstance().SetSecretKey(Settings.getInstance().getSecretKey());
        CheckMobiService.getInstance().SetBaseUrl(Settings.getInstance().getBaseUrl());

        finish();
    }
}

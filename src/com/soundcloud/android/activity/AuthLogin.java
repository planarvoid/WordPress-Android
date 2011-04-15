package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AuthLogin extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.auth_login);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_password);
        final Button cancelBtn = (Button) findViewById(R.id.btn_cancel);
        final Button loginBtn = (Button) findViewById(R.id.btn_login);

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return loginBtn.performClick();
                } else {
                    return false;
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0){
                    CloudUtils.showToast(AuthLogin.this, R.string.authentication_error_incomplete_fields);
                    return;
                }
                Log.i(getClass().getSimpleName(),"Login with " + emailField.getText().toString() + " and "  + passwordField.getText().toString());
            }
        });


        CloudUtils.clickify(((TextView)findViewById(R.id.txt_msg)), getResources().getString(R.string.authentication_I_forgot_my_password),new ClickSpan.OnClickListener()
         {
            @Override
            public void onClick() {
                Intent i = new Intent(AuthLogin.this, AuthRecover.class);
                if (emailField.getText().length() > 0){
                    i.putExtra("email", emailField.getText().toString());
                }
                startActivity(i);
            }
        });


    }

}

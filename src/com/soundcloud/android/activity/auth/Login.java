package com.soundcloud.android.activity.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Login extends LoginActivity {
    protected void build() {
        setContentView(R.layout.login);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText passwordField = (EditText) findViewById(R.id.txt_password);
        final Button loginBtn = (Button) findViewById(R.id.btn_login);

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
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

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    CloudUtils.showToast(Login.this, R.string.authentication_error_incomplete_fields);
                } else {
                    final String email = emailField.getText().toString();
                    final String password = passwordField.getText().toString();
                    login(email, password);
                }
            }
        });

        CloudUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_I_forgot_my_password),
                new ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        Intent i = new Intent(Login.this, Recover.class);
                        if (emailField.getText().length() > 0) {
                            i.putExtra("email", emailField.getText().toString());
                        }
                        startActivityForResult(i, 0);
                    }
                }, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Start.handleRecoverResult(this, data);
        }
    }
}

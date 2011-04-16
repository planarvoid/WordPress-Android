package com.soundcloud.android.activity.auth;

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

public class SignUp extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.auth_signup);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final EditText choosePasswordField = (EditText) findViewById(R.id.txt_choose_a_password);
        final EditText repeatPasswordField = (EditText) findViewById(R.id.txt_repeat_your_password);
        final Button cancelBtn = (Button) findViewById(R.id.btn_cancel);
        final Button signupBtn = (Button) findViewById(R.id.btn_signup);

        repeatPasswordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return signupBtn.performClick();
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

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || choosePasswordField.getText().length() == 0 || choosePasswordField.getText().length() == 0){
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_incomplete_fields);
                    return;
                }

                if (!choosePasswordField.getText().equals(choosePasswordField.getText())){
                    CloudUtils.showToast(SignUp.this, R.string.authentication_error_password_mismatch);
                    return;
                }

                Log.i(getClass().getSimpleName(),"Signup with " + emailField.getText().toString());

                startActivity(new Intent(SignUp.this, AddInfo.class));
            }
        });


        CloudUtils.clickify(((TextView)findViewById(R.id.txt_msg)), getResources().getString(R.string.authentication_terms_of_use),new ClickSpan.OnClickListener()
         {
            @Override
            public void onClick() {
                Log.i(getClass().getSimpleName(),"Go to terms of use ");
            }
        });


    }

}

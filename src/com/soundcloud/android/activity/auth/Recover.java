package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

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

public class Recover extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.auth_recover);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final Button cancelBtn = (Button) findViewById(R.id.btn_cancel);
        final Button recoverBtn = (Button) findViewById(R.id.btn_ok);

        emailField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return recoverBtn.performClick();
                } else {
                    return false;
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "cancel");
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        recoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0) {
                    CloudUtils.showToast(Recover.this, R.string.authentication_error_incomplete_fields);
                } else {
                    recoverPassword(emailField.getText().toString());
                }
            }
        });

        CloudUtils.clickify(((TextView) findViewById(R.id.txt_msg)), getResources().getString(R.string.authentication_support), new ClickSpan.OnClickListener() {
            @Override
            public void onClick() {
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.authentication_support_email_address)});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.authentication_support_email_subject));
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.authentication_support_email_message));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.authentication_support_email_chooser_text)));
            }
        });

        if (getIntent().hasExtra("email")) {
            emailField.setText(getIntent().getStringExtra("email"));
        }
    }

    private void recoverPassword(String email) {
        Log.i(getClass().getSimpleName(), "Recover with " + email);
    }
}

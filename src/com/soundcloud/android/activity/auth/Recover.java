package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.task.RecoverPasswordTask;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
        setContentView(R.layout.recover);

        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
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

        CloudUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_support),
                new ClickSpan.OnClickListener() {
            @Override public void onClick() {
                startActivity(
                        new Intent(Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.authentication_support_uri))));
            }
        }, true);

        if (getIntent().hasExtra("email")) {
            emailField.setText(getIntent().getStringExtra("email"));
        }
    }

    private void recoverPassword(final String email) {
        new RecoverPasswordTask((AndroidCloudAPI) getApplication()) {
            private ProgressDialog progressDialog;
            @Override
            protected void onPreExecute() {
                if (!isFinishing()) {
                    progressDialog = CloudUtils.showProgress(Recover.this,
                            R.string.authentication_recover_progress_message);
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!isFinishing() && progressDialog != null) {
                    try {
                        progressDialog.dismiss();
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                setResult(RESULT_OK, new Intent()
                        .putExtra("success", success)
                        .putExtra("error", getFirstError()));

                finish();
            }
        }.execute(email);
    }
}
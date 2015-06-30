package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.TrackedActivity;
import com.soundcloud.android.onboarding.auth.tasks.RecoverPasswordTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;

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

public class RecoverActivity extends TrackedActivity {

    private PublicApiWrapper publicCloudAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        build();
        if (savedInstanceState == null) {
            getEventBus().publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_FORGOT_PASSWORD));
        }
    }

    protected void build() {
        setContentView(R.layout.recover);
        publicCloudAPI = PublicApiWrapper.getInstance(this);
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
                    AndroidUtils.showToast(RecoverActivity.this, R.string.authentication_error_incomplete_fields);
                } else {
                    recoverPassword(emailField.getText().toString());
                }
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                getResources().getString(R.string.authentication_recover_password_visit_our_Help_Center),
                new ScTextUtils.ClickSpan.OnClickListener() {
                    @Override
                    public void onClick() {
                        startActivity(
                                new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(getString(R.string.url_forgot_email_help))));
                    }
                }, true, false);

        if (getIntent().hasExtra("email")) {
            emailField.setText(getIntent().getStringExtra("email"));
        }
    }

    private void recoverPassword(final String email) {
        new RecoverPasswordTask(publicCloudAPI) {
            private ProgressDialog progressDialog;
            @Override
            protected void onPreExecute() {
                if (!isFinishing()) {
                    progressDialog = AndroidUtils.showProgress(RecoverActivity.this,
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
package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.onboarding.auth.tasks.RecoverPasswordTask;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;

public class RecoverActivity extends RootActivity {

    @Inject EventBus eventBus;
    @Inject Resources resources;
    @Inject TokenInformationGenerator tokenInformationGenerator;
    @Inject OAuth oAuth;
    @Inject ApiClient apiClient;
    @Inject FeatureFlags featureFlags;
    @Inject RecoverPasswordOperations recoverPasswordOperations;

    public RecoverActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        build();
        if (savedInstanceState == null) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.AUTH_FORGOT_PASSWORD));
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.recover);
    }

    protected void build() {
        final EditText emailField = (EditText) findViewById(R.id.txt_email_address);
        final Button recoverBtn = (Button) findViewById(R.id.btn_ok);

        emailField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                return recoverBtn.performClick();
            } else {
                return false;
            }
        });

        recoverBtn.setOnClickListener(v -> {
            if (emailField.getText().length() == 0) {
                AndroidUtils.showToast(RecoverActivity.this, R.string.authentication_error_incomplete_fields);
            } else {
                recoverPassword(emailField.getText().toString());
            }
        });

        ScTextUtils.clickify(((TextView) findViewById(R.id.txt_msg)),
                             getResources().getString(R.string.authentication_recover_password_visit_our_Help_Center),
                             () -> startActivity(
                                     new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(getString(R.string.url_forgot_email_help)))), true, false);

        if (getIntent().hasExtra("email")) {
            emailField.setText(getIntent().getStringExtra("email"));
        }
    }

    private void recoverPassword(final String email) {
        new RecoverPasswordTask(tokenInformationGenerator, oAuth, apiClient, resources, featureFlags, recoverPasswordOperations) {
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
                        .putExtra("error", reason));

                finish();
            }
        }.execute(email);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }

}

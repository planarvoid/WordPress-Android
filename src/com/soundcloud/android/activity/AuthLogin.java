package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.utils.ClickSpan;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.CloudAPI;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

public class AuthLogin extends AccountAuthenticatorActivity {

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

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (emailField.getText().length() == 0 || passwordField.getText().length() == 0) {
                    CloudUtils.showToast(AuthLogin.this, R.string.authentication_error_incomplete_fields);
                } else {
                    final String email =  emailField.getText().toString();
                    final String password = passwordField.getText().toString();
                    Log.d(TAG, "Login with " + email + " and " + password);
                    login(email, password);
                }
            }
        });


        CloudUtils.clickify(((TextView) findViewById(R.id.txt_msg)), getResources().getString(R.string.authentication_I_forgot_my_password), new ClickSpan.OnClickListener() {
            @Override
            public void onClick() {
                Intent i = new Intent(AuthLogin.this, AuthRecover.class);
                if (emailField.getText().length() > 0) {
                    i.putExtra("email", emailField.getText().toString());
                }
                startActivity(i);
            }
        });


    }

    protected void login(final String username, final String password) {
        final String type = getString(R.string.account_type);
        final AndroidCloudAPI api = (AndroidCloudAPI) getApplication();

        new GetTokensTask(api) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                progress = new ProgressDialog(AuthLogin.this);
                progress.setIndeterminate(true);
                progress.setTitle(R.string.progress_sc_connect_title);
                progress.show();
            }

            @Override
            protected void onPostExecute(final Pair<String, String> tokens) {
                if (tokens != null) {
                    new LoadTask.LoadUserTask(api) {
                        @Override
                        protected void onPostExecute(User user) {
                            progress.dismiss();
                            SoundCloudApplication app = (SoundCloudApplication) getApplication();
                            if (user != null && app.addUserAccount(user, tokens.first, tokens.second)) {
                                final Bundle result = new Bundle();
                                result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
                                result.putString(AccountManager.KEY_ACCOUNT_TYPE, type);
                                setAccountAuthenticatorResult(result);
                                finish();
                            } else { // user request failed
                                showError(null);
                            }
                        }
                    }.execute(CloudAPI.Enddpoints.MY_DETAILS);
                } else { // no tokens obtained
                    progress.dismiss();
                    showError(mException);
                }
            }
        }.execute(username, password);
    }

    private void showError(IOException e) {
        final boolean tokenError = e instanceof CloudAPI.InvalidTokenException;
        new AlertDialog.Builder(AuthLogin.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.error_sc_connect_error_title)
                .setMessage(tokenError ? R.string.error_sc_connect_invalid_password : R.string.error_sc_connect_error_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // finish();
                    }
                })
                .create()
                .show();
    }
}

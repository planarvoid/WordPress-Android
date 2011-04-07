package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.api.CloudAPI;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

public class Authorize extends AccountAuthenticatorActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected void build() {
        setContentView(R.layout.authorize);
        final EditText usernameField = (EditText) findViewById(R.id.username);
        final EditText passwordField = (EditText) findViewById(R.id.password);

        passwordField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @SuppressWarnings({"SimplifiableIfStatement"})
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                event.getAction() == KeyEvent.ACTION_DOWN)) {
                    return findViewById(R.id.submit).performClick();
                } else {
                    return false;
                }
            }
        });

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                final String username = usernameField.getText().toString();
                final String password = passwordField.getText().toString();

                login(username, password);
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
                progress = new ProgressDialog(Authorize.this);
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
        }.execute(new Pair<String, String>(username, password));
    }

    private void showError(IOException e) {
        final boolean tokenError = e instanceof CloudAPI.InvalidTokenException;
        new AlertDialog.Builder(Authorize.this)
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

    static class GetTokensTask extends AsyncTask<Pair<String, String>, Void, Pair<String, String>> {
        private CloudAPI mApi;
        protected IOException mException;

        public GetTokensTask(CloudAPI api) {
            this.mApi = api;
        }

        @Override
        protected Pair<String, String> doInBackground(Pair<String, String>... params) {
            Pair<String, String> credentials = params[0];
            try {
                CloudAPI api = mApi.login(credentials.first, credentials.second);
                return new Pair<String, String>(api.getToken(), api.getRefreshToken());
            } catch (IOException e) {
                mException = e;
                Log.e(TAG, "error logging in", e);
                return null;
            }
        }
    }
}

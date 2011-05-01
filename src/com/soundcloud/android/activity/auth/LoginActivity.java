package com.soundcloud.android.activity.auth;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;

import java.io.IOException;

public abstract class LoginActivity extends Activity {

    protected void login(String... parameters) {
        final AndroidCloudAPI api = (AndroidCloudAPI) getApplication();

        new GetTokensTask(api) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                progress = ProgressDialog.show(LoginActivity.this, "",
                        LoginActivity.this.getString(R.string.authentication_login_progress_message));
            }

            @Override
            protected void onPostExecute(final Token token) {
                if (token != null) {
                    new LoadTask.LoadUserTask(api) {
                        @Override
                        protected void onPostExecute(User user) {
                            progress.dismiss();
                            if (user != null) {
                                setResult(RESULT_OK,
                                    new Intent().putExtra("user", user)
                                                .putExtra("token", token));

                                finish();
                            } else { // user request failed
                                showError(null);
                            }
                        }
                    }.execute(Request.to(Endpoints.MY_DETAILS));
                } else { // no tokens obtained
                    progress.dismiss();
                    showError(mException);
                }
            }
        }.execute(parameters);
    }

    protected void showError(IOException e) {
        final boolean tokenError = e instanceof CloudAPI.InvalidTokenException;
        new AlertDialog.Builder(LoginActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.authentication_error_title)
                .setMessage(tokenError ? R.string.authentication_login_error_password_message : R.string.authentication_error_no_connection_message)
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

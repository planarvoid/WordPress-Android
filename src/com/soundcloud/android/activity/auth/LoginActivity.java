package com.soundcloud.android.activity.auth;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.GetTokensTask;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.task.LoadUserInfoTask;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

public abstract class LoginActivity extends Activity {
    public static final String[] SCOPES_TO_REQUEST = { Token.SCOPE_NON_EXPIRING, Token.SCOPE_PLAYCOUNT };
    public static final String SCOPES_EXTRA = "scopes";
    public static final String SIGNED_UP_EXTRA = "signed_up";

    public static final String CODE_EXTRA = "code";
    public static final String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
    public static final String USERNAME_EXTRA = "username";
    public static final String PASSWORD_EXTRA = "password";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        build();
    }

    protected abstract void build();

    protected void login(String username, String password) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
        param.putString(PASSWORD_EXTRA, password);
        login(param);
    }


    protected void loginExtensionGrantype(String grantType) {
        final Bundle param = new Bundle();
        param.putString(EXTENSION_GRANT_TYPE_EXTRA, grantType);
        login(param);
    }

    protected void login(final Bundle data) {
        if (data.getString(SCOPES_EXTRA) == null) {
            // default to non-expiring scope+playcount
            data.putStringArray(SCOPES_EXTRA, SCOPES_TO_REQUEST);
        }

        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        new GetTokensTask(app) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                if (!isFinishing()) {
                    progress = ProgressDialog.show(LoginActivity.this, "",
                            LoginActivity.this.getString(R.string.authentication_login_progress_message));
                }
            }

            @Override
            protected void onPostExecute(final Token token) {
                if (token != null) {
                    new LoadUserInfoTask(app, -1) {
                        @Override
                        protected void onPostExecute(User user) {
                            if (user != null) {
                                app.trackEvent(Consts.Tracking.Categories.AUTH, "login");
                                app.trackPage(Consts.Tracking.LOGIN);
                                dismissDialog(progress);
                                setResult(RESULT_OK,
                                        new Intent().putExtra(SIGNED_UP_EXTRA, token.getSignup())
                                                .putExtra("user", user)
                                                .putExtra("token", token)
                                                .putExtras(data));
                                finish();
                            } else { // user request failed
                                showError(null);
                            }
                        }
                    }.execute(Request.to(Endpoints.MY_DETAILS));
                } else { // no tokens obtained
                    dismissDialog(progress);
                    showError(mException);
                }
            }
        }.execute(data);
    }

    protected void showError(IOException e) {
        if (!isFinishing()) {
            final boolean tokenError = e instanceof CloudAPI.InvalidTokenException;
            new AlertDialog.Builder(LoginActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(tokenError ? R.string.authentication_error_title : R.string.authentication_error_no_connection_title)
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

    protected void showDialog(Dialog d) {
        if (!isFinishing()) {
            d.show();
        }
    }

    protected void dismissDialog(Dialog d) {
        if (!isFinishing() && d != null) {
            try {
                d.dismiss();
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}

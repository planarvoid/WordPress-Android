package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.SuggestedUsers;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.auth.GetTokensTask;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class AbstractLoginActivity extends AccountAuthenticatorActivity {
    public static final String[] SCOPES_TO_REQUEST = { Token.SCOPE_NON_EXPIRING };
    public static final String SCOPES_EXTRA = "scopes";

    public static final String CODE_EXTRA = "code";
    public static final String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
    public static final String USERNAME_EXTRA = "username";
    public static final String PASSWORD_EXTRA = "password";

    protected void login(String username, String password) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
        param.putString(PASSWORD_EXTRA, password);
        login(param);
    }

    protected void login(final Bundle data) {
        if (!data.containsKey(SCOPES_EXTRA)) {
            // default to non-expiring scope
            data.putStringArray(SCOPES_EXTRA, SCOPES_TO_REQUEST);
        }
        final SoundCloudApplication app = (SoundCloudApplication) getApplication();

        new GetTokensTask(app) {
            ProgressDialog progress;

            @Override
            protected void onPreExecute() {
                if (!isFinishing()) {
                    progress = AndroidUtils.showProgress(AbstractLoginActivity.this,
                            R.string.authentication_login_progress_message);
                }
            }

            @Override
            protected void onPostExecute(final Token token) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "GetTokensTask#onPostExecute("+token+")");

                if (token != null) {
                    //TODO: pull this out, too many levels of indentation make me a saaaad panda.
                    new FetchUserTask(app) {
                        @Override
                        protected void onPostExecute(User user) {
                            dismissDialog(progress);

                            if (user != null) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "GetTokensTask#onPostExecute("+user+")");

                                // for logins, we don't track via
                                SignupVia via = SignupVia.NONE;

                                // need to create user account as soon as possible, so the executeRefreshTask logic in
                                // SoundCloudApplication works properly
                                boolean accountCreated = app.addUserAccountAndEnableSync(user, app.getToken(), via);
                                if (accountCreated) {
                                    // success path
                                    onAuthenticated(via, user);
                                } else {
                                    AndroidUtils.showToast(AbstractLoginActivity.this, R.string.error_creating_account);
                                }
                            } else {
                                // TODO: means we got a 404 on the user, needs to be more expressive...
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

    protected void onAuthenticated(@NotNull SignupVia via, @NotNull User user) {
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        result.putBoolean(Consts.Keys.WAS_SIGNUP, via != SignupVia.NONE);
        super.setAccountAuthenticatorResult(result);

        SoundCloudApplication.MODEL_MANAGER.cacheAndWrite(user, ScResource.CacheUpdateMode.FULL);

        if (via != SignupVia.NONE) {
            // user has signed up, schedule sync of user data to possibly refresh image data
            // which gets processed asynchronously by the backend and is only available after signup has happened
            final Context context = getApplicationContext();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    context.startService(new Intent(context, ApiSyncService.class).setData(Content.ME.uri));
                }
            }, 30 * 1000);
        }

        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                .putExtra(User.EXTRA, user)
                .putExtra(SignupVia.EXTRA, via.name));

        if (result.getBoolean(Consts.Keys.WAS_SIGNUP)) {
            startActivity(new Intent(this, SuggestedUsers.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Consts.Keys.WAS_SIGNUP, true));
        } else {
            startActivity(new Intent(this, Home.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        finish();
    }

    //TODO: use dialog fragments or managed dialogs (onCreateDialog)
    protected void showError(@Nullable IOException e) {
        if (!isFinishing()) {
            final boolean tokenError = e instanceof CloudAPI.InvalidTokenException;
            new AlertDialog.Builder(AbstractLoginActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(tokenError ? R.string.authentication_error_title : R.string.authentication_error_no_connection_title)
                    .setMessage(tokenError ? R.string.authentication_login_error_password_message : R.string.authentication_error_no_connection_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
        }
    }

    //TODO: use dialog fragments or managed dialogs (onCreateDialog)
    protected void showDialog(Dialog d) {
        if (!isFinishing()) {
            d.show();
        }
    }

    //TODO: use dialog fragments or managed dialogs (onCreateDialog)
    protected void dismissDialog(Dialog d) {
        if (!isFinishing() && d != null) {
            try {
                d.dismiss();
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}

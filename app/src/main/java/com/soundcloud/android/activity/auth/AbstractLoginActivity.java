package com.soundcloud.android.activity.auth;


import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.activity.landing.SuggestedUsers;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.dialog.auth.GooglePlusSignInDialogFragment;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.auth.LoginTask;
import com.soundcloud.api.Token;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

public abstract class AbstractLoginActivity extends SherlockFragmentActivity implements AuthTaskFragment.OnAuthResultListener {
    public static final String[] SCOPES_TO_REQUEST = { Token.SCOPE_NON_EXPIRING };
    public static final String SCOPES_EXTRA = "scopes";

    public static final String CODE_EXTRA = "code";
    public static final String EXTENSION_GRANT_TYPE_EXTRA = "extensionGrantType";
    public static final String USERNAME_EXTRA = "username";
    public static final String PASSWORD_EXTRA = "password";

    protected ProgressDialog mProgressDialog;

    /**
     * Extracted account authenticator functions. Extracted because of Fragment usage, we have to extend FragmentActivity.
     * See {@link AccountAuthenticatorActivity} for documentation
     */
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
    }

    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    protected void login(String username, String password) {
        final Bundle param = new Bundle();
        param.putString(USERNAME_EXTRA, username);
        param.putString(PASSWORD_EXTRA, password);
        login(param, null);
    }

    protected void login(Bundle data, final ProgressDialog progressDialog) {
        new LoginTask((SoundCloudApplication) getApplication()).execute(data);
    }

    @Override
    public void onAccountAdded(User user, SignupVia via){
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        result.putBoolean(Consts.Keys.WAS_SIGNUP, via != SignupVia.NONE);
        mResultBundle = result;

        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                .putExtra(User.EXTRA_ID, user.id)
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

    @Override
    public void onError(String message){
        if (!isFinishing()) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                dismissDialog(mProgressDialog);
            }
            new AlertDialog.Builder(AbstractLoginActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.authentication_error_title))
                    .setMessage(message)
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

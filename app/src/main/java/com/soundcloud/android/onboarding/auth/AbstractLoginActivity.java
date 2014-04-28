package com.soundcloud.android.onboarding.auth;


import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoriesFragment;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

public abstract class AbstractLoginActivity extends FragmentActivity implements AuthTaskFragment.OnAuthResultListener {
    protected static final String LOGIN_DIALOG_TAG = "login_dialog";

    /**
     * Extracted account authenticator functions. Extracted because of Fragment usage, we have to extend FragmentActivity.
     * See {@link AccountAuthenticatorActivity} for documentation
     */
    private AccountAuthenticatorResponse accountAuthenticatorResponse;
    private Bundle resultBundle;
    private EventBus eventBus;

    // a bullshit fix for https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/533f4054fabb27481b26624a
    // We need to redo onboarding, so this is just a quick fix to prevent the crashes during the sign in flow
    private boolean isBeingDestroyed = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        eventBus = SoundCloudApplication.fromContext(this).getEventBus();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this.getClass()));

        accountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (accountAuthenticatorResponse != null) {
            accountAuthenticatorResponse.onRequestContinued();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBeingDestroyed = false;
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this.getClass()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        isBeingDestroyed = true;
        super.onSaveInstanceState(outState);
    }

    public void finish() {
        if (accountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (resultBundle != null) {
                accountAuthenticatorResponse.onResult(resultBundle);
            } else {
                accountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            }
            accountAuthenticatorResponse = null;
        }
        super.finish();
    }

    /**
     * Used for creating SoundCloud account from {@link FacebookWebFlowActivity} and {@link FacebookSSOActivity}
     * @param data contains grant data and FB token
     */
    protected void login(Bundle data) {
        if (!isBeingDestroyed) {
            LoginTaskFragment.create(data).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
        }
    }

    @Override
    public void onAuthTaskComplete(User user, SignupVia via, boolean shouldAddUserInfo){
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        result.putBoolean(Consts.Keys.WAS_SIGNUP, via != SignupVia.NONE);
        resultBundle = result;

        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                .putExtra(User.EXTRA_ID, user.getId())
                .putExtra(SignupVia.EXTRA, via.name));

        if (result.getBoolean(Consts.Keys.WAS_SIGNUP) || wasAuthorizedViaSignupScreen()) {
            startActivity(new Intent(this, SuggestedUsersActivity.class)
                    .putExtra(SuggestedUsersCategoriesFragment.SHOW_FACEBOOK, this instanceof FacebookBaseActivity)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } else {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        finish();
    }

    protected abstract boolean wasAuthorizedViaSignupScreen();

    @Override
    public void onError(String message){
        if (!isFinishing()) {
            if (!TextUtils.isEmpty(message)){
                new AlertDialog.Builder(AbstractLoginActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getString(R.string.authentication_error_title))
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .create()
                        .show();
            }
        }
    }

    protected void setBundle(Bundle bundle){
        this.resultBundle = bundle;
    }
}

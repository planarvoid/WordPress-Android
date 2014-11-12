package com.soundcloud.android.onboarding.auth;


import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.HttpProperties;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoriesFragment;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.util.Locale;

@SuppressWarnings("PMD.RedundantFieldInitializer")
public abstract class AbstractLoginActivity extends FragmentActivity implements AuthTaskFragment.OnAuthResultListener {
    protected static final String LOGIN_DIALOG_TAG = "login_dialog";
    private static final String SIGNUP_WITH_CAPTCHA_URI = "https://soundcloud.com/connect?c=true&highlight=signup&client_id=%s&redirect_uri=soundcloud://auth&response_type=code&scope=non-expiring";
    private static final String EXTRA_WAS_SIGNUP = "wasSignup";

    /**
     * Extracted account authenticator functions. Extracted because of Fragment usage, we have to extend FragmentActivity.
     * See {@link AccountAuthenticatorActivity} for documentation
     */
    private AccountAuthenticatorResponse accountAuthenticatorResponse;
    private Bundle resultBundle;

    private HttpProperties httpProperties;
    private EventBus eventBus;

    // a bullshit fix for https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/533f4054fabb27481b26624a
    // We need to redo onboarding, so this is just a quick fix to prevent the crashes during the sign in flow
    private boolean isBeingDestroyed = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        httpProperties = new HttpProperties();
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
     * Used for creating SoundCloud account from Facebook SDK
     *
     * @param data contains grant data and FB token
     */
    protected void login(Bundle data) {
        if (!isBeingDestroyed) {
            LoginTaskFragment.create(data).show(getSupportFragmentManager(), LOGIN_DIALOG_TAG);
        }
    }

    @Override
    public void onAuthTaskComplete(PublicApiUser user, SignupVia via, boolean shouldAddUserInfo) {
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, user.username);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
        result.putBoolean(EXTRA_WAS_SIGNUP, via != SignupVia.NONE);
        resultBundle = result;

        sendBroadcast(new Intent(Actions.ACCOUNT_ADDED)
                .putExtra(PublicApiUser.EXTRA_ID, user.getId())
                .putExtra(SignupVia.EXTRA, via.name));

        if (result.getBoolean(EXTRA_WAS_SIGNUP) || wasAuthorizedViaSignupScreen()) {
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
    public void onError(String message) {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(TextUtils.isEmpty(message) ? getString(R.string.authentication_signup_error_message) : message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupGeneralError());
    }

    @Override
    public void onEmailTaken() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_email_taken_message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupExistingEmail());
    }

    @Override
    public void onSpam() {
        final SpamDialogOnClickListener spamDialogOnClickListener = new SpamDialogOnClickListener();
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_captcha_message)
                .setPositiveButton(getString(R.string.try_again), spamDialogOnClickListener)
                .setNeutralButton(getString(R.string.cancel), spamDialogOnClickListener);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupServeCaptcha());
    }

    @Override
    public void onBlocked() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_blocked_title)
                .setMessage(R.string.authentication_blocked_message)
                .setPositiveButton(R.string.close, null);
        showDialogWithHiperLinksAndTrackEvent(dialogBuilder, OnboardingEvent.signupDenied());
    }

    @Override
    public void onEmailInvalid() {
        final AlertDialog.Builder dialogBuilder = createDefaultAuthErrorDialogBuilder(R.string.authentication_error_title)
                .setMessage(R.string.authentication_email_invalid_message)
                .setPositiveButton(android.R.string.ok, null);
        showDialogAndTrackEvent(dialogBuilder, OnboardingEvent.signupInvalidEmail());
    }

    private void showDialogAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            dialogBuilder
                    .create()
                    .show();
            eventBus.publish(EventQueue.ONBOARDING, event);
        }
    }

    private void showDialogWithHiperLinksAndTrackEvent(AlertDialog.Builder dialogBuilder, OnboardingEvent event) {
        if (!isFinishing()) {
            final AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();

            final TextView messageView = (TextView) alertDialog.findViewById(android.R.id.message);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());

            eventBus.publish(EventQueue.ONBOARDING, event);
        }
    }

    private AlertDialog.Builder createDefaultAuthErrorDialogBuilder(int title) {
        return new AlertDialog.Builder(AbstractLoginActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(title));
    }

    private class SpamDialogOnClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    onCaptchaRequested();
                    dialog.dismiss();
                    break;
                default:
                    dialog.dismiss();
            }
        }
    }

    private void onCaptchaRequested() {
        String uriString = String.format(Locale.US, SIGNUP_WITH_CAPTCHA_URI, httpProperties.getClientId());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        startActivity(intent);
    }

    protected void setBundle(Bundle bundle) {
        this.resultBundle = bundle;
    }
}

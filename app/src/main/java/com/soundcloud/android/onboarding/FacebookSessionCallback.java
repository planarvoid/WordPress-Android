package com.soundcloud.android.onboarding;


import static com.soundcloud.android.onboarding.OnboardActivity.ONBOARDING_TAG;
import static com.soundcloud.android.utils.ErrorUtils.log;
import static java.util.Collections.singletonList;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.soundcloud.android.utils.ErrorUtils;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

public class FacebookSessionCallback implements FacebookCallback<LoginResult> {
    static final List<String> DEFAULT_FACEBOOK_READ_PERMISSIONS = Arrays.asList("public_profile",
                                                                                "email",
                                                                                "user_birthday",
                                                                                "user_friends",
                                                                                "user_likes");
    static List<String> EMAIL_ONLY_PERMISSION = singletonList("email");

    private final WeakReference<FacebookLoginCallbacks> activityRef;

    public FacebookSessionCallback(FacebookLoginCallbacks callbacks) {
        this.activityRef = new WeakReference<>(callbacks);
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        log(Log.INFO, ONBOARDING_TAG, "Facebook authorization succeeded");
        FacebookLoginCallbacks callbacks = activityRef.get();
        if (callbacks != null) {
            handleSuccessWithActivity(loginResult, callbacks);
        } else {
            log(Log.WARN, ONBOARDING_TAG, "Facebook callback called but activity was garbage collected.");
        }
    }

    private void handleSuccessWithActivity(LoginResult loginResult, FacebookLoginCallbacks callbacks) {
        if (loginResult.getRecentlyDeniedPermissions().contains("email")) {
            callbacks.confirmRequestForFacebookEmail();
        } else {
            callbacks.loginWithFacebook(loginResult.getAccessToken().getToken());
        }
    }

    @Override
    public void onCancel() {
        Log.i(ONBOARDING_TAG, "Facebook authorization cancelled");
    }

    @Override
    public void onError(FacebookException e) {
        log(Log.ERROR, ONBOARDING_TAG, "Facebook authorization returned an exception " + e.getMessage());
        ErrorUtils.handleSilentException(e);

        FacebookLoginCallbacks callbacks = activityRef.get();
        if (callbacks != null) {
            handleError(e, callbacks);
        } else {
            log(Log.WARN, ONBOARDING_TAG, "Facebook callback called but activity was garbage collected.");
        }
    }

    private static void handleError(FacebookException e, FacebookLoginCallbacks callbacks) {
        if (isConnectionError(e)) {
            callbacks.onFacebookConnectionErrorMessage();
        } else {
            callbacks.onFacebookAuthenticationFailedMessage();
        }
    }

    private static boolean isConnectionError(FacebookException e) {
        return e instanceof FacebookAuthorizationException && e.getMessage().equals("CONNECTION_FAILURE: CONNECTION_FAILURE");
    }

    interface FacebookLoginCallbacks {
        void loginWithFacebook(String facebookToken);

        void confirmRequestForFacebookEmail();

        void onFacebookAuthenticationFailedMessage();

        void onFacebookConnectionErrorMessage();
    }

}

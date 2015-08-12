package com.soundcloud.android.onboarding;


import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.soundcloud.android.R;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import static com.soundcloud.android.utils.Log.ONBOARDING_TAG;

public class FacebookSessionCallback implements FacebookCallback<LoginResult> {
    static final List<String> DEFAULT_FACEBOOK_READ_PERMISSIONS = Arrays.asList("public_profile", "email", "user_birthday", "user_friends", "user_likes");

    private final WeakReference<OnboardActivity> activityRef;
    private final TokenInformationGenerator tokenUtils;

    public FacebookSessionCallback(OnboardActivity onboardActivity, TokenInformationGenerator tokenUtils) {
        this.activityRef = new WeakReference<>(onboardActivity);
        this.tokenUtils = tokenUtils;
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        Log.i(ONBOARDING_TAG, "Facebook authorization succeeded");
        OnboardActivity activity = activityRef.get();
        if (activity != null) {
            activity.login(tokenUtils.getGrantBundle(OAuth.GRANT_TYPE_FACEBOOK, AccessToken.getCurrentAccessToken().getToken()));
        } else {
            Log.w(ONBOARDING_TAG, "Facebook callback called but activity was garbage collected.");
        }
    }

    @Override
    public void onCancel() {
        Log.i(ONBOARDING_TAG, "Facebook authorization cancelled");
    }

    @Override
    public void onError(FacebookException e) {
        Log.w(ONBOARDING_TAG, "Facebook authorization returned an exception", e);
        ErrorUtils.handleSilentException(e);

        OnboardActivity activity = activityRef.get();
        if (activity != null) {
            final boolean allowUserFeedback = true;
            activity.onError(activity.getString(R.string.facebook_authentication_failed_message), allowUserFeedback);
        } else {
            Log.w(ONBOARDING_TAG, "Facebook callback called but activity was garbage collected.");
        }
    }

}

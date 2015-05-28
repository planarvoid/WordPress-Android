package com.soundcloud.android.onboarding;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.oauth.OAuth;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

class FacebookSessionCallback implements Session.StatusCallback {
    static final List<String> DEFAULT_FACEBOOK_READ_PERMISSIONS = Arrays.asList("public_profile", "email", "user_birthday", "user_friends", "user_likes");
    static final String DEFAULT_FACEBOOK_PUBLISH_PERMISSION = "publish_actions";

    private final WeakReference<OnboardActivity> activityRef;

    public FacebookSessionCallback(OnboardActivity onboardActivity) {
        this.activityRef = new WeakReference<>(onboardActivity);
    }

    @Override
    public void call(Session session, SessionState state, Exception exception) {
        OnboardActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        if (state == SessionState.OPENED && !session.getPermissions().contains(DEFAULT_FACEBOOK_PUBLISH_PERMISSION)) {
            Session.NewPermissionsRequest newPermissionRequest = new Session.NewPermissionsRequest(
                    activity, DEFAULT_FACEBOOK_PUBLISH_PERMISSION);
            session.requestNewPublishPermissions(newPermissionRequest);
        } else if (ScTextUtils.isNotBlank(session.getAccessToken())) {
            TokenInformationGenerator tokenInformationGenerator = new TokenInformationGenerator(new PublicApi(activity));
            activity.login(tokenInformationGenerator.getGrantBundle(OAuth.GRANT_TYPE_FACEBOOK, session.getAccessToken()));
        } else if (exception != null && !(exception instanceof FacebookOperationCanceledException)) {
            Log.w(TAG, "Facebook returned an exception", exception);
            ErrorUtils.handleSilentException(exception);
            activity.onError(activity.getString(R.string.facebook_authentication_failed_message));
        }
    }
}

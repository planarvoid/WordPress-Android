package com.soundcloud.android.onboarding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.AccessToken;
import com.facebook.login.LoginResult;
import org.junit.Test;

import java.util.HashSet;

public class FacebookSessionCallbackTest {

    @Test
    public void shouldNotCrashIfActivityHasBeenGarbageCollected() {
        OnboardActivity onboardActivity = null;
        FacebookSessionCallback facebookSessionCallback = new FacebookSessionCallback(onboardActivity);

        facebookSessionCallback.onSuccess(new LoginResult(null, new HashSet<String>(), new HashSet<String>()));
    }

    @Test
    public void shouldRerequestEmailIfUserDeniedPermissionToIt() {
        HashSet<String> declinedPermissions = new HashSet<>();
        declinedPermissions.add("email");
        AccessToken accessToken = new AccessToken("foo", "bar", "baz", null, declinedPermissions, null, null, null);
        OnboardActivity onboardActivity = mock(OnboardActivity.class);
        FacebookSessionCallback facebookSessionCallback = new FacebookSessionCallback(onboardActivity);

        facebookSessionCallback.onSuccess(new LoginResult(accessToken, new HashSet<String>(), declinedPermissions));

        verify(onboardActivity).confirmRequestForFacebookEmail();
    }
}

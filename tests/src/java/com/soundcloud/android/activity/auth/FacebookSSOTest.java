package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class FacebookSSOTest {
    @Test
    public void shouldCreateAuthIntentWithPermissions() throws Exception {
        Intent intent = FacebookSSO.getAuthIntent(Robolectric.application,
                "foo", "bar");

        expect(intent.getAction()).toBeNull();
        expect(intent.getComponent().getPackageName()).toEqual("com.facebook.katana");
        expect(intent.getComponent().getClassName()).toEqual("com.facebook.katana.ProxyAuth");
        expect(intent.getStringExtra(FacebookSSO.FB_PERMISSION_EXTRA)).toEqual("foo,bar");
    }

    @Test
    public void shouldNotAddPermissionExtraIfNoPermissionsRequested() throws Exception {
        Intent intent = FacebookSSO.getAuthIntent(Robolectric.application);
        expect(intent.hasExtra(FacebookSSO.FB_PERMISSION_EXTRA)).toBeFalse();
    }


    @Test
    public void shouldAskForDefaultPermissions() throws Exception {
        FacebookSSO sso = new FacebookSSO() {
            @Override protected boolean validateAppSignatureForIntent(Intent intent) {
                // override signature check
                return true;
            }
        };
        sso.onCreate(null);
        ShadowActivity.IntentForResult act = shadowOf(sso).getNextStartedActivityForResult();
        expect(act).not.toBeNull();
        expect(act.intent.getStringExtra(FacebookSSO.FB_PERMISSION_EXTRA))
                .toEqual("publish_actions,offline_access,email,user_birthday");
    }

    @Test
    public void shouldReturnErrorIfSignatureIncorrect() throws Exception {
        FacebookSSO sso = new FacebookSSO();
        sso.onCreate(null);
        expect(shadowOf(sso).getResultCode()).toEqual(Activity.RESULT_OK);
        Intent result = shadowOf(sso).getResultIntent();
        expect(result.getStringExtra("error")).toEqual("fb app not installed or sig invalid");
    }

    @Test
    public void shouldDetectFacebookIntent() throws Exception {
        expect(FacebookSSO.isFacebookView(Robolectric.application, null)).toBeFalse();
        expect(FacebookSSO.isFacebookView(Robolectric.application, new Intent())).toBeFalse();
        expect(FacebookSSO.isFacebookView(Robolectric.application, new Intent("com.facebook.application.123"))).toBeFalse();
        expect(FacebookSSO.isFacebookView(Robolectric.application, new Intent("com.facebook.application.19507961798"))).toBeTrue();
    }
}

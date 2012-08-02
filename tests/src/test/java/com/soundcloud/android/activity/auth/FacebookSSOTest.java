package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.res.RobolectricPackageManager;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.os.IBinder;

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
        expect(FacebookSSO.handleFacebookView(Robolectric.application, null)).toBeFalse();
        expect(FacebookSSO.handleFacebookView(Robolectric.application, new Intent())).toBeFalse();
        expect(FacebookSSO.handleFacebookView(Robolectric.application, new Intent("com.facebook.application.123"))).toBeFalse();
        expect(FacebookSSO.handleFacebookView(Robolectric.application, new Intent("com.facebook.application.19507961798"))).toBeTrue();
    }

    @Test @Ignore
    public void shouldExtendAccessToken() throws Exception {
        pretendFacebookIsInstalled();

        final ShadowApplication shadowApplication = shadowOf(Robolectric.application);
        IBinder binder = mock(IBinder.class);
        shadowApplication.setComponentNameAndServiceForBindService(null, binder);

        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.extendAccessToken(token, Robolectric.application);

        Intent svc = shadowApplication.peekNextStartedService();
        expect(svc).not.toBeNull();
        expect(svc.getAction()).toEqual("");
    }

    private void pretendFacebookIsInstalled() {
        // pretend facebook is installed
        RobolectricPackageManager pm = (RobolectricPackageManager) Robolectric.application.getPackageManager();
        ResolveInfo info = new ResolveInfo();
        info.serviceInfo = new ServiceInfo();
        info.serviceInfo.packageName = FacebookSSO.FB_PACKAGE;
        PackageInfo pinfo = new PackageInfo();
        pinfo.packageName = info.serviceInfo.packageName;
        Signature sig = mock(Signature.class);
        when(sig.toCharsString()).thenReturn(FacebookSSO.FB_APP_SIGNATURE);
        pinfo.signatures = new Signature[] { sig };
        pm.addPackage(pinfo);
        pm.addResolveInfoForIntent(FacebookSSO.getRefreshIntent(), info);
    }

    @Test
    public void shouldSendTokenBackToBackend() throws Exception {
        Robolectric.addHttpResponseRule("https://graph.facebook.com/me",
                IOUtils.readInputStream(getClass().getResourceAsStream("me.json")));

        Robolectric.addHttpResponseRule("POST", "/i1/me/facebook_token", new TestHttpResponse(200, "OK"));
        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        expect(task.doInBackground(token)).toBeTrue();
    }

    @Test
    public void shouldSendTokenBackToBackendMalformedResponse() throws Exception {
        Robolectric.addHttpResponseRule("https://graph.facebook.com/me", "{ }");
        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        expect(task.doInBackground(token)).toBeFalse();
    }

    @Test
    public void shouldSendTokenBackToBackendMalformedResponse2() throws Exception {
        Robolectric.addHttpResponseRule("https://graph.facebook.com/me", "hahaha");
        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        expect(task.doInBackground(token)).toBeFalse();
    }

    @Test
    public void shouldSendTokenBackToBackendFBFailure() throws Exception {
        TestHelper.addPendingIOException("/me");
        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        expect(task.doInBackground(token)).toBeFalse();
    }

    @Test
    public void shouldSendTokenBackToBackendSCFailure() throws Exception {
        Robolectric.addHttpResponseRule("https://graph.facebook.com/me", "{ \"id\": \"666\" }");
        TestHelper.addPendingIOException("/i1/me/facebook_token");

        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() + 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        expect(task.doInBackground(token)).toBeFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSendStaleTokenBackToBackend() throws Exception {
        FacebookSSO.FBToken token = new FacebookSSO.FBToken("1234", System.currentTimeMillis() - 86400 * 1000);
        FacebookSSO.PostTokenTask task = new FacebookSSO.PostTokenTask(DefaultTestRunner.application);
        task.doInBackground(token);
    }

    @Test
    public void shouldGetTokenFromIntent() throws Exception {
        Intent intent = new Intent()
              .putExtra("access_token", "12345")
              .putExtra("expires_in", "3600");

        FacebookSSO.FBToken token = FacebookSSO.FBToken.fromIntent(intent);
        expect(token).not.toBeNull();
        expect(token.accessToken).toEqual("12345");
        expect(token.isExpired()).toBeFalse();
        expect(token.expires).toBeGreaterThan(System.currentTimeMillis());

        expect(FacebookSSO.FBToken.fromIntent(new Intent())).toBeNull();
        expect(FacebookSSO.FBToken.fromIntent(new Intent().putExtra("foo", "bar"))).toBeNull();
        expect(FacebookSSO.FBToken.fromIntent(new Intent().putExtra("access_token", "bar").putExtra("expires_in", "0")).expires).toBe(0L);
    }
}

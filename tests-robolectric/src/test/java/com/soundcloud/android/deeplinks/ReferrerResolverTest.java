package com.soundcloud.android.deeplinks;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class ReferrerResolverTest {
    private ReferrerResolver resolver;
    private Resources resources;

    @Before
    public void setUp() throws Exception {
        resolver = new ReferrerResolver();
        resources = Robolectric.application.getResources();
    }

    @Test
    public void shouldNotDetectFacebookIntentForIntentWithoutAction() throws Exception {
        expect(resolver.isFacebookAction(new Intent(), resources)).toBeFalse();
    }

    @Test
    public void shouldNotDetectFacebookIntentForIntentWithIncorrectAppId() throws Exception {
        Intent intent = new Intent("com.facebook.application.123");
        expect(resolver.isFacebookAction(intent, resources)).toBeFalse();
    }

    @Test
    public void shouldDetectFacebookIntentViaAction() throws Exception {
        Intent intent = new Intent("com.facebook.application.19507961798");
        expect(resolver.isFacebookAction(intent, resources)).toBeTrue();
    }

    @Test
    public void shouldDetectFacebookIntentViaAppId() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("app_id", 19507961798l);
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.FACEBOOK);
    }

    @Test
    public void shouldNotDetectFacebookIntentViaOtherAppIds() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("app_id", 101l);
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.OTHER);
    }

    @Test
    public void shouldDetectOriginParameters() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=mobi"));
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.MOBI);
    }

    @Test
    public void shouldNotDetectMissingOrigin() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.OTHER);
    }

    @Test
    public void shouldNotDetectEmptyOrigin() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin="));
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.OTHER);
    }

    @Test
    public void shouldIgnoreCaseOfOrigin() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=tWiTtEr"));
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.TWITTER);
    }

    @Test
    public void shouldDetectOriginBeforeOtherIntentData() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=twitter"));
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.TWITTER);
    }

    @Test
    public void shouldUseFallbackToIntentDataWhenOriginIsEmpty() throws Exception {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin="));
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.GOOGLE_PLUS);
    }

    @Test
    public void shouldDetectFacebookReferrer() throws Exception {
        Intent intent = new Intent("com.facebook.application.19507961798");
        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.FACEBOOK);
    }

    @Test
    public void shouldDetectTwitterReferrer() throws Exception {
        Intent ancestorIntent = new Intent();
        ancestorIntent.setComponent(new ComponentName("com.twitter.android", "some-class"));
        Intent intent = new Intent();
        intent.putExtra("intent.extra.ANCESTOR", ancestorIntent);

        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.TWITTER);
    }

    @Test
    public void shouldDetectGooglePlusReferrer() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");

        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.GOOGLE_PLUS);
    }

    @Test
    public void shouldDetectBrowserReferrer() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("Referer", "http://www.google.com/");
        Intent intent = new Intent();
        intent.putExtra("com.android.browser.headers", bundle);

        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.GOOGLE);
    }

    @Test
    public void shouldNotDetectBrowserReferrerForUnknownHosts() throws Exception {
        Intent intent = new Intent();
        intent.putExtra("com.android.browser.headers", "http://www.lmgtfy.com/");

        expect(resolver.getReferrerFromIntent(intent, resources)).toEqual(Referrer.OTHER);
    }

    @Test
    public void shouldDetectOtherReferrerAsDefault() throws Exception {
        expect(resolver.getReferrerFromIntent(new Intent(), resources)).toEqual(Referrer.OTHER);
    }
}

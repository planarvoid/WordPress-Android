package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

public class ReferrerResolverTest extends AndroidUnitTest {
    private ReferrerResolver resolver;
    private Resources resources;

    @Before
    public void setUp() {
        resolver = new ReferrerResolver();
        resources = resources();
    }

    @Test
    public void shouldNotDetectFacebookIntentForIntentWithoutAction() {
        assertThat(resolver.getReferrerFromIntent(new Intent(), resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldNotDetectFacebookIntentForIntentWithIncorrectAppId() {
        Intent intent = new Intent("com.facebook.application.123");
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldDetectFacebookIntentViaAction() {
        Intent intent = new Intent("com.facebook.application.19507961798");
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.FACEBOOK.value());
    }

    @Test
    public void shouldDetectFacebookIntentViaAppId() {
        Intent intent = new Intent();
        intent.putExtra("app_id", 19507961798L);
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.FACEBOOK.value());
    }

    @Test
    public void shouldNotDetectFacebookIntentViaOtherAppIds() {
        Intent intent = new Intent();
        intent.putExtra("app_id", 101L);
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldDetectOriginParameters() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=mobi"));
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.MOBI.value());
    }

    @Test
    public void shouldNotDetectOriginParametersOnOpaqueUris() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud:sounds:1234?origin=mobi"));
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldNotDetectMissingOrigin() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldNotDetectEmptyOrigin() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin="));
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldIgnoreCaseOfOrigin() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=tWiTtEr"));
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.TWITTER.value());
    }

    @Test
    public void shouldDetectOriginBeforeOtherIntentData() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=twitter"));
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.TWITTER.value());
    }

    @Test
    public void shouldUseFallbackToIntentDataWhenOriginIsEmpty() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin="));
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.GOOGLE_PLUS.value());
    }

    @Test
    public void shouldDetectFacebookReferrer() {
        Intent intent = new Intent("com.facebook.application.19507961798");
        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.FACEBOOK.value());
    }

    @Test
    public void shouldDetectTwitterReferrer() {
        Intent ancestorIntent = new Intent();
        ancestorIntent.setComponent(new ComponentName("com.twitter.android", "some-class"));
        Intent intent = new Intent();
        intent.putExtra("intent.extra.ANCESTOR", ancestorIntent);

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.TWITTER.value());
    }

    @Test
    public void shouldDetectGooglePlusReferrer() {
        Intent intent = new Intent();
        intent.putExtra("com.android.browser.application_id", "com.google.android.apps.plus");

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.GOOGLE_PLUS.value());
    }

    @Test
    public void shouldDetectBrowserReferrer() {
        Bundle bundle = new Bundle();
        bundle.putString("Referer", "http://www.google.com/");
        Intent intent = new Intent();
        intent.putExtra("com.android.browser.headers", bundle);

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo("google.com");
    }

    @Test
    public void shouldDetectOtherReferrerAsDefault() {
        assertThat(resolver.getReferrerFromIntent(new Intent(), resources)).isEqualTo(Referrer.OTHER.value());
    }

    @Test
    public void shouldDetectGoogleCrawlerForReferrer() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
        intent.putExtra("android.intent.extra.REFERRER", Uri.parse("android-app://com.google.appcrawler"));

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.GOOGLE_CRAWLER.value());
    }

    @Test
    public void shouldDetectGoogleCrawlerForReferrerName() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
        intent.putExtra("android.intent.extra.REFERRER_NAME", "android-app://com.google.appcrawler");

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.GOOGLE_CRAWLER.value());
    }

    @Test
    public void shouldDetectIntentReferrer() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234"));
        Referrer.APPBOY_NOTIFICATION.addToIntent(intent);

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo(Referrer.APPBOY_NOTIFICATION.value());
    }

    @Test
    public void shouldDetectRefParams() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://soundcloudgo?ref=t6001"));

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo("t6001");
    }

    @Test
    public void shouldPrioritizeRefParamOverIntentReferrer() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://soundcloudgo?ref=t6001"));
        Referrer.APPBOY_NOTIFICATION.addToIntent(intent);

        assertThat(resolver.getReferrerFromIntent(intent, resources)).isEqualTo("t6001");
    }

}

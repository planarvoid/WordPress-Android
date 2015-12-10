package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;

public class ReferrerTest extends AndroidUnitTest {

    @Test
    public void shouldGetTrackingTag() {
        assertThat(Referrer.FACEBOOK.get()).isEqualTo("facebook");
    }

    @Test
    public void setsAndGetsReferrerFromIntent() {
        final Intent intent = new Intent();
        Referrer.FACEBOOK.addToIntent(intent);
        assertThat(Referrer.fromIntent(intent)).isEqualTo(Referrer.FACEBOOK);
    }

    @Test
    public void getsReferrerFromOrigin() {
        assertThat(Referrer.fromOrigin("facebook")).isEqualTo(Referrer.FACEBOOK);
    }

    @Test
    public void getsDefaultReferrerFromUnknownOrigin() {
        assertThat(Referrer.fromOrigin("bing")).isEqualTo(Referrer.OTHER);
    }

    @Test
    public void getsReferrerFromHost() {
        assertThat(Referrer.fromHost("google.com")).isEqualTo(Referrer.GOOGLE);
    }

    @Test
    public void getsDefaultReferrerFromUnknownHost() {
        assertThat(Referrer.fromHost("bing.com")).isEqualTo(Referrer.OTHER);
    }

    @Test
    public void getsReferrerFromHostWithWww() {
        assertThat(Referrer.fromHost("www.facebook.com")).isEqualTo(Referrer.FACEBOOK);
    }
}

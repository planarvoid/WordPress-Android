package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.Intent;

public class ReferrerTest extends AndroidUnitTest {

    @Test
    public void shouldGetTrackingTag() {
        assertThat(Referrer.FACEBOOK.value()).isEqualTo("facebook");
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
    public void getsReferrerFromUrl() {
        assertThat(Referrer.fromUrl("http://facebook.com/")).isEqualTo("facebook.com");
    }

    @Test
    public void getsReferrerFromUrlWithWww() {
        assertThat(Referrer.fromUrl("http://www.facebook.com/")).isEqualTo("facebook.com");
    }

    @Test
    public void removesReferrerFromIntent() throws Exception {
        Intent intent = new Intent();
        Referrer.PLAYBACK_WIDGET.addToIntent(intent);

        Referrer.removeFromIntent(intent);

        assertThat(Referrer.hasReferrer(intent)).isFalse();
    }
}

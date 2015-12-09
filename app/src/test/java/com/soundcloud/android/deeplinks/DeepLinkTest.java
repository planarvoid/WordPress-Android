package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.net.Uri;

public class DeepLinkTest extends AndroidUnitTest {

    @Test
    public void shouldFlagResetPasswordUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/login/reset/123456789abcdef");
    }

    @Test
    public void shouldFlagForgotUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/login/forgot");
    }

    @Test
    public void shouldFlagEmailConfirmationUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/emails/123456789abcdef");
    }

    @Test
    public void shouldFlagOtherLinksAsOther() {
        assertDeeplink(DeepLink.ENTITY, "https://soundcloud.com/settings");
    }

    @Test
    public void shouldHandleWebHomeTypes() {
        assertThat(DeepLink.fromUri(null)).isEqualTo(DeepLink.HOME);
        assertThat(DeepLink.fromUri(Uri.parse("http://soundcloud.com"))).isEqualTo(DeepLink.HOME);
        assertThat(DeepLink.fromUri(Uri.parse("http://soundcloud.com/"))).isEqualTo(DeepLink.HOME);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/"))).isEqualTo(DeepLink.HOME);
        assertThat(DeepLink.fromUri(Uri.parse("https://m.soundcloud.com/"))).isEqualTo(DeepLink.HOME);
    }

    @Test
    public void shouldHandleOtherWebUrlsAsOther() {
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex"))).isEqualTo(DeepLink.ENTITY);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex/some-track"))).isEqualTo(DeepLink.ENTITY);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex/sets/some-playlist"))).isEqualTo(DeepLink.ENTITY);
    }

    @Test
    public void shouldRequireResolve() {
        assertThat(DeepLink.ENTITY.requiresResolve()).isTrue();
        assertThat(DeepLink.EXPLORE.requiresResolve()).isFalse();
        assertThat(DeepLink.SEARCH.requiresResolve()).isFalse();
        assertThat(DeepLink.RECORD.requiresResolve()).isFalse();
        assertThat(DeepLink.HOME.requiresResolve()).isFalse();
        assertThat(DeepLink.STREAM.requiresResolve()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresResolve()).isFalse();
    }

    @Test
    public void shouldRequireLoggedIn() {
        assertThat(DeepLink.EXPLORE.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.ENTITY.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.SEARCH.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.RECORD.requiresLoggedInUser()).isTrue();

        assertThat(DeepLink.HOME.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.STREAM.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresLoggedInUser()).isFalse();
    }

    private void assertDeeplink(DeepLink deepLink, String url) {
        assertThat(DeepLink.fromUri(Uri.parse(url))).isEqualTo(deepLink);
    }
}

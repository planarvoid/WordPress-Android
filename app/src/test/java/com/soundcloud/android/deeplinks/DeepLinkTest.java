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
    public void shouldFlagEmailConfirmationUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/emails/123456789abcdef");
    }

    @Test
    public void shouldFlagOtherLinksAsOther() {
        assertDeeplink(DeepLink.OTHER, "https://soundcloud.com/settings");
    }

    @Test
    public void shouldFlagSoundCloudScheme() {
        assertDeeplink(DeepLink.HOME, "soundcloud://home");
        assertDeeplink(DeepLink.EXPLORE, "soundcloud://explore");
        assertDeeplink(DeepLink.RECORD, "soundcloud://upload");
        assertDeeplink(DeepLink.TRACK, "soundcloud://sounds:123456");
        assertDeeplink(DeepLink.TRACK, "soundcloud://tracks:123456");
        assertDeeplink(DeepLink.PLAYLIST, "soundcloud://playlists:123456");
        assertDeeplink(DeepLink.USER, "soundcloud://users:123456");
        assertDeeplink(DeepLink.OTHER, "soundcloud://skrillex:123456");
    }

    @Test
    public void shouldFlagSoundCloudUrns() {
        assertDeeplink(DeepLink.PLAYLIST, "soundcloud:playlists:123456");
        assertDeeplink(DeepLink.TRACK, "soundcloud:sounds:123456");
        assertDeeplink(DeepLink.TRACK, "soundcloud:tracks:123456");
        assertDeeplink(DeepLink.USER, "soundcloud:users:123456");
        assertDeeplink(DeepLink.OTHER, "soundcloud:skrillex:123456");
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
        assertThat(DeepLink.fromUri(Uri.parse(""))).isEqualTo(DeepLink.OTHER);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex"))).isEqualTo(DeepLink.OTHER);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex/other"))).isEqualTo(DeepLink.OTHER);
        assertThat(DeepLink.fromUri(Uri.parse("https://soundcloud.com/skrillex/sets/other"))).isEqualTo(DeepLink.OTHER);
    }

    @Test
    public void shouldRequireResolve() {
        assertThat(DeepLink.TRACK.requiresResolve()).isTrue();
        assertThat(DeepLink.PLAYLIST.requiresResolve()).isTrue();
        assertThat(DeepLink.USER.requiresResolve()).isTrue();

        assertThat(DeepLink.EXPLORE.requiresResolve()).isFalse();
        assertThat(DeepLink.SEARCH.requiresResolve()).isFalse();
        assertThat(DeepLink.RECORD.requiresResolve()).isFalse();
        assertThat(DeepLink.HOME.requiresResolve()).isFalse();
        assertThat(DeepLink.STREAM.requiresResolve()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresResolve()).isFalse();
        assertThat(DeepLink.OTHER.requiresResolve()).isFalse();
    }

    @Test
    public void shouldRequireLoggedIn() {
        assertThat(DeepLink.EXPLORE.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.USER.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.TRACK.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.PLAYLIST.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.SEARCH.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.RECORD.requiresLoggedInUser()).isTrue();

        assertThat(DeepLink.HOME.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.STREAM.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.OTHER.requiresLoggedInUser()).isFalse();
    }

    private void assertDeeplink(DeepLink deepLink, String url) {
        assertThat(DeepLink.fromUri(Uri.parse(url))).isEqualTo(deepLink);
    }
}

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
        assertThat(DeepLink.NOTIFICATION_PREFERENCES.requiresResolve()).isFalse();
    }

    @Test
    public void shouldRequireLoggedIn() {
        assertThat(DeepLink.EXPLORE.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.ENTITY.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.SEARCH.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.RECORD.requiresLoggedInUser()).isTrue();
        assertThat(DeepLink.NOTIFICATION_PREFERENCES.requiresLoggedInUser()).isTrue();

        assertThat(DeepLink.HOME.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.STREAM.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresLoggedInUser()).isFalse();
    }

    @Test
    public void shouldHandleSoundcloudScheme() {
        assertDeeplink(DeepLink.HOME, "soundcloud://home");
        assertDeeplink(DeepLink.STREAM, "soundcloud://stream");
        assertDeeplink(DeepLink.EXPLORE, "soundcloud://explore");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:people");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:sounds");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:sets");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:users");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:tracks");
        assertDeeplink(DeepLink.SEARCH, "soundcloud://search:playlists");
        assertDeeplink(DeepLink.RECORD, "soundcloud://record");
        assertDeeplink(DeepLink.RECORD, "soundcloud://upload");
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "soundcloud://discover");
        assertDeeplink(DeepLink.DISCOVERY, "soundcloud://discovery");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "soundcloud://soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "soundcloud://go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_BUY, "soundcloud://buysoundcloudgo");
        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "soundcloud://settings_offlinelistening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "soundcloud://notification_preferences");
        assertDeeplink(DeepLink.COLLECTION, "soundcloud://collection");
        assertDeeplink(DeepLink.ENTITY, "soundcloud://anythingelse");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts");
        assertDeeplink(DeepLink.TRACK_ENTITY, "soundcloud://tracks/123");
        assertDeeplink(DeepLink.PLAYLIST_ENTITY, "soundcloud://playlists/123");
        assertDeeplink(DeepLink.USER_ENTITY, "soundcloud://users/123");
    }

    @Test
    public void shouldHandleSoundcloudUrls() {
        assertThat(DeepLink.fromUri(null)).isEqualTo(DeepLink.HOME);
        assertDeeplink(DeepLink.HOME, "http://soundcloud.com");
        assertDeeplink(DeepLink.HOME, "http://soundcloud.com/");
        assertDeeplink(DeepLink.HOME, "https://soundcloud.com/");
        assertDeeplink(DeepLink.HOME, "https://m.soundcloud.com/");
        assertDeeplink(DeepLink.HOME, "https://www.soundcloud.com");
        assertDeeplink(DeepLink.HOME, "https://www.soundcloud.com/home");
        assertDeeplink(DeepLink.HOME, "https://www.soundcloud.com/home");
        assertDeeplink(DeepLink.STREAM, "https://www.soundcloud.com/stream");
        assertDeeplink(DeepLink.EXPLORE, "https://www.soundcloud.com/explore");
        assertDeeplink(DeepLink.RECORD, "https://www.soundcloud.com/upload");
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "https://www.soundcloud.com/discover");
        assertDeeplink(DeepLink.CHARTS, "https://www.soundcloud.com/charts");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/sounds");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/people");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/sets");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/tracks");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/users");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/search/playlists");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/tracks/search");
        assertDeeplink(DeepLink.SEARCH, "https://www.soundcloud.com/people/search");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://www.soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://www.soundcloud.com/go");
    }

    @Test
    public void shouldHandleWebSoundcloudGoTypes() {
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "http://soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "http://m.soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://m.soundcloud.com/go");

        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "http://soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "http://m.soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_UPSELL, "https://m.soundcloud.com/soundcloudgo");
    }

    private void assertDeeplink(DeepLink deepLink, String url) {
        assertThat(DeepLink.fromUri(Uri.parse(url))).isEqualTo(deepLink);
    }
}

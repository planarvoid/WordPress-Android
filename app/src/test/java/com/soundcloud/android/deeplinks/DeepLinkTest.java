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
        for (DeepLink deepLink : DeepLink.RESOLVE_REQUIRED) {
            assertThat(deepLink.requiresResolve()).isTrue();
        }

        assertThat(DeepLink.TRACK_RECOMMENDATIONS.requiresResolve()).isFalse();
        assertThat(DeepLink.DISCOVERY.requiresResolve()).isFalse();
        assertThat(DeepLink.SEARCH.requiresResolve()).isFalse();
        assertThat(DeepLink.RECORD.requiresResolve()).isFalse();
        assertThat(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL.requiresResolve()).isFalse();
        assertThat(DeepLink.SOUNDCLOUD_GO_PLUS_BUY.requiresResolve()).isFalse();
        assertThat(DeepLink.NOTIFICATION_PREFERENCES.requiresResolve()).isFalse();
        assertThat(DeepLink.COLLECTION.requiresResolve()).isFalse();
        assertThat(DeepLink.OFFLINE_SETTINGS.requiresResolve()).isFalse();
        assertThat(DeepLink.CHARTS.requiresResolve()).isFalse();
        assertThat(DeepLink.CHARTS_ALL_GENRES.requiresResolve()).isFalse();
        assertThat(DeepLink.HOME.requiresResolve()).isFalse();
        assertThat(DeepLink.STREAM.requiresResolve()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresResolve()).isFalse();
        assertThat(DeepLink.SHARE_APP.requiresResolve()).isFalse();
        assertThat(DeepLink.SYSTEM_SETTINGS.requiresResolve()).isFalse();
    }

    @Test
    public void shouldRequireLoggedIn() {
        for (DeepLink deepLink : DeepLink.LOGGED_IN_REQUIRED) {
            assertThat(deepLink.requiresLoggedInUser()).isTrue();
        }

        assertThat(DeepLink.HOME.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.STREAM.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.WEB_VIEW.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.SHARE_APP.requiresLoggedInUser()).isFalse();
        assertThat(DeepLink.SYSTEM_SETTINGS.requiresLoggedInUser()).isFalse();
    }

    @Test
    public void shouldHandleHierarchicalSoundcloudScheme() {
        assertDeeplink(DeepLink.HOME, "soundcloud://home");
        assertDeeplink(DeepLink.STREAM, "soundcloud://stream");
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
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "soundcloud://suggestedtracks_all");
        assertDeeplink(DeepLink.DISCOVERY, "soundcloud://discovery");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "soundcloud://soundcloudgo/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "soundcloud://go/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "soundcloud://soundcloudgo/soundcloudgoplus");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "soundcloud://go/soundcloudgoplus");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_BUY, "soundcloud://buysoundcloudgo");
        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "soundcloud://settings_offlinelistening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "soundcloud://notification_preferences");
        assertDeeplink(DeepLink.COLLECTION, "soundcloud://collection");
        assertDeeplink(DeepLink.ENTITY, "soundcloud://anythingelse");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts");
        assertDeeplink(DeepLink.TRACK_ENTITY, "soundcloud://tracks/123");
        assertDeeplink(DeepLink.PLAYLIST_ENTITY, "soundcloud://playlists/123");
        assertDeeplink(DeepLink.USER_ENTITY, "soundcloud://users/123");
        assertDeeplink(DeepLink.SHARE_APP, "soundcloud://share/app");
        assertDeeplink(DeepLink.SYSTEM_SETTINGS, "soundcloud://open-notification-settings");

        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "soundcloud://settings/offline_listening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "soundcloud://settings/notification_preferences");
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "soundcloud://suggested_tracks/all");
        assertDeeplink(DeepLink.SHARE_APP, "soundcloud://share_app");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://ht_modal");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_BUY, "soundcloud://buy_ht");
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
        assertDeeplink(DeepLink.RECORD, "https://www.soundcloud.com/upload");
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "https://www.soundcloud.com/discover");
        assertDeeplink(DeepLink.TRACK_RECOMMENDATIONS, "https://www.soundcloud.com/suggestedtracks_all");
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
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://www.soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://www.soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "https://www.soundcloud.com/soundcloudgo/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "https://www.soundcloud.com/go/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "https://www.soundcloud.com/soundcloudgo/soundcloudgoplus");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "https://www.soundcloud.com/go/soundcloudgoplus");
        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "https://www.soundcloud.com/settings_offlinelistening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "https://www.soundcloud.com/notification_preferences");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "https://soundcloud.com/settings/notifications");
        assertDeeplink(DeepLink.SHARE_APP, "https://www.soundcloud.com/share/app");
        assertDeeplink(DeepLink.SYSTEM_SETTINGS, "https://www.soundcloud.com/open-notification-settings");
    }

    @Test
    public void shouldHandleWebSoundcloudGoTypes() {
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "http://soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "http://m.soundcloud.com/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://m.soundcloud.com/go");

        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "http://soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "http://m.soundcloud.com/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "https://m.soundcloud.com/soundcloudgo");
    }

    @Test
    public void shouldHandleChartsDeeplinks() throws Exception {
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/top");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:top:all");
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/new?genre=all-music");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:new:all");
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/top?genre=electronic");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:top:electronic");
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/new?genre=electronic");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:new:electronic");
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/top?genre=hiphoprap");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:top:hiphoprap");
        assertDeeplink(DeepLink.CHARTS, "https://soundcloud.com/charts/new?genre=hiphoprap");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts:new:hiphoprap");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts/new?genre=hiphoprap");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts/top?genre=hiphoprap");

        assertDeeplink(DeepLink.CHARTS_ALL_GENRES, "soundcloud://charts:music");
        assertDeeplink(DeepLink.CHARTS_ALL_GENRES, "soundcloud://charts:audio");
    }

    private void assertDeeplink(DeepLink deepLink, String url) {
        assertThat(DeepLink.fromUri(Uri.parse(url))).isEqualTo(deepLink);
    }
}

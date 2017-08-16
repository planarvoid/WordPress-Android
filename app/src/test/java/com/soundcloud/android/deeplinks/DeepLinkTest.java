package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Java6Assertions.assertThat;

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
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/signin/forgot");
    }

    @Test
    public void shouldFlagSoundCloudConnectUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/connect");
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/connect/");
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/connect/some-sub-path");
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/connect?response_type=code&client_id=1234567");

        assertDeeplink(DeepLink.ENTITY, "https://soundcloud.com/connection");
        assertDeeplink(DeepLink.ENTITY, "https://soundcloud.com/connected/");
    }

    @Test
    public void shouldFlagSoundCloudTrackedUrlsAsTrackedRedirectDeepLink() {
        assertDeeplink(DeepLink.TRACKED_REDIRECT, "http://soundcloud.com/-/t/click/postman-email-account_lifecycle-password_reset_request?url=http%3A%2F%2Fsoundcloud.com%2Flogin%2Freset%2F123456789abcdef1234567");
    }

    @Test
    public void shouldLinkExternalToUnknown() throws Exception {
        assertDeeplink(DeepLink.UNKNOWN, "http://www.google.com");
        assertDeeplink(DeepLink.UNKNOWN, "mailto:sctest@soundcloud.com");
        assertDeeplink(DeepLink.UNKNOWN, "sctest@soundcloud.com");
    }

    @Test
    public void shouldFlagEmailConfirmationUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/emails/123456789abcdef");
    }

    @Test
    public void shouldFlagProUrlsAsWebView() {
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/pro");
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/pro/gifts");
        assertDeeplink(DeepLink.WEB_VIEW, "https://soundcloud.com/pro/buy/pro");
        assertDeeplink(DeepLink.ENTITY, "https://soundcloud.com/promises");
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

        assertThat(DeepLink.DISCOVERY.requiresResolve()).isFalse();
        assertThat(DeepLink.SEARCH.requiresResolve()).isFalse();
        assertThat(DeepLink.RECORD.requiresResolve()).isFalse();
        assertThat(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL.requiresResolve()).isFalse();
        assertThat(DeepLink.SOUNDCLOUD_GO_BUY.requiresResolve()).isFalse();
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
        assertDeeplink(DeepLink.DISCOVERY, "soundcloud://discover");
        assertDeeplink(DeepLink.DISCOVERY, "soundcloud://discover/");
        assertDeeplink(DeepLink.ENTITY, "soundcloud://discover/unsupported");
        assertDeeplink(DeepLink.ENTITY, "soundcloud://discovery");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "soundcloud://soundcloudgo/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_CHOICE, "soundcloud://go/soundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "soundcloud://soundcloudgo/soundcloudgoplus");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_CHOICE, "soundcloud://go/soundcloudgoplus");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_BUY, "soundcloud://buysoundcloudgo");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_BUY, "soundcloud://buysoundcloudgoplus");
        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "soundcloud://settings_offlinelistening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "soundcloud://notification_preferences");
        assertDeeplink(DeepLink.COLLECTION, "soundcloud://collection");
        assertDeeplink(DeepLink.ENTITY, "soundcloud://anythingelse");
        assertDeeplink(DeepLink.CHARTS, "soundcloud://charts");
        assertDeeplink(DeepLink.TRACK_ENTITY, "soundcloud://tracks/123");
        assertDeeplink(DeepLink.PLAYLIST_ENTITY, "soundcloud://playlists/123");
        assertDeeplink(DeepLink.SYSTEM_PLAYLIST_ENTITY, "soundcloud://system-playlists/123");
        assertDeeplink(DeepLink.USER_ENTITY, "soundcloud://users/123");
        assertDeeplink(DeepLink.SHARE_APP, "soundcloud://share/app");
        assertDeeplink(DeepLink.SYSTEM_SETTINGS, "soundcloud://open-notification-settings");
        assertDeeplink(DeepLink.OFFLINE_SETTINGS, "soundcloud://settings/offline_listening");
        assertDeeplink(DeepLink.NOTIFICATION_PREFERENCES, "soundcloud://settings/notification_preferences");
        assertDeeplink(DeepLink.SHARE_APP, "soundcloud://share_app");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_UPSELL, "soundcloud://ht_modal");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_BUY, "soundcloud://buy_mt");
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
        assertDeeplink(DeepLink.DISCOVERY, "https://www.soundcloud.com/discover");
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
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_BUY, "https://www.soundcloud.com/go/buy/go");
        assertDeeplink(DeepLink.SOUNDCLOUD_GO_PLUS_BUY, "https://www.soundcloud.com/go/buy/go-plus");
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

    @Test
    public void shouldHandleStationDeeplinks() throws Exception {
        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/artist/123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/artist/123");
        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/artist/soundcloud:users:123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/artist/soundcloud:users:123");
        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/artist/soundcloud:artist-stations:123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/artist/soundcloud:artist-stations:123");

        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/track/123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/track/123");
        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/track/soundcloud:tracks:123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/track/soundcloud:tracks:123");
        assertDeeplink(DeepLink.STATION, "https://soundcloud.com/stations/track/soundcloud:track-stations:123");
        assertDeeplink(DeepLink.STATION, "soundcloud://stations/track/soundcloud:track-stations:123");
    }

    @Test
    public void shouldHandleTheUploadDeeplinks() throws Exception {
        assertDeeplink(DeepLink.THE_UPLOAD, "soundcloud://discover/new-tracks-for-you");
        assertDeeplink(DeepLink.THE_UPLOAD, "https://soundcloud.com/discover/new-tracks-for-you");
        assertDeeplink(DeepLink.THE_UPLOAD, "https://soundcloud.com/the-upload");
        assertDeeplink(DeepLink.THE_UPLOAD, "soundcloud://the-upload");
    }

    @Test
    public void shouldHandleRemoteSignInDeeplinks() {
        assertDeeplink(DeepLink.REMOTE_SIGN_IN, "https://soundcloud.com/activate");
        assertDeeplink(DeepLink.REMOTE_SIGN_IN, "https://soundcloud.com/activate/something");
        assertDeeplink(DeepLink.REMOTE_SIGN_IN, "https://soundcloud.com/activate/something/else");
        assertDeeplink(DeepLink.REMOTE_SIGN_IN, "https://soundcloud.com/activate?foo=bar");

        assertDeeplink(DeepLink.REMOTE_SIGN_IN, "soundcloud://remote-sign-in");

        assertDeeplink(DeepLink.ENTITY, "https://soundcloud.com/activate-whatever");
    }

    @Test
    public void shouldHandleLegacyDeeplinks() throws Exception {
        assertDeeplink(DeepLink.HOME, "https://www.soundcloud.com/suggestedtracks_all");
        assertDeeplink(DeepLink.HOME, "soundcloud://suggestedtracks_all");
        assertDeeplink(DeepLink.HOME, "soundcloud://suggested_tracks/all");
    }

    private void assertDeeplink(DeepLink deepLink, String url) {
        assertThat(DeepLink.fromUri(Uri.parse(url))).isEqualTo(deepLink);
    }
}

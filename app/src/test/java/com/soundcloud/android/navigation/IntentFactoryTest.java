package com.soundcloud.android.navigation;

import static com.soundcloud.android.model.Urn.forAd;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.eventlogger.DevEventLoggerMonitorActivity;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.creators.CreatorUtilsKt;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.RecordPermissionsActivity;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.main.DevEventLoggerMonitorReceiver;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresPresenter;
import com.soundcloud.android.olddiscovery.charts.ChartActivity;
import com.soundcloud.android.olddiscovery.charts.ChartTracksFragment;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class IntentFactoryTest extends AndroidUnitTest {

    @Mock Context context;

    @Test
    public void openNotificationPreferencesFromDeeplink() {
        assertIntent(IntentFactory.createNotificationPreferencesFromDeeplinkIntent(context))
                .opensActivity(NotificationPreferencesActivity.class)
                .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    @Test
    public void openNotificationPreferences() {
        assertIntent(IntentFactory.createNotificationPreferencesFromDeeplinkIntent(context))
                .opensActivity(NotificationPreferencesActivity.class);
    }

    @Test
    public void openHelpCenter() {
        final String helpCenter = "http://help.soundcloud.com";
        final Uri uri = Uri.parse(helpCenter);
        when(context.getString(R.string.url_support)).thenReturn(helpCenter);
        assertIntent(IntentFactory.createHelpCenterIntent(context))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(uri);
    }

    @Test
    public void openLegal() {
        assertIntent(IntentFactory.createLegalIntent(context))
                .opensActivity(LegalActivity.class);
    }

    @Test
    public void openAdClickthrough() {
        final Uri uri = Uri.parse("http://clickthroughurl.com");
        assertIntent(IntentFactory.createAdClickthroughIntent(uri))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(uri)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void openVideoFullScreen() {
        final Urn urn = forAd("network", "123");
        assertIntent(IntentFactory.createFullscreenVideoAdIntent(context, urn))
                .containsExtra(FullScreenVideoActivity.EXTRA_AD_URN, urn.getContent())
                .opensActivity(FullScreenVideoActivity.class);
    }

    @Test
    public void openVisualPrestitial() {
        assertIntent(IntentFactory.createPrestititalAdIntent(context))
                .opensActivity(PrestitialActivity.class);
    }

    @Test
    public void opensChartTracks() {
        final Urn genreUrn = new Urn("soundcloud:genre:123");
        final ChartType chartType = ChartType.TOP;
        final String header = "header";
        final ChartCategory chartCategory = ChartCategory.AUDIO;
        assertThat(IntentFactory.createChartsIntent(context, ChartDetails.create(chartType, genreUrn, chartCategory, Optional.of(header))))
                .containsExtra(ChartTracksFragment.EXTRA_GENRE_URN, genreUrn.getContent())
                .containsExtra(ChartTracksFragment.EXTRA_TYPE, chartType)
                .containsExtra(ChartTracksFragment.EXTRA_HEADER, header)
                .opensActivity(ChartActivity.class);
    }

    @Test
    public void opensAllGenres() {
        assertThat(IntentFactory.createAllGenresIntent(context, null))
                .opensActivity(AllGenresActivity.class);
    }

    @Test
    public void opensAllGenresFromDeeplink() throws Exception {
        assertThat(IntentFactory.createAllGenresIntent(context, ChartCategory.MUSIC))
                .containsExtra(AllGenresPresenter.EXTRA_CATEGORY, ChartCategory.MUSIC)
                .opensActivity(AllGenresActivity.class);
    }

    @Test
    public void openProfile() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        final Referrer referrer = Referrer.PLAYBACK_WIDGET;
        assertIntent(IntentFactory.createProfileIntent(context, urn, Optional.of(screen), Optional.of(searchQuerySourceInfo), Optional.of(referrer)))
                .containsExtra(ProfileActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .containsReferrer(referrer)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void openProfileReposts() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileRepostsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserRepostsActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserRepostsActivity.class);
    }

    @Test
    public void openProfileTracks() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileTracksIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserTracksActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserTracksActivity.class);
    }

    @Test
    public void openProfileLikes() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileLikesIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserLikesActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserLikesActivity.class);
    }

    @Test
    public void openProfileAlbums() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfileAlbumsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserAlbumsActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserAlbumsActivity.class);
    }

    @Test
    public void openProfilePlaylists() {
        final Urn urn = forUser(123);
        final Screen screen = Screen.UNKNOWN;
        final SearchQuerySourceInfo searchQuerySourceInfo = mock(SearchQuerySourceInfo.class);
        assertIntent(IntentFactory.createProfilePlaylistsIntent(context, urn, screen, Optional.of(searchQuerySourceInfo)))
                .containsExtra(UserPlaylistsActivity.EXTRA_USER_URN, urn.getContent())
                .containsExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo)
                .containsScreen(screen)
                .opensActivity(UserPlaylistsActivity.class);
    }

    @Test
    public void legacyOpenStationInfo() {
        final Urn someStation = Urn.forArtistStation(123L);
        assertThat(IntentFactory.createStationsInfoIntent(context, someStation, Optional.absent(), Optional.of(DiscoverySource.STATIONS)))
                .containsExtra(StationInfoActivity.EXTRA_SOURCE, DiscoverySource.STATIONS.value())
                .containsExtra(StationInfoActivity.EXTRA_URN, someStation.getContent())
                .opensActivity(StationInfoActivity.class);
    }

    @Test
    public void openStationInfo() {
        final Urn someStation = Urn.forArtistStation(123L);
        final Urn seedTrack = Urn.forTrack(123L);
        assertThat(IntentFactory.createStationsInfoIntent(context, someStation, Optional.of(seedTrack), Optional.of(DiscoverySource.STATIONS)))
                .containsExtra(StationInfoActivity.EXTRA_SOURCE, DiscoverySource.STATIONS.value())
                .containsExtra(StationInfoActivity.EXTRA_URN, someStation.getContent())
                .containsExtra(StationInfoActivity.EXTRA_SEED_URN, seedTrack.getContent())
                .opensActivity(StationInfoActivity.class);
    }

    @Test
    public void openOfflineSettingsOnboarding() {
        assertIntent(IntentFactory.createOfflineSettingsOnboardingIntent(context))
                .opensActivity(OfflineSettingsOnboardingActivity.class);
    }

    @Test
    public void openOfflineSettings() {
        assertIntent(IntentFactory.createOfflineSettingsIntent(context))
                .opensActivity(OfflineSettingsActivity.class);
    }

    @Test
    public void openSettings() {
        assertIntent(IntentFactory.createSettingsIntent(context))
                .opensActivity(SettingsActivity.class);
    }

    @Test
    public void openRecord() {
        Recording recording = mock(Recording.class);
        assertIntent(IntentFactory.createRecordIntent(context, Optional.of(recording), Screen.RECORD_MAIN))
                .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .containsExtra(Recording.EXTRA, recording)
                .opensActivity(RecordActivity.class);
    }

    @Test
    public void openRecordPermission() {
        Recording recording = mock(Recording.class);
        assertIntent(IntentFactory.createRecordPermissionIntent(context, Optional.of(recording), Screen.RECORD_MAIN))
                .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .containsExtra(Recording.EXTRA, recording)
                .opensActivity(RecordPermissionsActivity.class);
    }

    @Test
    public void createDevEventLoggerMonitorReceiverIntent() {
        assertIntent(IntentFactory.createDevEventLoggerMonitorReceiverIntent(context, false))
                .containsExtra(DevEventLoggerMonitorReceiver.EXTRA_MONITOR_MUTE, false);

        assertIntent(IntentFactory.createDevEventLoggerMonitorReceiverIntent(context, true))
                .containsExtra(DevEventLoggerMonitorReceiver.EXTRA_MONITOR_MUTE, true);
    }

    @Test
    public void createDevEventLoggerMonitorIntent() {
        assertIntent(IntentFactory.createDevEventLoggerMonitorIntent(context))
                .opensActivity(DevEventLoggerMonitorActivity.class);
    }

    @Test
    public void createTextShareIntentChooser() {
        assertIntent(IntentFactory.createTextShareIntentChooser(context, "text"))
                .containsAction(Intent.ACTION_CHOOSER)
                .wrappedIntent()
                .containsAction(Intent.ACTION_SEND)
                .containsExtra(Intent.EXTRA_TEXT, "text");
    }

    @Test
    public void createNotificationPrefences() {
        assertIntent(IntentFactory.createNotificationPreferencesFromDeeplinkIntent(context))
                .opensActivity(NotificationPreferencesActivity.class)
                .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    @Test
    public void opensOnboarding() {
        Uri uri = Uri.parse("soundcloud://tracks:123");

        assertIntent(IntentFactory.createOnboardingIntent(context, Screen.DEEPLINK, uri))
                .containsExtra(OnboardActivity.EXTRA_DEEP_LINK_URI, uri)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(OnboardActivity.class);
    }

    @Test
    public void opensStream() {
        assertIntent(IntentFactory.createStreamIntent(Screen.DEEPLINK))
                .containsAction(Actions.STREAM)
                .containsScreen(Screen.DEEPLINK);
    }


    @Test
    public void opensDiscovery() {
        assertThat(IntentFactory.createDiscoveryIntent(Screen.DEEPLINK))
                .containsAction(Actions.DISCOVERY)
                .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensLauncher() {
        assertIntent(IntentFactory.createLauncherIntent(activity()))
                .opensActivity(LauncherActivity.class);
    }

    @Test
    public void opensStreamWithExpandedPlayer() {
        assertIntent(IntentFactory.createStreamWithExpandedPlayerIntent(Screen.DEEPLINK))
                .containsAction(Actions.STREAM)
                .containsExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                .containsScreen(Screen.DEEPLINK);
    }


    @Test
    public void openLinkWorksForEmails() {
        String email = "email@address.com";

        assertIntent(IntentFactory.createEmailIntent(email))
                .containsAction(Intent.ACTION_SENDTO)
                .containsUri(Uri.parse("mailto:"))
                .containsExtra(Intent.EXTRA_EMAIL, new String[]{email});
    }

    @Test
    public void openLinkFallsBackToAndroidForUnrecognisedLinks() {
        String url = "http://facebook.com/whatever";

        assertIntent(IntentFactory.createViewIntent(Uri.parse(url)))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(Uri.parse(url));
    }

    @Test
    public void openExternalAppIfInstalled() throws Exception {
        String packageName = "com.soundcloud.android";

        Intent intent = mock(Intent.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getApplicationInfo(packageName, 0)).thenReturn(mock(ApplicationInfo.class));
        when(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(intent);
        when(context.getPackageManager()).thenReturn(packageManager);

        assertIntent(IntentFactory.createExternalAppIntent(context, packageName))
                .isEqualTo(intent);
    }

    @Test
    public void openExternalAppIfInstalledButNoLauncherIntentFound() throws Exception {
        String packageName = "com.soundcloud.android";

        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getApplicationInfo(packageName, 0)).thenReturn(mock(ApplicationInfo.class));
        when(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(null);
        when(context.getPackageManager()).thenReturn(packageManager);

        assertIntent(IntentFactory.createExternalAppIntent(context, packageName))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(Uri.parse(CreatorUtilsKt.PLAY_STORE_URL));
    }

    @Test
    public void openExternalAppIfNotInstalledAndPlayStoreInstalled() throws Exception {
        String packageName = "com.soundcloud.android";

        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getApplicationInfo(packageName, 0)).thenThrow(new PackageManager.NameNotFoundException("error"));
        when(packageManager.getApplicationInfo(CreatorUtilsKt.PACKAGE_NAME_PLAY_STORE, 0)).thenReturn(mock(ApplicationInfo.class));
        when(context.getPackageManager()).thenReturn(packageManager);

        assertIntent(IntentFactory.createExternalAppIntent(context, packageName))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(Uri.parse(CreatorUtilsKt.PLAY_STORE_URL));
    }

    @Test
    public void openExternalAppIfNotInstalledAndPlayStoreNotInstalled() throws Exception {
        String packageName = "com.soundcloud.android";

        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getApplicationInfo(packageName, 0)).thenThrow(new PackageManager.NameNotFoundException("error"));
        when(packageManager.getApplicationInfo(CreatorUtilsKt.PACKAGE_NAME_PLAY_STORE, 0)).thenThrow(new PackageManager.NameNotFoundException("error"));
        when(context.getPackageManager()).thenReturn(packageManager);

        assertIntent(IntentFactory.createExternalAppIntent(context, packageName))
                .containsAction(Intent.ACTION_VIEW)
                .containsUri(Uri.parse(CreatorUtilsKt.PLAY_STORE_WEB_URL));
    }

    private IntentAssert assertIntent(Intent intent) {
        return new IntentAssert(intent);
    }
}

package com.soundcloud.android.navigation;

import static com.soundcloud.android.model.Urn.forAd;
import static com.soundcloud.android.navigation.IntentFactory.createActivitiesIntent;
import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.ads.VisualPrestitialAd;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.RecordPermissionsActivity;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;
import com.soundcloud.android.olddiscovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresPresenter;
import com.soundcloud.android.olddiscovery.charts.ChartActivity;
import com.soundcloud.android.olddiscovery.charts.ChartTracksFragment;
import com.soundcloud.android.olddiscovery.recommendations.ViewAllRecommendedTracksActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.ConversionActivity;
import com.soundcloud.android.payments.ProductChoiceActivity;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.payments.WebCheckoutActivity;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.settings.ChangeStorageLocationActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Shadows;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

public class NavigationExecutorTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);

    private NavigationExecutor navigationExecutor;

    private Context appContext;
    private Activity activityContext;

    @Mock private EventTracker eventTracker;
    @Mock private FeatureFlags featureFlags;

    @Before
    public void setUp() throws Exception {
        navigationExecutor = new NavigationExecutor(eventTracker, featureFlags);
        appContext = context();
        activityContext = activity();
    }

    @Test
    public void openHome() {
        navigationExecutor.openHome(activityContext);
        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(MainActivity.class);
    }

    @Test
    public void openHomeAsRootScreen() {
        navigationExecutor.openHomeAsRootScreen(activityContext);
        assertThat(activityContext).isFinishing();
        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                   .opensActivity(MainActivity.class);
    }

    @Test
    public void launchHome() {
        final Intent intent = new Intent();
        Referrer.FACEBOOK.addToIntent(intent);
        Screen.AUTH_LOG_IN.addToIntent(intent);

        navigationExecutor.launchHome(activityContext, intent.getExtras());

        assertThat(activityContext).nextStartedIntent()
                                   .containsScreen(Screen.AUTH_LOG_IN)
                                   .containsReferrer(Referrer.FACEBOOK)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(MainActivity.class);
    }

    @Test
    public void launchHomeWithoutExtra() {
        navigationExecutor.launchHome(activityContext, null);

        assertThat(activityContext).nextStartedIntent()
                                   .containsScreen(Screen.UNKNOWN)
                                   .containsReferrer(Referrer.HOME_BUTTON)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(MainActivity.class);
    }

    @Test
    public void openCollectionAsTopScreen() {
        navigationExecutor.openCollectionAsTopScreen(appContext);
        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .containsFlag(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                   .containsAction(Actions.COLLECTION);
    }

    @Test
    public void openCollectionAsRootScreen() {
        navigationExecutor.openCollectionAsRootScreen(activityContext);
        assertThat(activityContext).isFinishing();
        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                   .containsAction(Actions.COLLECTION);
    }

    @Test
    public void openCollection() {
        navigationExecutor.openCollection(activityContext);
        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.COLLECTION);
    }

    @Test
    public void openAdClickthrough() {
        final Uri uri = Uri.parse("http://clickthroughurl.com");
        navigationExecutor.openAdClickthrough(activityContext, uri);
        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Intent.ACTION_VIEW)
                                   .containsUri(uri)
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void openVideoFullScreen() {
        final Urn urn = forAd("network", "123");
        navigationExecutor.openFullscreenVideoAd(activityContext, urn);
        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(FullScreenVideoActivity.EXTRA_AD_URN, urn)
                                   .opensActivity(FullScreenVideoActivity.class);
    }

    @Test
    public void openVisualPrestitial() {
        final VisualPrestitialAd ad = VisualPrestitialAd.create(AdFixtures.getApiVisualPrestitial());
        navigationExecutor.openPrestititalAd(activityContext);
        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(PrestitialActivity.class);
    }

    @Test
    public void openUpgrade() {
        navigationExecutor.openUpgrade(activityContext, UpsellContext.OFFLINE);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(ConversionActivity.class);
    }

    @Test
    public void openUpgradeFromDeeplink() {
        navigationExecutor.openUpgradeOnMain(activityContext, UpsellContext.DEFAULT);
        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(ConversionActivity.class);
    }

    @Test
    public void openProductChoiceOnMain() {
        navigationExecutor.openProductChoiceOnMain(activityContext, Plan.MID_TIER);
        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(ProductChoiceActivity.class)
                                   .containsExtra(ProductChoiceActivity.DEFAULT_PLAN, Plan.MID_TIER);
    }

    @Test
    public void openDirectCheckout() {
        navigationExecutor.openDirectCheckout(activityContext, Plan.HIGH_TIER);
        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(WebCheckoutActivity.class)
                                   .containsExtra(IntentFactory.EXTRA_CHECKOUT_PLAN, Plan.HIGH_TIER);
    }

    @Test
    public void openNotificationPreferencesFromDeeplink() {
        navigationExecutor.openNotificationPreferencesFromDeeplink(activityContext);
        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(NotificationPreferencesActivity.class)
                                   .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    @Test
    public void opensPlaylist() {
        PlaylistItem playlist = PlayableFixtures.expectedPromotedPlaylist();
        Urn playlistUrn = playlist.getUrn();

        PromotedSourceInfo promotedInfo = PromotedSourceInfo.fromItem(playlist);
        SearchQuerySourceInfo queryInfo = new SearchQuerySourceInfo(playlistUrn, "query");
        final UIEvent event = UIEvent.fromNavigation(Urn.forTrack(123L), EventContextMetadata.builder().build());

        navigationExecutor.openPlaylist(activityContext, playlistUrn, Screen.SEARCH_PLAYLISTS, queryInfo, promotedInfo, event);

        verify(eventTracker).trackNavigation(event);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlistUrn)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO, queryInfo)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_PROMOTED_SOURCE_INFO, promotedInfo)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }


    @Test
    public void opensPlaylistWithoutSearchQuerySourceInfo() {
        PlaylistItem playlist = PlayableFixtures.expectedPromotedPlaylist();
        Urn playlistUrn = playlist.getUrn();

        final UIEvent event = UIEvent.fromNavigation(Urn.forTrack(123L), EventContextMetadata.builder().build());

        navigationExecutor.openPlaylist(activityContext, playlistUrn, Screen.SEARCH_PLAYLISTS, event);

        verify(eventTracker).trackNavigation(event);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlistUrn)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensLegacyPlaylist() {
        PlaylistItem playlist = PlayableFixtures.expectedPromotedPlaylist();
        Urn playlistUrn = playlist.getUrn();

        PromotedSourceInfo promotedInfo = PromotedSourceInfo.fromItem(playlist);
        SearchQuerySourceInfo queryInfo = new SearchQuerySourceInfo(playlistUrn, "query");

        navigationExecutor.legacyOpenPlaylist(activityContext, playlistUrn, Screen.SEARCH_PLAYLISTS, queryInfo, promotedInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlistUrn)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO, queryInfo)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_PROMOTED_SOURCE_INFO, promotedInfo)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensLegacyPlaylistWithoutSearchQuerySourceInfo() {
        Urn playlist = Urn.forPlaylist(123L);

        navigationExecutor.legacyOpenPlaylist(activityContext, playlist, Screen.SEARCH_PLAYLISTS);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlist)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void openPlaylistWithAutoPlay() {
        Urn playlist = Urn.forPlaylist(123L);

        navigationExecutor.openPlaylistWithAutoPlay(activityContext, playlist, Screen.SEARCH_PLAYLISTS);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlist)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_AUTO_PLAY, true)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void legacyOpensProfileActivity() {
        navigationExecutor.legacyOpenProfile(activityContext, USER_URN);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void opensProfileActivity() {
        UIEvent uiEvent = UIEvent.fromPlayerOpen(false);

        navigationExecutor.openProfile(activityContext, USER_URN, uiEvent);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .opensActivity(ProfileActivity.class);

        verify(eventTracker).trackNavigation(uiEvent);
    }

    @Test
    public void opensProfileActivityWithScreen() {
        navigationExecutor.legacyOpenProfile(activityContext, USER_URN, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .containsScreen(Screen.DEEPLINK)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void opensProfileActivityWithSearchSourceInfo() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");

        navigationExecutor.legacyOpenProfile(activityContext, USER_URN, Screen.DEEPLINK, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.DEEPLINK)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                                   .containsScreen(Screen.NOTIFICATION)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                                   .containsScreen(Screen.WIDGET)
                                   .containsReferrer(Referrer.PLAYBACK_WIDGET)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void openRecordPermissions() {
        Recording recording = new Recording();
        navigationExecutor.openRecord(activityContext, recording);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(Recording.EXTRA, recording)
                                   .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(RecordPermissionsActivity.class);
    }

    @Test
    public void openRecord() {
        Shadows.shadowOf(activityContext.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);
        Recording recording = new Recording();
        navigationExecutor.openRecord(activityContext, recording);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(Recording.EXTRA, recording)
                                   .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(RecordActivity.class);
    }

    @Test
    public void opensOnboarding() {
        Uri uri = Uri.parse("soundcloud://tracks:123");
        navigationExecutor.openOnboarding(activityContext, uri, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(OnboardActivity.EXTRA_DEEP_LINK_URI, uri)
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                   .containsScreen(Screen.DEEPLINK)
                                   .opensActivity(OnboardActivity.class);
    }

    @Test
    public void opensResolveActivity() {
        Uri uri = Uri.parse("soundcloud://tracks:123");
        navigationExecutor.openResolveForUri(activityContext, uri);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Intent.ACTION_VIEW)
                                   .containsUri(uri)
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                   .opensActivity(ResolveActivity.class);
    }

    @Test
    public void opensStream() {
        navigationExecutor.openStream(activityContext, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.STREAM)
                                   .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensDiscovery() {
        navigationExecutor.openDiscovery(activityContext, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.DISCOVERY)
                                   .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensLauncher() {
        navigationExecutor.openLauncher(activityContext);

        assertThat(activityContext).nextStartedIntent().opensActivity(LauncherActivity.class);
    }

    @Test
    public void opensStreamWithExpandedPlayer() {
        navigationExecutor.openStreamWithExpandedPlayer(activityContext, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.STREAM)
                                   .containsExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                                   .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensWebView() {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        navigationExecutor.openWebView(activityContext, uri);

        assertThat(activityContext).nextStartedIntent()
                                   .containsUri(uri)
                                   .opensActivity(WebViewActivity.class);
    }

    @Test
    public void opensViewAllRecommendations() {
        navigationExecutor.openViewAllRecommendations(activityContext);

        assertThat(activityContext).nextStartedIntent().opensActivity(ViewAllRecommendedTracksActivity.class);
    }

    @Test
    public void opensPlaylistDiscoveryTag() {
        final String playListTag = "playListTag";

        navigationExecutor.openPlaylistDiscoveryTag(activityContext, playListTag);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(PlaylistDiscoveryActivity.EXTRA_PLAYLIST_TAG, playListTag)
                                   .opensActivity(PlaylistDiscoveryActivity.class);
    }

    @Test
    public void opensTrackLikes() {
        navigationExecutor.openTrackLikes(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(TrackLikesActivity.class);
    }

    @Test
    public void opensNewForYou() {
        navigationExecutor.openNewForYou(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(SystemPlaylistActivity.class)
                                   .containsExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, true);
    }

    @Test
    public void opensSystemPlaylist() {
        final Urn urn = Urn.forSystemPlaylist("123");
        navigationExecutor.openSystemPlaylist(activityContext, urn, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(SystemPlaylistActivity.class)
                                   .containsExtra(SystemPlaylistActivity.EXTRA_FOR_NEW_FOR_YOU, false)
                                   .containsExtra(SystemPlaylistActivity.EXTRA_PLAYLIST_URN, urn)
                                   .containsScreen(Screen.DEEPLINK);
    }


    @Test
    public void opensSearchPremiumContentResults() {
        final List<Urn> urns = Collections.emptyList();
        final String searchQuery = "query";
        final SearchType searchType = SearchType.ALL;
        final Optional<Link> nextHref = Optional.absent();
        final Urn queryUrn = Urn.forTrack(1);

        navigationExecutor.openSearchPremiumContentResults(activityContext,
                                                           searchQuery,
                                                           searchType,
                                                           urns,
                                                           nextHref,
                                                           queryUrn);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(SearchPremiumResultsActivity.class)
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY, searchQuery)
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE, searchType)
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS, urns)
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY_URN, queryUrn)
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF,
                                                  nextHref.orNull());
    }

    @Test
    public void performSearch() {
        navigationExecutor.performSearch(activityContext, "query");

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PERFORM_SEARCH)
                                   .containsExtra(SearchManager.QUERY, "query");
    }

    @Test
    public void opensTrackComments() {
        Urn trackUrn = Urn.forTrack(123);

        navigationExecutor.openTrackComments(activityContext, trackUrn);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(TrackCommentsActivity.class)
                                   .containsExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN, trackUrn);
    }

    @Test
    public void opensOfflineSettingsOnboarding() {
        navigationExecutor.openOfflineSettingsOnboarding(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(OfflineSettingsOnboardingActivity.class);
    }

    @Test
    public void resetsAccountForUpgrade() {
        navigationExecutor.resetForAccountUpgrade(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(GoOnboardingActivity.class)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Assertions.assertThat(activityContext.isFinishing()).isTrue();
    }

    @Test
    public void resetsAccountForDowngrade() {
        navigationExecutor.resetForAccountDowngrade(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(GoOffboardingActivity.class)
                                   .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Assertions.assertThat(activityContext.isFinishing()).isTrue();
    }

    @Test
    public void opensProfileReposts() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        navigationExecutor.openProfileReposts(activityContext, USER_URN, Screen.USERS_REPOSTS, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(UserRepostsActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.USERS_REPOSTS)
                                   .opensActivity(UserRepostsActivity.class);
    }

    @Test
    public void opensProfileTracks() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        navigationExecutor.openProfileTracks(activityContext, USER_URN, Screen.USER_TRACKS, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(UserTracksActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.USER_TRACKS)
                                   .opensActivity(UserTracksActivity.class);
    }

    @Test
    public void openProfileAlbums() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        navigationExecutor.openProfileAlbums(activityContext, USER_URN, Screen.USER_ALBUMS, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(UserAlbumsActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.USER_ALBUMS)
                                   .opensActivity(UserAlbumsActivity.class);
    }

    @Test
    public void opensProfileLikes() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        navigationExecutor.openProfileLikes(activityContext, USER_URN, Screen.USER_LIKES, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(UserLikesActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.USER_LIKES)
                                   .opensActivity(UserLikesActivity.class);
    }

    @Test
    public void opensProfilePlaylists() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L), "query");
        navigationExecutor.openProfilePlaylists(activityContext, USER_URN, Screen.USER_PLAYLISTS, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(UserPlaylistsActivity.EXTRA_USER_URN, USER_URN)
                                   .intentExtraIsNotNull(UserPlaylistsActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                                   .containsScreen(Screen.USER_PLAYLISTS)
                                   .opensActivity(UserPlaylistsActivity.class);
    }

    @Test
    public void opensPlayHistory() {
        navigationExecutor.openPlayHistory(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(PlayHistoryActivity.class);
    }

    @Test
    public void opensRecentlyPlayed() {
        navigationExecutor.openRecentlyPlayed(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(RecentlyPlayedActivity.class);
    }

    @Test
    public void opensChartTracks() {
        final Urn genreUrn = new Urn("soundcloud:genre:123");
        final ChartType chartType = ChartType.TOP;
        final String header = "header";
        final ChartCategory chartCategory = ChartCategory.AUDIO;
        navigationExecutor.openChart(activityContext, genreUrn, chartType, chartCategory, header);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ChartTracksFragment.EXTRA_GENRE_URN, genreUrn)
                                   .containsExtra(ChartTracksFragment.EXTRA_TYPE, chartType)
                                   .containsExtra(ChartTracksFragment.EXTRA_HEADER, header)
                                   .opensActivity(ChartActivity.class);
    }

    @Test
    public void opensAllGenres() {
        navigationExecutor.openAllGenres(activityContext);

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(AllGenresActivity.class);
    }

    @Test
    public void opensAllGenresFromDeeplink() throws Exception {
        navigationExecutor.openAllGenres(activityContext, ChartCategory.MUSIC);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(AllGenresPresenter.EXTRA_CATEGORY, ChartCategory.MUSIC)
                                   .opensActivity(AllGenresActivity.class);
    }

    @Test
    public void legacyOpenStationInfo() {
        final Urn someStation = Urn.forArtistStation(123L);
        navigationExecutor.legacyOpenStationInfo(activityContext, someStation, DiscoverySource.STATIONS);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(StationInfoActivity.EXTRA_SOURCE, DiscoverySource.STATIONS.value())
                                   .containsExtra(StationInfoActivity.EXTRA_URN, someStation)
                                   .opensActivity(StationInfoActivity.class);
    }

    @Test
    public void openStationInfo() {
        final Urn someStation = Urn.forArtistStation(123L);
        final Urn seedTrack = Urn.forTrack(123L);
        final UIEvent navigationEvent = UIEvent.fromNavigation(seedTrack, EventContextMetadata.builder().build());

        navigationExecutor.openStationInfo(activityContext, someStation, seedTrack, DiscoverySource.STATIONS, navigationEvent);

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(StationInfoActivity.EXTRA_SOURCE, DiscoverySource.STATIONS.value())
                                   .containsExtra(StationInfoActivity.EXTRA_URN, someStation)
                                   .containsExtra(StationInfoActivity.EXTRA_SEED_URN, seedTrack)
                                   .opensActivity(StationInfoActivity.class);

        verify(eventTracker).trackNavigation(navigationEvent);
    }

    @Test
    public void openActivities() {
        activityContext.startActivity(createActivitiesIntent((Context) activityContext));
        assertThat(activityContext).nextStartedIntent().opensActivity(ActivitiesActivity.class);
    }

    @Test
    public void createsPendingOfflineSettings() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.createPendingOfflineSettings(activityContext);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(OfflineSettingsActivity.class);
    }

    @Test
    public void createsPendingChangeStorageLocation() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.createPendingChangeStorageLocation(activityContext);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .opensActivity(ChangeStorageLocationActivity.class);
    }

    @Test
    public void createsPendingHomeIntent() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.createPendingHomeIntent(activityContext);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .opensActivity(MainActivity.class);
    }

    @Test
    public void createsPendingCollectionIntent() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.createPendingCollectionIntent(activityContext);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                   .containsAction(Actions.COLLECTION);
    }

    @Test
    public void openLinkWorksForEmails() {
        navigationExecutor.openEmail(activityContext, "email@address.com");

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Intent.ACTION_SENDTO)
                                   .containsUri(Uri.parse("mailto:"))
                                   .containsExtra(Intent.EXTRA_EMAIL, new String[]{"email@address.com"});
    }

    @Test
    public void openLinkFallsBackToAndroidForUnrecognisedLinks() {
        navigationExecutor.openExternal(activityContext, Uri.parse("http://facebook.com/whatever"));

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Intent.ACTION_VIEW)
                                   .containsUri(Uri.parse("http://facebook.com/whatever"));
    }
}
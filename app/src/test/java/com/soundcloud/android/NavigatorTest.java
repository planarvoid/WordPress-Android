package com.soundcloud.android;

import static com.soundcloud.android.testsupport.Assertions.assertThat;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.discovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.discovery.RecommendedTracksActivity;
import com.soundcloud.android.explore.ExploreActivity;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.UpgradeActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

public class NavigatorTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    public static final int SEARCH_TYPE = 0;

    @Mock private FeatureFlags flags;

    private Navigator navigator;

    private Context appContext;
    private Activity activityContext;

    @Before
    public void setUp() throws Exception {
        navigator = new Navigator();
        appContext = context();
        activityContext = new Activity();
    }

    @Test
    public void openHome() {
        navigator.openHome(activityContext);
        assertThat(activityContext).nextStartedIntent()
                .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .opensActivity(MainActivity.class);
    }

    @Test
    public void launchHome() {
        final Intent intent = new Intent();
        Referrer.FACEBOOK.addToIntent(intent);
        Screen.AUTH_LOG_IN.addToIntent(intent);

        navigator.launchHome(activityContext, intent.getExtras());

        assertThat(activityContext).nextStartedIntent()
                .containsScreen(Screen.AUTH_LOG_IN)
                .containsReferrer(Referrer.FACEBOOK)
                .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .opensActivity(MainActivity.class);
    }

    @Test
    public void launchHomeWithoutExtra() {
        navigator.launchHome(activityContext, null);

        assertThat(activityContext).nextStartedIntent()
                .containsScreen(Screen.UNKNOWN)
                .containsReferrer(Referrer.HOME_BUTTON)
                .containsFlag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .opensActivity(MainActivity.class);
    }

    @Test
    public void openUpgrade() {
        navigator.openUpgrade(activityContext);
        assertThat(activityContext).nextStartedIntent().opensActivity(UpgradeActivity.class);
    }

    @Test
    public void opensPlaylist() {
        PromotedPlaylistItem playlist = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        Urn playlistUrn = playlist.getEntityUrn();

        PromotedSourceInfo promotedInfo = PromotedSourceInfo.fromItem(playlist);
        SearchQuerySourceInfo queryInfo = new SearchQuerySourceInfo(playlistUrn);

        navigator.openPlaylist(activityContext, playlistUrn, Screen.SEARCH_PLAYLISTS, queryInfo, promotedInfo);

        assertThat(activityContext).nextStartedIntent()
                .containsAction(Actions.PLAYLIST)
                .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlistUrn)
                .containsExtra(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO, queryInfo)
                .containsExtra(PlaylistDetailActivity.EXTRA_PROMOTED_SOURCE_INFO, promotedInfo)
                .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensPlaylistWithoutSearchQuerySourceInfo() {
        Urn playlist = Urn.forPlaylist(123L);

        navigator.openPlaylist(activityContext, playlist, Screen.SEARCH_PLAYLISTS);

        assertThat(activityContext).nextStartedIntent()
                .containsAction(Actions.PLAYLIST)
                .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlist)
                .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void opensProfileActivity() {
        navigator.openProfile(activityContext, USER_URN);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void opensProfileActivityWithScreen() {
        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void opensProfileActivityWithSearchSourceInfo() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));

        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                .intentExtraIsNotNull(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                .containsScreen(Screen.NOTIFICATION)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsScreen(Screen.WIDGET)
                .containsReferrer(Referrer.PLAYBACK_WIDGET)
                .opensActivity(ProfileActivity.class);
    }

    @Test
    public void openRecord() {
        Recording recording = new Recording();
        navigator.openRecord(activityContext, recording);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(Recording.EXTRA, recording)
                .containsFlag(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .opensActivity(RecordActivity.class);
    }

    @Test
    public void opensOnboarding() {
        navigator.openOnboarding(activityContext, USER_URN, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(OnboardActivity.EXTRA_DEEPLINK_URN, USER_URN)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(OnboardActivity.class);
    }

    @Test
    public void opensStream() {
        navigator.openStream(activityContext, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                .containsAction(Actions.STREAM)
                .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensExplore() {
        navigator.openExplore(activityContext, Screen.YOU);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(ExploreActivity.class)
                .containsScreen(Screen.YOU);
    }

    @Test
    public void opensLauncher() {
        navigator.openLauncher(activityContext);

        assertThat(activityContext).nextStartedIntent().opensActivity(LauncherActivity.class);
    }

    @Test
    public void opensStreamWithExpandedPlayer() {
        navigator.openStreamWithExpandedPlayer(activityContext, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                .containsAction(Actions.STREAM)
                .containsExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                .containsScreen(Screen.DEEPLINK);
    }

    @Test
    public void opensWebView() {
        Uri uri = Uri.parse("http://soundcloud.com/skrillex");
        navigator.openWebView(activityContext, uri);

        assertThat(activityContext).nextStartedIntent()
                .containsUri(uri)
                .opensActivity(WebViewActivity.class);
    }

    @Test
    public void opensRecommendation() {
        navigator.openRecommendation(activityContext, 88L);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(RecommendedTracksActivity.EXTRA_LOCAL_SEED_ID, 88L)
                .opensActivity(RecommendedTracksActivity.class);
    }

    @Test
    public void opensPlaylistDiscoveryTag() {
        final String playListTag = "playListTag";

        navigator.openPlaylistDiscoveryTag(activityContext, playListTag);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(PlaylistDiscoveryActivity.EXTRA_PLAYLIST_TAG, playListTag)
                .opensActivity(PlaylistDiscoveryActivity.class);
    }

    @Test
    public void opensTrackLikes() {
        navigator.openTrackLikes(activityContext);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(TrackLikesActivity.class);
    }

    @Test
    public void opensSearchPremiumContentResults() {
        final List<PropertySet> propertySets = Collections.emptyList();
        final String searchQuery = "query";
        final int searchType = 1;
        final Optional<Link> nextHref = Optional.absent();

        navigator.openSearchPremiumContentResults(activityContext, searchQuery, searchType, propertySets, nextHref);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(SearchPremiumResultsActivity.class)
                .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY, searchQuery)
                .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_TYPE, searchType)
                .containsExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS, propertySets)
                .containsExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_NEXT_HREF, nextHref.orNull());
    }

    @Test
    public void opensTrackComments() {
        Urn trackUrn = Urn.forTrack(123);

        navigator.openTrackComments(activityContext, trackUrn);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(TrackCommentsActivity.class)
                .containsExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN, trackUrn);
    }

    @Test
    public void shouldOpenPendingActivityFromIntentExtras() {
        Bundle extras = new Bundle();
        extras.putString(Navigator.EXTRA_PENDING_ACTIVITY, MainActivity.class.getCanonicalName());

        navigator.openPendingActivity(activityContext, extras);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(MainActivity.class);
    }

    @Test
    public void shouldOpenPendingActivityFromIntentExtrasWithPending() {
        Bundle extras = new Bundle();
        extras.putString(Navigator.EXTRA_PENDING_ACTIVITY, MainActivity.class.getCanonicalName());

        navigator.openPendingActivity(activityContext, extras);

        assertThat(activityContext).nextStartedIntent()
                .opensActivity(MainActivity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfOpenPendingActivityCannotBeResolved() {
        Bundle extras = new Bundle();
        extras.putString(Navigator.EXTRA_PENDING_ACTIVITY, "non existo");

        navigator.openPendingActivity(activityContext, extras);
    }
}

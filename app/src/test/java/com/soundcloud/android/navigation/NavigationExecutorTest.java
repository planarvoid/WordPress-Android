package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createActivitiesIntent;
import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static java.util.Collections.emptyList;

import com.soundcloud.android.Actions;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistActivity;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.olddiscovery.recommendations.ViewAllRecommendedTracksActivity;
import com.soundcloud.android.payments.ConversionActivity;
import com.soundcloud.android.payments.ProductChoiceActivity;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.payments.WebCheckoutActivity;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchType;
import com.soundcloud.android.settings.ChangeStorageLocationActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

public class NavigationExecutorTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);

    private NavigationExecutor navigationExecutor;

    private Context appContext;
    private Activity activityContext;

    @Before
    public void setUp() throws Exception {
        navigationExecutor = new NavigationExecutor();
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
        assertThat(navigationExecutor.openDirectCheckout(activityContext, Plan.HIGH_TIER))
                .opensActivity(WebCheckoutActivity.class)
                .containsExtra(IntentFactory.EXTRA_CHECKOUT_PLAN, Plan.HIGH_TIER);
    }

    @Test
    public void openPlaylistWithAutoPlay() {
        Urn playlist = Urn.forPlaylist(123L);

        navigationExecutor.openPlaylistWithAutoPlay(activityContext, playlist, Screen.SEARCH_PLAYLISTS);

        assertThat(activityContext).nextStartedIntent()
                                   .containsAction(Actions.PLAYLIST)
                                   .containsExtra(PlaylistDetailActivity.EXTRA_URN, playlist.getContent())
                                   .containsExtra(PlaylistDetailActivity.EXTRA_AUTO_PLAY, true)
                                   .containsScreen(Screen.SEARCH_PLAYLISTS);
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN.getContent())
                                   .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                                   .containsScreen(Screen.NOTIFICATION)
                                   .opensActivity(ProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = PendingIntentFactory.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                                   .containsExtra(ProfileActivity.EXTRA_USER_URN, USER_URN.getContent())
                                   .containsScreen(Screen.WIDGET)
                                   .containsReferrer(Referrer.PLAYBACK_WIDGET)
                                   .opensActivity(ProfileActivity.class);
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
    public void opensSearchPremiumContentResults() {
        final List<Urn> urns = emptyList();
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
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_PREMIUM_CONTENT_RESULTS, emptyList())
                                   .containsExtra(SearchPremiumResultsActivity.EXTRA_SEARCH_QUERY_URN, queryUrn.getContent())
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
                                   .containsExtra(TrackCommentsActivity.EXTRA_COMMENTED_TRACK_URN, trackUrn.getContent());
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
}

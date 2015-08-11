package com.soundcloud.android;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.discovery.RecommendedTracksActivity;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.payments.UpgradeActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.profile.LegacyProfileActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class NavigatorTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);

    @Mock private FeatureFlags flags;

    private Navigator navigator;

    private Context appContext;
    private Activity activityContext;

    @Before
    public void setUp() throws Exception {
        navigator = new Navigator(flags);
        appContext = context();
        activityContext = new Activity();
        when(flags.isEnabled(Flag.NEW_PROFILE)).thenReturn(false);
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
    public void opensMyProfileActivity() {
        navigator.openMyProfile(activityContext, USER_URN);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .opensActivity(MeActivity.class);
    }

    @Test
    public void opensProfileActivity() {
        navigator.openProfile(activityContext, USER_URN);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .opensActivity(LegacyProfileActivity.class);
    }

    @Test
    public void opensProfileActivityWithScreen() {
        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(LegacyProfileActivity.class);
    }

    @Test
    public void opensProfileActivityWithSearchSourceInfo() {
        SearchQuerySourceInfo searchSourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));

        navigator.openProfile(activityContext, USER_URN, Screen.DEEPLINK, searchSourceInfo);

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .intentExtraIsNotNull(LegacyProfileActivity.EXTRA_QUERY_SOURCE_INFO)
                .containsScreen(Screen.DEEPLINK)
                .opensActivity(LegacyProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromNotification() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromNotification(appContext, USER_URN);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsFlag(Intent.FLAG_ACTIVITY_NEW_TASK)
                .containsScreen(Screen.NOTIFICATION)
                .opensActivity(LegacyProfileActivity.class);
    }

    @Test
    public void createsPendingIntentToProfileFromWidget() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = navigator.openProfileFromWidget(appContext, USER_URN, 0);

        pendingIntent.send();

        assertThat(activityContext).nextStartedIntent()
                .containsExtra(LegacyProfileActivity.EXTRA_USER_URN, USER_URN)
                .containsScreen(Screen.WIDGET)
                .opensActivity(LegacyProfileActivity.class);

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
                .containsExtra(OnboardActivity.EXTRA_URN, USER_URN)
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
}

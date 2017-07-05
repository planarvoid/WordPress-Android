package com.soundcloud.android.navigation;

import static com.soundcloud.android.model.Urn.forAd;
import static com.soundcloud.android.model.Urn.forUser;
import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.ads.FullScreenVideoActivity;
import com.soundcloud.android.ads.PrestitialActivity;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.main.DevEventLoggerMonitorReceiver;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.charts.AllGenresActivity;
import com.soundcloud.android.olddiscovery.charts.AllGenresPresenter;
import com.soundcloud.android.olddiscovery.charts.ChartActivity;
import com.soundcloud.android.olddiscovery.charts.ChartTracksFragment;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentFactoryTest extends AndroidUnitTest {

    @Mock Context context;

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
    public void createDevEventLoggerMonitorReceiverIntent() {
        assertIntent(IntentFactory.createDevEventLoggerMonitorReceiverIntent(context, false))
                .containsExtra(DevEventLoggerMonitorReceiver.EXTRA_MONITOR_MUTE, false);

        assertIntent(IntentFactory.createDevEventLoggerMonitorReceiverIntent(context, true))
                .containsExtra(DevEventLoggerMonitorReceiver.EXTRA_MONITOR_MUTE, true);
    }

    private IntentAssert assertIntent(Intent intent) {
        return new IntentAssert(intent);
    }
}

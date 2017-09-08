package com.soundcloud.android.tests.collection;

import static com.soundcloud.android.framework.TestUser.collectionUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.core.Is;
import org.junit.Test;

public class RecentlyPlayedTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_RECENTLY_PLAYED_PLAYLIST = "specs/audio-events-recently-played-playlist.spec";
    private static final String TEST_SCENARIO_RECENTLY_PLAYED_STATION = "specs/audio-events-recently-played-station.spec";
    private static final String TEST_SCENARIO_RECENTLY_PLAYED_PROFILE = "specs/audio-events-recently-played-profile.spec";

    private CollectionScreen collectionScreen;

    public RecentlyPlayedTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return collectionUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        collectionScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(DISCOVER_BACKEND);
        super.tearDown();
    }

    @Test
    public void testPlayingAPlaylistFromTheRecentlyPlayedBucketFulfilsSpec() throws Exception {
        mrLocalLocal.startEventTracking();

        PlaylistDetailsScreen playlistDetailsScreen = collectionScreen.clickPlaylistOnRecentlyPlayedBucket();
        assertThat(playlistDetailsScreen.isVisible(), is(true));

        final VisualPlayerElement playerElement = playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        mrLocalLocal.verify(TEST_SCENARIO_RECENTLY_PLAYED_PLAYLIST);
    }

    @Test
    public void testPlayingAStationFromTheRecentlyPlayedBucketFulfilsSpec() throws Exception {
        mrLocalLocal.startEventTracking();

        StationHomeScreen stationHomeScreen = collectionScreen.clickStationOnRecentlyPlayedBucket();
        assertThat(stationHomeScreen.isVisible(), is(true));

        final VisualPlayerElement playerElement = stationHomeScreen.clickPlay();

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        mrLocalLocal.verify(TEST_SCENARIO_RECENTLY_PLAYED_STATION);
    }

    @Test
    public void testPlayingAProfileFromTheRecentlyPlayedBucketFulfilsSpec() throws Exception {
        mrLocalLocal.startEventTracking();

        ProfileScreen profileScreen = collectionScreen.clickProfileOnRecentlyPlayedBucket();
        assertThat(profileScreen.isVisible(), is(true));

        final VisualPlayerElement playerElement = profileScreen.playTrack(0);

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        mrLocalLocal.verify(TEST_SCENARIO_RECENTLY_PLAYED_PROFILE);
    }
}

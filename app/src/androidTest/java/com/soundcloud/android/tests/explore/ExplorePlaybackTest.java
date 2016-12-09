package com.soundcloud.android.tests.explore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ExplorePlaybackTest extends ActivityTest<MainActivity> {
    private ExploreScreen exploreScreen;
    private VisualPlayerElement playerScreen;

    public ExplorePlaybackTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.goTestUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        exploreScreen = mainNavHelper.goToExplore();
    }

    public void testPlayingTrendingMusicTrack() {
        exploreScreen.touchTrendingMusicTab();
        String trackName = exploreScreen.getTrackTitle(0);
        String trackName2 = exploreScreen.getTrackTitle(1);

        playerScreen = exploreScreen.playPopularTrack(0);
        playerScreen.waitForExpandedPlayerToStartPlaying();

        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));

        playerScreen.swipeNext();
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName2)));
    }

    public void testPlayingExploreGenreTrack() {
        exploreScreen.touchGenresTab();

        ExploreGenreCategoryScreen categoryScreen = exploreScreen.clickGenreItem("Ambient");
        String trackName = categoryScreen.getTrackTitle(0);
        String trackName2 = categoryScreen.getTrackTitle(1);

        playerScreen = categoryScreen.playTrack(0);
        playerScreen.waitForExpandedPlayerToStartPlaying();

        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));

        playerScreen.swipeNext();
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName2)));
    }
}

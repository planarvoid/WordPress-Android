package com.soundcloud.android.tests.explore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
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
    protected void logInHelper() {
        TestUser.goTestUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ConfigurationHelper.disableIntroductoryOverlay(getInstrumentation().getTargetContext(), IntroductoryOverlayKey.PLAY_QUEUE);
        exploreScreen = mainNavHelper.goToExplore();
    }

    public void testPlayingTrendingMusicTrack() {
        exploreScreen.touchTrendingMusicTab();
        String trackName = exploreScreen.getTrackTitle(0);
        playerScreen = exploreScreen.playPopularTrack(0);
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));
    }

    public void testPlayingExploreGenreTrack() {
        exploreScreen.touchGenresTab();
        ExploreGenreCategoryScreen categoryScreen = exploreScreen.clickGenreItem("Ambient");
        String trackName = categoryScreen.getTrackTitle(0);
        playerScreen = categoryScreen.playTrack(0);
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));
    }
}

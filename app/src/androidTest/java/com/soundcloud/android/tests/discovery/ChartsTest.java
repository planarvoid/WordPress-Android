package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.DiscoveryChartsTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.AllGenresScreen;
import com.soundcloud.android.screens.discovery.ChartsScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@DiscoveryChartsTest
public class ChartsTest extends ActivityTest<MainActivity> {
    private DiscoveryScreen discoveryScreen;

    public ChartsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testPlayTrackFromGenreChart() {
        AllGenresScreen genresScreen = discoveryScreen.chartBucket().clickViewAll();
        assertThat(genresScreen.getActionBarTitle(), equalTo("All genres"));
        assertThat(genresScreen.activeTabTitle(), equalTo("Music"));
        genresScreen.swipeLeft();
        assertThat(genresScreen.activeTabTitle(), equalTo("Audio"));

        ChartsScreen chartsScreen = genresScreen.clickGenre("Audiobooks");
        assertThat(chartsScreen.getActionBarTitle(), equalTo("Audiobooks"));
        assertThat(chartsScreen.activeTabTitle(), equalTo("New & hot"));
        chartsScreen.swipeLeft();
        assertThat(chartsScreen.activeTabTitle(), equalTo("Top 50"));

        final String trackTitle = chartsScreen.firstTrackTitle();
        final VisualPlayerElement player = chartsScreen.clickFirstTrack();
        player.waitForExpandedPlayer();
        assertThat(trackTitle, equalTo(player.getTrackTitle()));
    }

}

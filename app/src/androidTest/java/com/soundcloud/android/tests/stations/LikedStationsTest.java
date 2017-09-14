package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.TestUser.stationsUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableStationsOnboarding;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.stations.LikedStationsScreen;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class LikedStationsTest extends ActivityTest<MainActivity> {

    public LikedStationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return stationsUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        disableStationsOnboarding(activityTestRule.getActivity());
    }

    @Test
    public void testLikeAndUnlikeStation() throws Exception {
        StationHomeScreen stationHome = mainNavHelper.goToDiscovery().multipleSelectionCard().clickFirstPlaylist().getTracks().get(0).clickOverflowButton().clickStation();

        String title = stationHome.stationTitle();
        boolean liked = stationHome.isStationLiked();
        if (!liked) {
            stationHome.clickStationLike();
            assertThat(stationHome.isStationLiked(), is(true));
        }

        stationHome.goBack().goBack(DiscoveryScreen::new);

        final LikedStationsScreen likedStationsScreen = mainNavHelper.goToCollections().clickStations();
        stationHome = likedStationsScreen.clickStationWithTitle(title);
        assertThat(stationHome.isStationLiked(), is(true));

        stationHome.clickStationLike();
        assertThat(stationHome.isStationLiked(), is(false));

    }

}

package com.soundcloud.android.tests.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BlockedTrackTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.tests.ActivityTest;

@BlockedTrackTest
public class GeoBlockTest extends ActivityTest<MainActivity> {

    private CollectionsScreen collectionsScreen;

    public GeoBlockTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.freeNonMonetizedUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        collectionsScreen = mainNavHelper.goToCollections();
    }

    public void testSkipsBlockedTracks() throws Exception {
        final PlaylistDetailsScreen playlistScreen = collectionsScreen.clickPlaylistWithTitle("Geoblock Test");
        final String title = playlistScreen.clickFirstTrack()
                .waitForTheExpandedPlayerToPlayNextTrack()
                .getTrackTitle();
        assertThat(title, is("Post - Geoblock"));
    }
}

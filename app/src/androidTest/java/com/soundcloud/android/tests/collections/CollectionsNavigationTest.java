package com.soundcloud.android.tests.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.CollectionsTrackLikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class CollectionsNavigationTest extends ActivityTest<MainActivity> {

    protected CollectionsScreen collectionsScreen;

    public CollectionsNavigationTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.COLLECTIONS);
        super.setUp();

        menuScreen = new MenuScreen(solo);
        collectionsScreen = menuScreen.open().clickCollections();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionsUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testGoesToPlaylistDetailsPage() {
        PlaylistDetailsScreen playlistDetailsScreen = collectionsScreen.clickOnFirstPlaylist();
        assertThat(playlistDetailsScreen.isVisible(), is(true));
    }

    public void testGoesToTrackLikesPage() {
        CollectionsTrackLikesScreen trackLikesScreen = collectionsScreen.clickTrackLikes();
        assertThat(trackLikesScreen.isVisible(), is(true));
    }
}

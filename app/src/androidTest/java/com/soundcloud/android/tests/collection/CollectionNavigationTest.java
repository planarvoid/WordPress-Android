package com.soundcloud.android.tests.collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

public class CollectionNavigationTest extends ActivityTest<MainActivity> {

    protected CollectionScreen collectionScreen;

    public CollectionNavigationTest() {
        super(MainActivity.class);
    }

    private void navigateToCollections() {
        collectionScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void logInHelper() {
        TestUser.collectionUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testGoesToTrackLikesPage() {
        navigateToCollections();
        TrackLikesScreen trackLikesScreen = collectionScreen.clickLikedTracksPreview();
        assertThat(trackLikesScreen.isVisible(), is(true));
    }

    public void testGoesToPlaylistsPage() {
        navigateToCollections();
        PlaylistsScreen playlistsScreen = collectionScreen.clickPlaylistsPreview();
        assertThat(playlistsScreen.isVisible(), is(true));
    }
}

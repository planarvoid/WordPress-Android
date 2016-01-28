package com.soundcloud.android.tests.collection;

import static com.soundcloud.android.framework.TestUser.emptyUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

@Ignore
public class CollectionEmptyTest extends ActivityTest<MainActivity> {

    protected CollectionScreen collectionScreen;

    public CollectionEmptyTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        collectionScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyPlaylistsMessage() {
        assertThat(collectionScreen.isVisible(), is(true));
    }

    public void testShowsEmptyTrackLikes() {
        TrackLikesScreen trackLikesScreen = collectionScreen.clickLikedTracksPreview();
        assertThat(trackLikesScreen.emptyView().isVisible(), is(true));
    }
}

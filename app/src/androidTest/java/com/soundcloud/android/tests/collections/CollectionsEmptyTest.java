package com.soundcloud.android.tests.collections;

import static com.soundcloud.android.framework.TestUser.emptyUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

@Ignore
public class CollectionsEmptyTest extends ActivityTest<MainActivity> {

    protected CollectionsScreen collectionsScreen;

    public CollectionsEmptyTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.COLLECTIONS);
        super.setUp();

        collectionsScreen = mainNavHelper.goToCollections();
    }

    @Override
    protected void logInHelper() {
        emptyUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowsEmptyPlaylistsMessage() {
        assertThat(collectionsScreen.isVisible(), is(true));
    }

    public void testShowsEmptyTrackLikes() {
        TrackLikesScreen trackLikesScreen = collectionsScreen.clickTrackLikes();
        assertThat(trackLikesScreen.emptyView().isVisible(), is(true));
    }
}

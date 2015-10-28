package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.PlaylistOverflowMenu;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDetailsEngagementsTest extends ActivityTest<LauncherActivity> {

    private CollectionsScreen collectionsScreen;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetailsEngagementsTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        collectionsScreen = mainNavHelper.goToCollections();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = collectionsScreen.clickOnFirstPlaylist();
    }

    public void testShufflePlaylistShowsPlayer() throws Exception {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        PlaylistOverflowMenu overflowMenu = playlistDetailsScreen.clickPlaylistOverflowButton();

        assertThat(player, is(not(visible())));
        overflowMenu.shuffle();
        assertThat(player, is(visible()));
    }
}

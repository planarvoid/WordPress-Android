package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.tests.ActivityTest;

public class OtherProfileTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen screen;

    public OtherProfileTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        screen = new MenuScreen(solo)
                .open()
                .clickUserProfile().touchFollowingsTab()
                .getUsers()
                .get(0)
                .click();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testPostsLoadNextPage() {
        int postItemsBefore = screen.getCurrentListItemCount();
        screen.scrollToBottomOfCurrentListAndLoadMoreItems();
        assertThat(postItemsBefore, is(lessThan(screen.getCurrentListItemCount())));
    }

    public void testPostsTrackClickStartsPlayer() {
        assertThat(screen.getTracks().get(0).click(), is(visible()));
    }

    // this dude has no playlists
    public void testPostsPlaylistClickOpensPlaylistPage() {
        final PlaylistItemElement expectedPlaylist = screen
                .getPlaylists()
                .get(0);

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testClickFollowingsLoadsProfile() {
        screen.touchFollowingsTab();

        final UserItemElement expectedUser = screen
                .getUsers()
                .get(0);

        String targetUsername = expectedUser.getUsername();
        assertEquals(expectedUser.click().getUserName(), targetUsername);
    }
}

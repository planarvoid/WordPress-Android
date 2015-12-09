package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

public class MyProfileTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.EVENTLOGGER_AUDIO_V1);
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testPostsLoadNextPage() {
        int postItemsBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(postItemsBefore, is(lessThan(profileScreen.currentItemCount())));
    }

    public void testPostsTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testPostsPlaylistClickOpensPlaylistPage() {
        final PlaylistElement expectedPlaylist = profileScreen
                .getPlaylists()
                .get(0);

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testPlaylistsLoadsNextPage() {
        profileScreen.touchPlaylistsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        int playlistItemsBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(playlistItemsBefore, is(lessThan(profileScreen.currentItemCount())));
    }

    public void testPlaylistClickOpensPlaylistPage() {
        profileScreen.touchPlaylistsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        final PlaylistElement expectedPlaylist = profileScreen
                .getPlaylists()
                .get(0);

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testLikesLoadsNextPage() {
        profileScreen.touchLikesTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        int likesBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(likesBefore, is(lessThan(profileScreen.currentItemCount())));
    }

    public void testLikedPlaylistClickOpensPlaylistPage() {
        profileScreen.touchLikesTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        final PlaylistElement expectedPlaylist = profileScreen
                .getPlaylists()
                .get(0);

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testLikedTrackClickStartsPlayer() {
        profileScreen.touchLikesTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testFollowingsClickOpensProfilePage() {
        profileScreen.touchFollowingsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testFollowingsLoadsNextPage() {
        profileScreen.touchFollowingsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        int followingsBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(followingsBefore, is(lessThan(profileScreen.currentItemCount())));
    }
}

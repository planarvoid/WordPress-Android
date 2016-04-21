package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.tests.ActivityTest;

public class MyProfileTest extends ActivityTest<ResolveActivity> {

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() { TestUser.profileTestUser.logIn(getInstrumentation().getTargetContext()); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testShowsSoundsTab() {
        profileScreen.touchSoundsTab();
    }

    public void testTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testTracksViewAllOpensTracksPage() {
        profileScreen.clickViewAllTracks();
        assertEquals(profileScreen.getActionBarTitle(), ressourceString(R.string.user_profile_sounds_header_tracks));
    }

    public void testPlaylistClickOpensPlaylistPage() {
        PlaylistElement expectedPlaylist = profileScreen.getPlaylists().get(0);

        assertEquals(profileScreen.scrollToFirstPlaylist().click().getTitle(), expectedPlaylist.getTitle());
    }

    /*public void testPlaylistClickOpensPlaylistPage() {
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

        final PlaylistElement expectedPlaylist = profileScreen.scrollToFirstPlaylist();

        String targetPlaylistTitle = expectedPlaylist.getTitle();
        assertEquals(expectedPlaylist.click().getTitle(), targetPlaylistTitle);
    }

    public void testLikedTrackClickStartsPlayer() {
        profileScreen.touchLikesTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    */
    public void testFollowingsClickOpensProfilePage() {
        profileScreen.touchFollowingsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testFollowersClickOpensProfilePage() {
        profileScreen.touchFollowersTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testSpotlightExists() {
        assertTrue(profileScreen.getSpotlightTitle().hasVisibility());
    }
    /*

    public void testFollowingsLoadsNextPage() {
        profileScreen.touchLegacyFollowingsTab();
        waiter.waitForContentAndRetryIfLoadingFailed();

        int followingsBefore = profileScreen.currentItemCount();
        profileScreen.scrollToBottomAndLoadMoreItems();
        assertThat(followingsBefore, is(lessThan(profileScreen.currentItemCount())));
    }*/

    public void testClickFirstRepostOpensPlayer() {
        assertThat(profileScreen.testttt(), is(visible()));
    }
}

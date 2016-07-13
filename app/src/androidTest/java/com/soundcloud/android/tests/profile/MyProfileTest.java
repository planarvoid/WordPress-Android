package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.PlaylistElement;

public class MyProfileTest extends TrackingActivityTest<ResolveActivity> {
    private String MY_PROFILE_PAGEVIEWS_SCENARIO = "my_profile_pageview_events";

    private ProfileScreen profileScreen;

    public MyProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.profileTestUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        profileScreen = mainNavHelper.goToMyProfile();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testHasAllBucketsAndOpensListOnViewAll() {
        // TRACKS
        assertTrue(profileScreen.tracksHeader().hasVisibility());
        assertEquals(profileScreen.clickViewAllTracks().getActionBarTitle(),
                     ressourceString(R.string.user_profile_sounds_header_tracks));

        profileScreen.goBack();

        // TODO: Enable once albums switched on
        // ALBUMS
        // assertTrue(profileScreen.albumsHeader().hasVisibility());
        // assertEquals(profileScreen.clickViewAllAlbums().getActionBarTitle(),
        //        ressourceString(R.string.user_profile_sounds_header_albums));
        // profileScreen.goBack();

        // REPOSTS
        assertTrue(profileScreen.repostHeader().hasVisibility());
        assertEquals(profileScreen.clickViewAllReposts().getActionBarTitle(),
                     ressourceString(R.string.user_profile_sounds_header_reposts));

        profileScreen.goBack();

        // LIKES
        assertTrue(profileScreen.likesHeader().hasVisibility());
        assertEquals(profileScreen.clickViewAllLikes().getActionBarTitle(),
                     ressourceString(R.string.user_profile_sounds_header_likes));
    }

    public void testPlaylistClickOpensPlaylistPage() {
        final PlaylistElement expectedPlaylist = profileScreen
                .scrollToPlaylists()
                .get(0);

        assertEquals(profileScreen.scrollToFirstPlaylist().click().getTitle(), expectedPlaylist.getTitle());
    }

    public void testFollowingsClickOpensProfilePage() {
        profileScreen.touchFollowingsTab();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testFollowersClickOpensProfilePage() {
        profileScreen.touchFollowersTab();

        assertTrue(profileScreen.clickUserAt(0).isVisible());
    }

    public void testPageViewEvents() {
        startEventTracking();

        profileScreen.touchInfoTab();
        profileScreen.touchSoundsTab();
        profileScreen.touchFollowersTab();
        profileScreen.touchFollowingsTab();

        finishEventTracking(MY_PROFILE_PAGEVIEWS_SCENARIO);
    }
}

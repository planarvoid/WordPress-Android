package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.Element;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

public class MyProfileTest extends TrackingActivityTest<ResolveActivity> {
    private static final String MY_PROFILE_PAGEVIEWS_SCENARIO = "my_profile_pageview_events";

    // Have to do this because Java can't do import aliasing ;_;
    private static Matcher<Screen> isScreenVisible() {
        return Is.is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible());
    }

    private static Matcher<Element> isElementVisible() {
        return Is.is(com.soundcloud.android.framework.matcher.element.IsVisible.visible());
    }

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

    public void testPlayAndPauseTrackTracksBucket() {
        final VisualPlayerElement playerElement = profileScreen
                .scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.TRACKS);

        assertPlayAndPause(playerElement);
    }

    public void testOpenPlaylistFromPlaylistsBucket() {
        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .scrollToBucketAndClickFirstPlaylist(ProfileScreen.Bucket.PLAYLISTS);

        assertThat(playlistDetailsScreen, isScreenVisible());
    }

    public void testOpenPlaylistFromAlbumsBucket() {
        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .scrollToBucketAndClickFirstPlaylist(ProfileScreen.Bucket.ALBUMS);

        assertThat(playlistDetailsScreen, isScreenVisible());
    }

    public void testPlayAndPauseTrackRepostsBucket() {
        final VisualPlayerElement playerElement = profileScreen
                .scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.REPOSTS);

        assertPlayAndPause(playerElement);
    }

    public void testPlayAndPauseTrackLikesBucket() {
        final VisualPlayerElement playerElement = profileScreen
                .scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.LIKES);

        assertPlayAndPause(playerElement);
    }

    public void testPlayAndPauseFromMyTracksList() {
        final VisualPlayerElement playerElement = profileScreen.scrollToViewAllTracks()
                                                               .goToAllTracks()
                                                               .clickFirstTrack();

        assertPlayAndPause(playerElement);
    }

    public void testOpenPlaylistFromMyPlaylistsList() {
        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen.scrollToViewAllPlaylists()
                                                                         .goToAllPlaylists()
                                                                         .clickFirstPlaylist();
        assertThat(playlistDetailsScreen, isScreenVisible());
    }

    public void testPlayAndPauseFromMyRepostsList() {
        final VisualPlayerElement playerElement = profileScreen.scrollToViewAllReposts()
                                                               .goToAllReposts()
                                                               .clickFirstTrack();
        assertPlayAndPause(playerElement);
    }

    public void testPlayAndPauseFromMyLikesList() {
        final VisualPlayerElement playerElement = profileScreen.scrollToViewAllLikes()
                                                               .goToAllLikes()
                                                               .clickFirstTrack();
        assertPlayAndPause(playerElement);
    }

    private void assertPlayAndPause(final VisualPlayerElement playerElement) {
        assertThat(playerElement, isElementVisible());
        assertThat(playerElement, Is.is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));
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

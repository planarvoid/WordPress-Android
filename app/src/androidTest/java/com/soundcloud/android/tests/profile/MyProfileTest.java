package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.ProfileAlbumTest;
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
    private static final String TEST_SCENARIO_TRACKS_BUCKET = "audio-events-v1-you-profile-tracks-bucket";
    private static final String TEST_SCENARIO_LIKES_BUCKET = "audio-events-v1-you-profile-likes-bucket";
    private static final String TEST_SCENARIO_REPOSTS_BUCKET = "audio-events-v1-you-profile-reposts-bucket";
    private static final String TEST_SCENARIO_PLAYLISTS_BUCKET = "audio-events-v1-you-profile-playlists-bucket";
    private static final String TEST_SCENARIO_ALBUMS_BUCKET = "audio-events-v1-you-profile-albums-bucket";

    private static final String TEST_SCENARIO_TRACKS_LIST = "audio-events-v1-you-profile-tracks-list";
    private static final String TEST_SCENARIO_LIKES_LIST = "audio-events-v1-you-profile-likes-list";
    private static final String TEST_SCENARIO_REPOSTS_LIST = "audio-events-v1-you-profile-reposts-list";
    private static final String TEST_SCENARIO_PLAYLISTS_LIST = "audio-events-v1-you-profile-playlists-list";
    private static final String TEST_SCENARIO_ALBUMS_LIST = "audio-events-v1-you-profile-albums-list";

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
        startScenario(TEST_SCENARIO_TRACKS_BUCKET);

        final VisualPlayerElement playerElement = profileScreen.scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.TRACKS);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_TRACKS_BUCKET);
    }

    public void testOpenPlaylistFromPlaylistsBucket() {
        startScenario(TEST_SCENARIO_PLAYLISTS_BUCKET);

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen.scrollToBucketAndClickFirstPlaylist(
                ProfileScreen.Bucket.PLAYLISTS);

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_PLAYLISTS_BUCKET);
    }

    @ProfileAlbumTest
    public void testOpenPlaylistFromAlbumsBucket() {
        startScenario(TEST_SCENARIO_ALBUMS_BUCKET);

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen.scrollToBucketAndClickFirstPlaylist(
                ProfileScreen.Bucket.ALBUMS);

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_ALBUMS_BUCKET);
    }

    public void testPlayAndPauseTrackRepostsBucket() {
        startScenario(TEST_SCENARIO_REPOSTS_BUCKET);

        final VisualPlayerElement playerElement = profileScreen.scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.REPOSTS);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_REPOSTS_BUCKET);
    }

    public void testPlayAndPauseTrackLikesBucket() {
        startScenario(TEST_SCENARIO_LIKES_BUCKET);

        final VisualPlayerElement playerElement = profileScreen.scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.LIKES);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_LIKES_BUCKET);
    }

    // Testing from full list views

    public void testPlayAndPauseFromMyTracksList() {
        startScenario(TEST_SCENARIO_TRACKS_LIST);

        final VisualPlayerElement playerElement = profileScreen.scrollToAndClickViewAllTracks()
                                                               .clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_TRACKS_LIST);
    }

    public void testOpenPlaylistFromMyPlaylistsList() {
        startScenario(TEST_SCENARIO_PLAYLISTS_LIST);

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen.scrollToAndClickViewAllPlaylists()
                                                                         .clickFirstPlaylist();

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_PLAYLISTS_LIST);
    }

    @ProfileAlbumTest
    public void testOpenPlaylistFromMyAlbumsList() {
        startScenario(TEST_SCENARIO_ALBUMS_LIST);

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen.scrollToAndClickViewAllAlbums()
                                                                         .clickFirstPlaylist();

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_ALBUMS_LIST);
    }

    public void testPlayAndPauseFromMyRepostsList() {
        startScenario(TEST_SCENARIO_REPOSTS_LIST);

        final VisualPlayerElement playerElement = profileScreen.scrollToAndClickViewAllReposts()
                                                               .clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_REPOSTS_LIST);
    }

    public void testPlayAndPauseFromMyLikesList() {
        startScenario(TEST_SCENARIO_LIKES_LIST);

        final VisualPlayerElement playerElement = profileScreen.scrollToAndClickViewAllLikes()
                                                               .clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_LIKES_LIST);
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

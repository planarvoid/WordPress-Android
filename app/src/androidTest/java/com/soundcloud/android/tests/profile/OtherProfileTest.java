package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.Element;
import com.soundcloud.android.screens.elements.PlaylistElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;

import android.content.Intent;

public class OtherProfileTest extends TrackingActivityTest<ResolveActivity> {
    private static final String OTHER_PROFILE_PAGEVIEWS_SCENARIO = "other_profile_pageview_events";
    private static final String TEST_SCENARIO_TRACKS_BUCKET = "audio-events-v1-other-profile-tracks-bucket";
    private static final String TEST_SCENARIO_LIKES_BUCKET = "audio-events-v1-other-profile-likes-bucket";
    private static final String TEST_SCENARIO_REPOSTS_BUCKET = "audio-events-v1-other-profile-reposts-bucket";
    private static final String TEST_SCENARIO_PLAYLISTS_BUCKET = "other-profile-playlists-bucket";
    private static final String TEST_SCENARIO_ALBUMS_BUCKET = "other-profile-albums-bucket";
    private static final String TEST_SCENARIO_FOLLOW_USER = "follow-user";

    private static final String TEST_SCENARIO_TRACKS_LIST = "audio-events-v1-other-profile-tracks-list";
    private static final String TEST_SCENARIO_LIKES_LIST = "audio-events-v1-other-profile-likes-list";
    private static final String TEST_SCENARIO_REPOSTS_LIST = "audio-events-v1-other-profile-reposts-list";
    private static final String TEST_SCENARIO_PLAYLISTS_LIST = "other-profile-playlists-list";

    // Have to do this because Java can't do import aliasing ;_;
    private static Matcher<Screen> isScreenVisible() {
        return Is.is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible());
    }

    private static Matcher<Element> isElementVisible() {
        return Is.is(com.soundcloud.android.framework.matcher.element.IsVisible.visible());
    }

    private ProfileScreen profileScreen;

    public OtherProfileTest() {
        super(ResolveActivity.class);
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.OTHER_PROFILE_USER_URI));
        super.setUp();

        profileScreen = new ProfileScreen(solo);
    }

    public void testPostsTrackClickStartsPlayer() {
        assertThat(profileScreen.playTrack(0), is(visible()));
    }

    public void testPostsPlaylistClickOpensPlaylistPage() {
        final PlaylistElement expectedPlaylist = profileScreen
                .scrollToPlaylists()
                .get(0);

        assertEquals(expectedPlaylist.click().getTitle(), expectedPlaylist.getTitle());
    }

    public void testClickFollowingsLoadsProfile() {
        profileScreen.touchFollowingsTab();

        final UserItemElement expectedUser = profileScreen
                .getUsers()
                .get(0);

        String targetUsername = expectedUser.getUsername();
        assertEquals(expectedUser.click().getUserName(), targetUsername);
    }

    public void testPlayAndPauseFromTracksBucket() {
        startEventTracking();

        final VisualPlayerElement playerElement =
                profileScreen.scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.TRACKS);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_TRACKS_BUCKET);
    }

    public void testOpenPlaylistFromPlaylistsBucket() {
        startEventTracking();

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .scrollToBucketAndClickFirstPlaylist(ProfileScreen.Bucket.PLAYLISTS);

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_PLAYLISTS_BUCKET);
    }

    public void testOpenPlaylistFromAlbumsBucket() {
        assertTrue(profileScreen.albumsHeader().hasVisibility());
        
        startEventTracking();

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .scrollToBucketAndClickFirstPlaylist(ProfileScreen.Bucket.ALBUMS);

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_ALBUMS_BUCKET);
    }

    public void testPlayAndPauseFromRepostsBucket() {
        startEventTracking();

        final VisualPlayerElement playerElement = profileScreen
                .scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.REPOSTS);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_REPOSTS_BUCKET);
    }

    public void testPlayAndPauseFromLikesBucket() {
        startEventTracking();

        final VisualPlayerElement playerElement = profileScreen
                .scrollToBucketAndClickFirstTrack(ProfileScreen.Bucket.LIKES);

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_LIKES_BUCKET);
    }

    public void testPlayAndPauseFromTracksList() {
        profileScreen.scrollToViewAllTracks();
        startEventTracking();
        final VisualPlayerElement playerElement = profileScreen.goToAllTracks().clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_TRACKS_LIST);
    }

    public void testOpenPlaylistFromPlaylistsList() {
        profileScreen.scrollToViewAllPlaylists();
        startEventTracking();

        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .goToAllPlaylists()
                .clickFirstPlaylist();

        assertThat(playlistDetailsScreen, isScreenVisible());

        endScenario(TEST_SCENARIO_PLAYLISTS_LIST);
    }

    public void testPlayAndPauseFromRepostsList() {
        profileScreen.scrollToViewAllReposts();
        startEventTracking();

        final VisualPlayerElement playerElement = profileScreen
                .goToAllReposts()
                .clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_REPOSTS_LIST);
    }

    public void testPlayAndPauseFromLikesList() {
        profileScreen.scrollToViewAllLikes();
        startEventTracking();

        final VisualPlayerElement playerElement = profileScreen
                .goToAllLikes()
                .clickFirstTrack();

        assertPlayAndPause(playerElement);

        endScenario(TEST_SCENARIO_LIKES_LIST);
    }

    public void testFollowUserTracking() {
        startEventTracking();

        profileScreen.clickFollowToggle();

        endScenario(TEST_SCENARIO_FOLLOW_USER);
    }

    private void assertPlayAndPause(final VisualPlayerElement playerElement) {
        assertThat(playerElement, isElementVisible());
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));
    }

    public void testPageViewEvents() {
        startEventTracking();

        profileScreen.touchInfoTab();
        profileScreen.touchSoundsTab();
        profileScreen.touchFollowersTab();
        profileScreen.touchFollowingsTab();

        finishEventTracking(OTHER_PROFILE_PAGEVIEWS_SCENARIO);
    }
}

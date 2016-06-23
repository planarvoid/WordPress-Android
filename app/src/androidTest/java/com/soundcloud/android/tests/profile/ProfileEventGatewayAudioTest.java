package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.NewProfileTest;
import com.soundcloud.android.framework.annotation.ProfileAlbumTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.Element;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.Matcher;

@EventTrackingTest
@NewProfileTest
public class ProfileEventGatewayAudioTest extends TrackingActivityTest<MainActivity> {
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
        return is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible());
    }

    private static Matcher<Element> isElementVisible() {
        return is(com.soundcloud.android.framework.matcher.element.IsVisible.visible());
    }

    private ProfileScreen profileScreen;

    public ProfileEventGatewayAudioTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        profileScreen = mainNavHelper.goToMyProfile();
    }

    @Override
    protected void logInHelper() {
        TestUser.profileTestUser.logIn(getInstrumentation().getTargetContext());
    }

    // Testing from bucket views

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
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));
    }

}

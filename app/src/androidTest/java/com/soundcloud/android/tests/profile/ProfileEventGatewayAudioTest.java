package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.NewProfileTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
@NewProfileTest
public class ProfileEventGatewayAudioTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_POSTS = "audio-events-v1-you-posts";
    private static final String TEST_SCENARIO_LIKES = "audio-events-v1-you-likes";
    private static final String TEST_SCENARIO_PLAYLISTS = "audio-events-v1-you-playlists";

    public ProfileEventGatewayAudioTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testPlayAndPauseTrackFromPosts() {
        final ProfileScreen profileScreen = mainNavHelper.goToMyProfile();

        startEventTracking();

        final VisualPlayerElement playerElement =
                profileScreen.clickFirstRepostedTrack();

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_POSTS);
    }

    public void testPlayAndPauseTrackFromMyPlaylist() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper
                .goToMyProfile()
                .scrollToFirstPlaylist()
                .click();

        startEventTracking();

        // TODO: event tracking has not been implemented for the new profile yet, theses tests will fail
        final VisualPlayerElement playerElement =
                playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_PLAYLISTS);
    }

    public void testPlayAndPauseTrackFromLikes() {
        final ProfileScreen profileScreen = mainNavHelper
                .goToMyProfile();

        startEventTracking();

        // TODO: figure out how to trigger a track from a specific bucket
        final VisualPlayerElement playerElement =
                profileScreen.playTrack(0);

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_LIKES);
    }
}

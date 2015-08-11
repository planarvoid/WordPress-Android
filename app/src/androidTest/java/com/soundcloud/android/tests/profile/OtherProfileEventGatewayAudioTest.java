package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.profileEntryUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.content.Intent;

@EventTrackingTest
public class OtherProfileEventGatewayAudioTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_POSTS = "audio-events-v1-user-posts";
    private static final String TEST_SCENARIO_LIKES = "audio-events-v1-user-likes";
    private static final String TEST_SCENARIO_PLAYLISTS = "audio-events-v1-user-playlists";

    private ProfileScreen profileScreen;

    public OtherProfileEventGatewayAudioTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.EVENTLOGGER_AUDIO_V1);
        setActivityIntent(new Intent(Intent.ACTION_VIEW).setData(TestConsts.OTHER_PROFILE_USER_URI));
        super.setUp();

        profileScreen = new ProfileScreen(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    @Override
    protected void logInHelper() {
        profileEntryUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testPlayAndPauseTrackFromPosts() {
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
        final PlaylistDetailsScreen playlistDetailsScreen = profileScreen
                .touchPlaylistsTab()
                .clickFirstPlaylistWithTracks();

        startEventTracking();

        final VisualPlayerElement playerElement =
                playlistDetailsScreen.clickFirstTrack();

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_PLAYLISTS);
    }

    public void testPlayAndPauseTrackFromLikes() {
        profileScreen.touchLikesTab();

        startEventTracking();

        final VisualPlayerElement playerElement =
                profileScreen.playTrack(0);

        assertThat(playerElement, is(visible()));
        assertThat(playerElement, is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, is(not(playing())));

        finishEventTracking(TEST_SCENARIO_LIKES);
    }
}

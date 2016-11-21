package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.core.Is;

public class StreamTest extends TrackingActivityTest<LauncherActivity> {
    private static final String TEST_SCENARIO_STREAM = "audio-events-v1-stream";
    private static final String TEST_SCENARIO_STREAM_PLAYLIST = "audio-events-v1-stream-playlist";
    private static final String REPOST_TRACK_PLAYING_FROM_STREAM = "stream_engagements_repost_from_player";
    private static final String LIKE_TRACK_PLAYING_FROM_STREAM = "stream_engagements_like_from_player";

    private StreamScreen streamScreen;

    public StreamTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    public void testStreamLoadsNextPage() {
        int itemsBeforePaging = streamScreen.getItemCount();
        streamScreen.scrollToBottomOfPage();
        assertThat(streamScreen.getItemCount(), is(greaterThan(itemsBeforePaging)));
    }

    public void testPlayAndPauseTrackFromStream() {
        startEventTracking();

        final VisualPlayerElement playerElement =
                streamScreen.clickFirstRepostedTrack();

        assertThat(playerElement, is(expanded()));
        assertThat(playerElement, Is.is(visible()));
        assertThat(playerElement, Is.is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));

        // Like
        ViewElement likeButton = playerElement.likeButton();
        likeButton.click();
        likeButton.click();

        finishEventTracking(LIKE_TRACK_PLAYING_FROM_STREAM);

        playerElement.pressBackToCollapse();

        startEventTracking();

        final VisualPlayerElement playerElement2 =
                streamScreen.clickFirstRepostedTrack();

        // Repost
        playerElement2.clickMenu().toggleRepost();
        // fake wait, so that the snackbar with reposted message disappears
        playerElement2.playForFiveSeconds();
        playerElement2.clickMenu().toggleRepost();

        finishEventTracking(REPOST_TRACK_PLAYING_FROM_STREAM);
    }

    public void testPlayAndPausePlaylistTrackFromStream() {
        startEventTracking();

        final VisualPlayerElement playerElement =
                streamScreen.clickFirstNotPromotedPlaylistCard().clickFirstTrack();

        assertThat(playerElement, Is.is(visible()));
        assertThat(playerElement, Is.is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));

        finishEventTracking(TEST_SCENARIO_STREAM_PLAYLIST);
    }
}

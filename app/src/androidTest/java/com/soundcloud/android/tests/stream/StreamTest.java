package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.core.Is;

public class StreamTest extends TrackingActivityTest<LauncherActivity> {
    private static final String TEST_SCENARIO_STREAM_PLAYLIST = "audio-events-v1-stream-playlist";

    private StreamScreen streamScreen;

    public StreamTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
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

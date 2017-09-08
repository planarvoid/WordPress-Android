package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.core.Is;
import org.junit.Test;

public class StreamTest extends ActivityTest<LauncherActivity> {
    private static final String TEST_SCENARIO_STREAM_PLAYLIST = "specs/audio-events-v1-stream-playlist.spec";

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

    @Test
    public void testStreamLoadsNextPage() throws Exception {
        int itemsBeforePaging = streamScreen.getItemCount();
        streamScreen.scrollToBottomOfPage();
        assertThat(streamScreen.getItemCount(), is(greaterThan(itemsBeforePaging)));
    }

    @Test
    public void testPlayAndPausePlaylistTrackFromStream() throws Exception {
        mrLocalLocal.startEventTracking();

        final VisualPlayerElement playerElement =
                streamScreen.clickFirstNotPromotedPlaylistCard().clickFirstTrack();

        assertThat(playerElement, Is.is(visible()));
        assertThat(playerElement, Is.is(playing()));

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));

        mrLocalLocal.verify(TEST_SCENARIO_STREAM_PLAYLIST);
    }
}

package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.matchers.SoundCloudMatchers.queryStringEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.UnsupportedEncodingException;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerParamsBuilderTest {

    @Mock
    private Track track;
    @Mock
    private TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        when(trackSourceInfo.getOriginScreen()).thenReturn("origin");
        when(trackSourceInfo.getIsUserTriggered()).thenReturn(true);
    }

    @Test
    public void createParamsWithOriginAndTrigger() throws Exception {
        checkUrl("action=play&ts=321&duration=0&sound=soundcloud%3Asounds%3A0&user=soundcloud%3Ausers%3A1&trigger=manual&context=origin");
    }

    @Test
    public void createParamsWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        checkUrl("duration=0&ts=321&action=play&sound=soundcloud%3Asounds%3A0&user=soundcloud%3Ausers%3A1&trigger=manual&context=origin&source=source1&source_version=version1");
    }

    @Test
    public void createParamsFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        checkUrl("ts=321&action=play&duration=0&sound=soundcloud%3Asounds%3A0&user=soundcloud%3Ausers%3A1&trigger=manual&context=origin&set_id=123&set_position=2");
    }

    @Test
    public void createFullParams() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        checkUrl("ts=321&action=play&duration=0&sound=soundcloud%3Asounds%3A0&user=soundcloud%3Ausers%3A1&trigger=manual&context=origin&source=source1&source_version=version1&set_id=123&set_position=2");
    }

    private void checkUrl(String expected) throws UnsupportedEncodingException {
        final String actualQueryString = new EventLoggerParamsBuilder().buildFromPlaybackEvent(PlaybackEvent.forPlay(track, 1L, trackSourceInfo, 321L));
        assertThat(actualQueryString, is(queryStringEqualTo(expected)));
    }
}

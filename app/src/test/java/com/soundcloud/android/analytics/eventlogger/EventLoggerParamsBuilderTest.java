package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerParamsBuilderTest {

    @Mock
    TrackSourceInfo trackSourceInfo;

    @Before
    public void setUp() throws Exception {
        when(trackSourceInfo.getOriginScreen()).thenReturn("origin");
        when(trackSourceInfo.getIsUserTriggered()).thenReturn(true);
    }

    @Test
    public void createParamsWithJustOriginAndTrigger() throws Exception {
        expect(new EventLoggerParamsBuilder().build(trackSourceInfo)).toEqual("trigger=manual&context=origin");
    }

    @Test
    public void createParamsWithSourceAndSourceVersion() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        expect(new EventLoggerParamsBuilder().build(trackSourceInfo)).toEqual("trigger=manual&context=origin&source=source1&source_version=version1");
    }

    @Test
    public void createParamsFromPlaylist() throws Exception {
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        expect(new EventLoggerParamsBuilder().build(trackSourceInfo)).toEqual("trigger=manual&context=origin&set_id=123&set_position=2");
    }

    @Test
    public void createFullParams() throws Exception {
        when(trackSourceInfo.hasSource()).thenReturn(true);
        when(trackSourceInfo.getSource()).thenReturn("source1");
        when(trackSourceInfo.getSourceVersion()).thenReturn("version1");
        when(trackSourceInfo.isFromPlaylist()).thenReturn(true);
        when(trackSourceInfo.getPlaylistId()).thenReturn(123L);
        when(trackSourceInfo.getPlaylistPosition()).thenReturn(2);
        expect(new EventLoggerParamsBuilder().build(trackSourceInfo)).toEqual("trigger=manual&context=origin&source=source1&source_version=version1&set_id=123&set_position=2");
    }
}

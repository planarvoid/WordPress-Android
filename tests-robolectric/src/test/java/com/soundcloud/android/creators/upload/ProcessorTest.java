package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ProcessorTest {
    private TestEventBus eventBus = new TestEventBus();
    private Recording recording;
    Processor processor;

    @Mock private PlaybackStream playbackStream;

    @Before
    public void before() throws Exception {
        recording = RecordingTestHelper.getValidRecording();
        processor = new Processor(recording, eventBus);
    }

    @Test
    public void shouldNotProcessFileIfNotModified() throws Exception {
        processor.run();

        expect(eventBus.lastEventOn(EventQueue.UPLOAD)).toEqual(
                UploadEvent.processingSuccess(recording));
    }

    @Test
    public void shouldTrimFileIfBoundsSet() throws Exception {
        recording.setPlaybackStream(playbackStream);
        when(playbackStream.getEndPos()).thenReturn(20l);

        processor.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.processingStarted(recording),
                UploadEvent.processingSuccess(recording));

        expect(recording.getEncodedFile().exists()).toBeTrue();
    }
}

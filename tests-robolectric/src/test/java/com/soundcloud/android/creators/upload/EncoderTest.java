package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EncoderTest {
    private TestEventBus eventBus = new TestEventBus();
    private Recording recording;
    private Encoder encoder;

    @Before
    public void before() throws Exception {
        recording = RecordingTestHelper.getValidRecording();
        encoder = new Encoder(recording);
    }

    @Test
    public void shouldEncode() throws Exception {
        expect(recording.getEncodedFile().delete()).toBeTrue();
        Recording rec = RecordingTestHelper.getValidRecording();
        rec.getEncodedFile().delete();

        ShadowVorbisEncoder.simulateProgress = true;
        encoder.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(4);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.processingStarted(recording),
                UploadEvent.processingProgress(recording, 0),
                UploadEvent.processingSuccess(recording));

        expect(recording.getEncodedFile().exists()).toBeTrue();
    }

    @Test
    public void shouldHandleEncodingFailure() throws Exception {
        Recording rec = RecordingTestHelper.getValidRecording();
        ShadowVorbisEncoder.throwException = new IOException();
        encoder.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.processingStarted(recording),
                UploadEvent.error(recording));
    }

    @Test
    public void shouldHandleCancel() throws Exception {
        Recording rec = RecordingTestHelper.getValidRecording();
        ShadowVorbisEncoder.simulateCancel = true;
        encoder.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.processingStarted(recording),
                UploadEvent.cancelled(recording));
    }

    @Test
    public void shouldMakeSureOutputfileGetsCreatedAtomically() throws Exception {
        expect(recording.getEncodedFile().delete()).toBeTrue();
        Recording rec = RecordingTestHelper.getValidRecording();
        expect(rec.getEncodedFile().delete()).toBeTrue();

        ShadowVorbisEncoder.simulateCancel = true;
        encoder.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.processingStarted(recording),
                UploadEvent.cancelled(recording));

        expect(recording.getEncodedFile().exists()).toBeFalse();
    }
}

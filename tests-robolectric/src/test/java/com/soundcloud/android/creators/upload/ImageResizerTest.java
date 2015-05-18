package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ImageResizerTest {
    private TestEventBus eventBus = new TestEventBus();

    private ImageResizer resizer;
    private Recording recording;

    @Before
    public void before() throws Exception {
        recording = RecordingTestHelper.getValidRecording();
        resizer = new ImageResizer(recording, eventBus);
    }

    @Test
    public void shouldNotResizeWithEmptyArtwork() throws Exception {
        resizer.run();
        expect(eventBus.lastEventOn(EventQueue.UPLOAD).isError()).toBeTrue();
    }

    @Test
    public void shouldResizeWithArtwork() throws Exception {
        recording.artwork_path = TestApplication.createJpegFile();
        resizer.run();

        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.resizeStarted(recording),
                UploadEvent.resizeSuccess(recording));
    }
}

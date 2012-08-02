package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ProcessorTest {
    List<Intent> intents = new ArrayList<Intent>();
    List<String> actions = new ArrayList<String>();

    @Mock private PlaybackStream playbackStream;


    @Before
    public void before() {
        LocalBroadcastManager.getInstance(Robolectric.application).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intents.add(intent);
                actions.add(intent.getAction());
            }
        }, UploadService.getIntentFilter());
    }

    @Test
    public void shouldNotProcessFileIfNotModified() throws Exception {
        Recording r = TestApplication.getValidRecording();
        Processor processor = new Processor(Robolectric.application, r);
        processor.run();
        expect(actions).toContainExactly(UploadService.PROCESSING_SUCCESS);
    }

    @Test
    public void shouldTrimFileIfBoundsSet() throws Exception {
        Recording r = TestApplication.getValidRecording();
        r.setPlaybackStream(playbackStream);
        when(playbackStream.getEndPos()).thenReturn(20l);

        Processor processor = new Processor(Robolectric.application, r);
        processor.run();
        expect(intents).not.toBeEmpty();
        expect(actions).toContain(UploadService.PROCESSING_STARTED, UploadService.PROCESSING_SUCCESS);
    }
}

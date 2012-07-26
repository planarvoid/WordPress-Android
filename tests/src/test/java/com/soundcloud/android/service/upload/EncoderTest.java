package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.shadows.ShadowVorbisEncoder;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class EncoderTest {
    List<Intent> intents = new ArrayList<Intent>();
    List<String> actions = new ArrayList<String>();

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
    public void shouldEncode() throws Exception {
        Recording rec = TestApplication.getValidRecording();
        ShadowVorbisEncoder.simulateProgress = true;
        Encoder encoder = new Encoder(Robolectric.application, rec);
        encoder.run();
        expect(actions).toContainExactly(UploadService.PROCESSING_STARTED,
                UploadService.PROCESSING_PROGRESS,
                UploadService.PROCESSING_PROGRESS,
                UploadService.PROCESSING_PROGRESS,
                UploadService.PROCESSING_SUCCESS);
    }

    @Test
    public void shouldHandleEncodingFailure() throws Exception {
        Recording rec = TestApplication.getValidRecording();
        ShadowVorbisEncoder.throwException = new IOException();
        Encoder encoder = new Encoder(Robolectric.application, rec);
        encoder.run();
        expect(actions).toContainExactly(UploadService.PROCESSING_STARTED, UploadService.PROCESSING_ERROR);
    }
}

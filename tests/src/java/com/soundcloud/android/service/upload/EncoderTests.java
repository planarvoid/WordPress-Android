package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class EncoderTests {
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
        rec.audio_path =  new File(getClass().getResource("short_test.wav").getFile());
        Encoder encoder = new Encoder(Robolectric.application, rec);
        encoder.run();
        expect(actions).toContainExactly(UploadService.ENCODING_STARTED, UploadService.ENCODING_SUCCESS);
    }
}

package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ImageResizerTest {
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
    public void shouldNotResizeWithEmptyArtwork() throws Exception {
        ImageResizer resizer = new ImageResizer(Robolectric.application, RecordingTestHelper.getValidRecording());
        resizer.run();
        expect(actions).toContainExactly(UploadService.RESIZE_ERROR);
    }

    @Test
    public void shouldResizeWithArtwork() throws Exception {
        final Recording recording = RecordingTestHelper.getValidRecording();
        ImageResizer resizer = new ImageResizer(Robolectric.application, recording);
        recording.artwork_path = TestApplication.createJpegFile();
        resizer.run();

        expect(actions).toContainExactly(UploadService.RESIZE_STARTED, UploadService.RESIZE_SUCCESS);
    }
}

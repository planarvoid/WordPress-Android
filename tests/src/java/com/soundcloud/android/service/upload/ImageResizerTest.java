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

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
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
        ImageResizer resizer = new ImageResizer(DefaultTestRunner.application, TestApplication.getValidRecording());
        resizer.run();
        expect(actions).toContainExactly(UploadService.RESIZE_ERROR);
    }

    @Test
    public void shouldResizeWithArtwork() throws Exception {
        final Recording recording = TestApplication.getValidRecording();
        ImageResizer resizer = new ImageResizer(DefaultTestRunner.application, recording);
        recording.artwork_path = TestApplication.getTestFile();
        resizer.run();

        expect(actions).toContainExactly(UploadService.RESIZE_STARTED, UploadService.RESIZE_SUCCESS);
    }
}

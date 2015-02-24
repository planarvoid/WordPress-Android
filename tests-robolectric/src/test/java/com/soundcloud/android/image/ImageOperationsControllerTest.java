package com.soundcloud.android.image;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.app.ActionBarActivity;

@RunWith(SoundCloudTestRunner.class)
public class ImageOperationsControllerTest {
    private ImageOperationsController imageOperationsController;
    @Mock private ImageOperations imageOperations;
    @Mock private ActionBarActivity activity;

    @Before
    public void setUp() throws Exception {
        imageOperationsController = new ImageOperationsController(imageOperations);
    }

    @Test
    public void resumeImageOperationsOnResume() {
        imageOperationsController.onResume(activity);

        verify(imageOperations).resume();
    }
}
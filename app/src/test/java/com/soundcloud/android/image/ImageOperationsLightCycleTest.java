package com.soundcloud.android.image;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class ImageOperationsLightCycleTest {
    private ImageOperationsLightCycle imageOperationsLightCycle;
    @Mock private ImageOperations imageOperations;
    @Mock private FragmentActivity activity;

    @Before
    public void setUp() throws Exception {
        imageOperationsLightCycle = new ImageOperationsLightCycle(imageOperations);
    }

    @Test
    public void resumeImageOperationsOnResume() {
        imageOperationsLightCycle.onResume(activity);

        verify(imageOperations).resume();
    }
}
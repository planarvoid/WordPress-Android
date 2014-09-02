package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Build;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ScrollXHelperTest {

    @Mock private View progressView;

    @Test
    public void doesNotSetScrollXOnViewForHoneycomb() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB_MR2);
        new ScrollXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView, times(0)).setScrollX(100);
    }

    @Test
    public void callsSetScrollXForIceCreamSandwich() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        new ScrollXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView).setScrollX(100);
    }
}

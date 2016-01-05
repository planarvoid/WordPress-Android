package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class ScrollXHelperTest {

    @Mock private View progressView;

    @Test
    public void callsSetScrollXForIceCreamSandwich() {
        new ScrollXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView).setScrollX(100);
    }
}

package com.soundcloud.android.playback.ui.progress;

import static org.mockito.Mockito.verify;

import com.nineoldandroids.animation.Animator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.os.Build;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TranslateXAnimatorHCTest {

    private TranslateXAnimatorHC translateXAnimatorHC;

    @Mock private View progressView;

    @Before
    public void setUp() throws Exception {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB);
        translateXAnimatorHC = new TranslateXAnimatorHC(progressView, 0f, 100f);
    }

    @Test
    public void startAnimationEnablesHardwareLayer() {
        translateXAnimatorHC.start();
        verify(progressView).setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Test
    public void animationListenerDisablesHardwareListenerOnFinish() {
        translateXAnimatorHC.getWrappedAnimationListeners().get(0).onAnimationEnd(Mockito.mock(Animator.class));
        verify(progressView).setLayerType(View.LAYER_TYPE_NONE, null);
    }
}
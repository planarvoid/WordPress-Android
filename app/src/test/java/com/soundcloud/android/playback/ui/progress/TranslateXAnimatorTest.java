package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.Expect.expect;

import com.nineoldandroids.animation.ObjectAnimator;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;
import android.view.animation.LinearInterpolator;

@RunWith(SoundCloudTestRunner.class)
public class TranslateXAnimatorTest {

    @Mock private View progressView;

    private TranslateXAnimator translateXAnimator;

    @Before
    public void setUp() throws Exception {
        translateXAnimator = new TranslateXAnimator(progressView, 0f, 100f);
    }

    @Test
    public void createAnimatorCreatesAnimatorForProgressView() {
        ObjectAnimator animator = translateXAnimator.createAnimator(0f, 100f);
        expect(animator.getTarget()).toBe(progressView);
    }

    @Test
    public void createAnimatorCreatesAnimatorForTranslateXProperty() {
        ObjectAnimator animator = translateXAnimator.createAnimator(0f, 100f);
        expect(animator.getPropertyName()).toEqual("translationX");
    }

    @Test
    public void createAnimatorCreatesAnimatorWithLinearInterpolator() {
        ObjectAnimator animator = translateXAnimator.createAnimator(0f, 100f);
        expect(animator.getInterpolator()).toBeInstanceOf(LinearInterpolator.class);
    }
}
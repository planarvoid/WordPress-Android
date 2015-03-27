package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.animation.ObjectAnimator;
import android.view.View;

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

}
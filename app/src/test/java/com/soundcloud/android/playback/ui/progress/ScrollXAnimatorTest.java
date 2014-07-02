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
public class ScrollXAnimatorTest {

    private ScrollXAnimator scrollXAnimator;

    @Mock private View progressView;

    @Before
    public void setUp() throws Exception {
        scrollXAnimator = new ScrollXAnimator(progressView, 0, 100);
    }

    @Test
    public void createAnimatorCreatesAnimatorForProgressView() {
        ObjectAnimator animator = scrollXAnimator.createAnimator(0f, 100f);
        expect(animator.getTarget()).toBe(progressView);
    }

    @Test
    public void createAnimatorCreatesAnimatorForScrolLXProperty() {
        ObjectAnimator animator = scrollXAnimator.createAnimator(0f, 100f);
        expect(animator.getPropertyName()).toEqual("scrollX");
    }

    @Test
    public void createAnimatorCreatesAnimatorWithLinearInterpolator() {
        ObjectAnimator animator = scrollXAnimator.createAnimator(0f, 100f);
        expect(animator.getInterpolator()).toBeInstanceOf(LinearInterpolator.class);
    }
}
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
public class ScrollXAnimatorTest {

    @Mock private View progressView;

    private ScrollXAnimator scrollXAnimator;

    @Before
    public void setUp() throws Exception {
        scrollXAnimator = new ScrollXAnimator(progressView, 0f, 100f);
    }

    @Test
    public void createAnimatorCreatesAnimatorForProgressView() {
        ObjectAnimator animator = scrollXAnimator.createAnimator(0f, 100f);
        expect(animator.getTarget()).toBe(progressView);
    }

    @Test
    public void createAnimatorCreatesAnimatorForScrollXProperty() {
        ObjectAnimator animator = scrollXAnimator.createAnimator(0f, 100f);
        expect(animator.getPropertyName()).toEqual("scrollX");
    }

}
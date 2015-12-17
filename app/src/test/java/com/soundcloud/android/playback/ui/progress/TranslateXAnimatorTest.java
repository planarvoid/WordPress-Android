package com.soundcloud.android.playback.ui.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.animation.ObjectAnimator;
import android.view.View;

public class TranslateXAnimatorTest extends AndroidUnitTest {

    @Mock private View progressView;

    private TranslateXAnimator translateXAnimator;

    @Before
    public void setUp() throws Exception {
        translateXAnimator = new TranslateXAnimator(progressView, 0f, 100f);
    }

    @Test
    public void createAnimatorCreatesAnimatorForProgressView() {
        ObjectAnimator animator = translateXAnimator.createAnimator(0f, 100f);
        assertThat(animator.getTarget()).isEqualTo(progressView);
    }

    @Test
    public void createAnimatorCreatesAnimatorForTranslateXProperty() {
        ObjectAnimator animator = translateXAnimator.createAnimator(0f, 100f);
        assertThat(animator.getPropertyName()).isEqualTo("translationX");
    }
}

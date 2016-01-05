package com.soundcloud.android.playback.ui.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class TranslateXHelperTest extends AndroidUnitTest {

    @Mock private View progressView;

    @Test
    public void createsTranslateXAnimator() {
        assertThat(new TranslateXHelper(0, 0).createAnimator(progressView, .5f)).isInstanceOf(TranslateXAnimator.class);
    }

    @Test
    public void setsTranslateValueOnView() {
        new TranslateXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView).setTranslationX(100);
    }
}

package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TranslateXHelperTest {

    @Mock private View progressView;

    @Test
    public void createsTranslateXAnimator() {
        expect(new TranslateXHelper(0, 0).createAnimator(progressView, .5f)).toBeInstanceOf(TranslateXAnimator.class);
    }

    @Test
    public void setsTranslateValueOnView() {
        new TranslateXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView).setTranslationX(100);
    }
}
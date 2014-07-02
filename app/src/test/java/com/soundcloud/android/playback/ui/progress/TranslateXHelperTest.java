package com.soundcloud.android.playback.ui.progress;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Build;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class TranslateXHelperTest {

    @Mock private View progressView;

    @Test
    public void createsHoneyCombTranslateAnimator() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB);
        expect(new TranslateXHelper(0, 0).createAnimator(progressView, .5f)).toBeInstanceOf(TranslateXAnimatorHC.class);
    }

    @Test
    public void createsBaseTranslateAnimator() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.GINGERBREAD_MR1);
        final ProgressAnimator animator = new TranslateXHelper(0, 0).createAnimator(progressView, .5f);
        expect(animator).not.toBeInstanceOf(TranslateXAnimatorHC.class);
        expect(animator).toBeInstanceOf(TranslateXAnimator.class);
    }

    @Test
    public void setsTranslateValueOnView() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.HONEYCOMB);
        new TranslateXHelper(0, 0).setValue(progressView, 100f);
        verify(progressView).setTranslationX(100);
    }
}
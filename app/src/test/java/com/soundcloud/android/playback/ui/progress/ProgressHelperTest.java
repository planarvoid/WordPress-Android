package com.soundcloud.android.playback.ui.progress;

import static org.assertj.core.api.Assertions.assertThat;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class ProgressHelperTest {

    private static final int START_POSITION = 0;
    private static final int END_POSITION = 100;

    private TestProgressHelper progressHelper;

    @Mock private ProgressAnimator progressAnimator;
    @Mock private View progressView;

    @Before
    public void setUp() throws Exception {
        progressHelper = new TestProgressHelper(START_POSITION, END_POSITION);
    }

    @Test
    public void setValueFromProportionSetsValueWithProgressView() {
        progressHelper.setValueFromProportion(progressView, .5f);
        assertThat(progressHelper.getProgressView()).isEqualTo(progressView);
    }

    @Test
    public void setValueFromProportionSetsValueProportionalValue() {
        progressHelper.setValueFromProportion(progressView, .5f);
        assertThat(progressHelper.getSetValue()).isEqualTo(50f);
    }

    @Test
    public void getProgressFromPositionReturnsProgress() {
        assertThat(progressHelper.getProgressFromPosition(30)).isEqualTo(.3f);
    }

    private static class TestProgressHelper extends ProgressHelper {

        private View progressViewCapture;
        private float setValue;

        protected TestProgressHelper(int startPosition, int endPosition) {
            super(startPosition, endPosition);
        }

        @Override
        public void setValue(View progressView, float value) {
            progressViewCapture = progressView;
            setValue = value;
        }

        @Nullable
        @Override
        public ProgressAnimator createAnimator(View progressView, float startProportion) {
            return null;
        }

        public View getProgressView() {
            return progressViewCapture;
        }

        public float getSetValue() {
            return setValue;
        }
    }
}

package com.soundcloud.android.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.view.LayoutInflater;

public class SnappedTagViewTest extends AndroidUnitTest {

    private SnappedTagView tagView;

    @Before
    public void setUp() throws Exception {
        tagView = (SnappedTagView) LayoutInflater.from(context()).inflate(R.layout.btn_tag, null);
    }

    @Test
    public void shouldRoundUpWidthMeasurementToGrid() {
        int measuredWidth = tagView.roundUpWidth(17, 100);
        assertThat(measuredWidth).isEqualTo(24);
    }

    @Test
    public void shouldMeasureWidthToMatchParentWhenContentOverflows() {
        int measuredWidth = tagView.roundUpWidth(200, 100);
        assertThat(measuredWidth).isEqualTo(100);
    }

    @Test
    public void shouldMeasureWidthToMatchParentWhenContentIsTheSameSize() {
        int measuredWidth = tagView.roundUpWidth(100, 100);
        assertThat(measuredWidth).isEqualTo(100);
    }

}

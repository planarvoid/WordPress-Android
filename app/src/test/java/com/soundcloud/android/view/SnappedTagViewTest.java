package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.LayoutInflater;

@RunWith(SoundCloudTestRunner.class)
public class SnappedTagViewTest {

    private SnappedTagView tagView;

    @Before
    public void setUp() throws Exception {
        tagView = (SnappedTagView) LayoutInflater.from(Robolectric.application).inflate(R.layout.btn_tag, null);
    }

    @Test
    public void shouldRoundUpWidthMeasurementToGrid() {
        int measuredWidth = tagView.roundUpWidth(17, 100);
        expect(measuredWidth).toEqual(24);
    }

    @Test
    public void shouldMeasureWidthToMatchParentWhenContentOverflows() {
        int measuredWidth = tagView.roundUpWidth(200, 100);
        expect(measuredWidth).toEqual(100);
    }

    @Test
    public void shouldMeasureWidthToMatchParentWhenContentIsTheSameSize() {
        int measuredWidth = tagView.roundUpWidth(100, 100);
        expect(measuredWidth).toEqual(100);
    }

}

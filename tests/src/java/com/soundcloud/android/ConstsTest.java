package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ImageUtils;
import org.junit.Test;

import android.widget.ImageView;

public class ConstsTest {
    @Test
    public void shouldGetMinimumGraphicSize() throws Exception {
        Expect.expect(Consts.GraphicSize.getMinimumSizeFor(99, 101, true)).toEqual(Consts.GraphicSize.T300);
        Expect.expect(Consts.GraphicSize.getMinimumSizeFor(99, 101, false)).toEqual(Consts.GraphicSize.LARGE);

        Expect.expect(Consts.GraphicSize.getMinimumSizeFor(67, 67, true)).toEqual(Consts.GraphicSize.T67);
        Expect.expect(Consts.GraphicSize.getMinimumSizeFor(68, 67, true)).toEqual(Consts.GraphicSize.LARGE);
        Expect.expect(Consts.GraphicSize.getMinimumSizeFor(68, 67, false)).toEqual(Consts.GraphicSize.T67);
    }
}

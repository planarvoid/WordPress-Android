package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class ConstsTest {
    @Test
    public void shouldGetMinimumGraphicSize() throws Exception {
        expect(Consts.GraphicSize.getMinimumSizeFor(99, 101, true)).toEqual(Consts.GraphicSize.T300);
        expect(Consts.GraphicSize.getMinimumSizeFor(99, 101, false)).toEqual(Consts.GraphicSize.LARGE);
        expect(Consts.GraphicSize.getMinimumSizeFor(67, 67, true)).toEqual(Consts.GraphicSize.T67);
        expect(Consts.GraphicSize.getMinimumSizeFor(68, 67, true)).toEqual(Consts.GraphicSize.LARGE);
        expect(Consts.GraphicSize.getMinimumSizeFor(68, 67, false)).toEqual(Consts.GraphicSize.T67);
    }
}

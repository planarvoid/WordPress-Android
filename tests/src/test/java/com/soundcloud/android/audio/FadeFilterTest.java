package com.soundcloud.android.audio;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.record.WavHeaderTest;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.streaming.BufferUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.hardware.Camera;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

@RunWith(DefaultTestRunner.class)
public class FadeFilterTest {

    private static final int SIZE = 100;
    public static final int SIZE_SHORT = SIZE / 2;
    private static final int FADE_SIZE = 20;
    private static final int FADE_SIZE_SHORT = FADE_SIZE / 2;

    @Test
    public void shouldFadeBoth() throws Exception {
        FadeFilter fadeFilter = new FadeFilter(FadeFilter.FADE_TYPE_BOTH, FADE_SIZE);

        ByteBuffer buffer = getFilledBuffer(SIZE);
        ByteBuffer original = BufferUtils.clone(buffer);

        expect(buffer).toEqual(original);
        fadeFilter.apply(buffer, 0, SIZE);
        expect(buffer).not.toEqual(original);

        expectValuesFaded(original.asShortBuffer(), buffer.asShortBuffer(), 0, FADE_SIZE_SHORT);
        expectValuesEqual(original.asShortBuffer(), buffer.asShortBuffer(), FADE_SIZE_SHORT, SIZE_SHORT - FADE_SIZE_SHORT);
        expectValuesFaded(original.asShortBuffer(), buffer.asShortBuffer(), SIZE_SHORT - FADE_SIZE_SHORT + 1, SIZE_SHORT);
    }

    @Test
    public void shouldFadeBeginning() throws Exception {
        FadeFilter fadeFilter = new FadeFilter(FadeFilter.FADE_TYPE_BEGINNING, FADE_SIZE);

        ByteBuffer buffer = getFilledBuffer(SIZE);
        ByteBuffer original = BufferUtils.clone(buffer);

        expect(buffer).toEqual(original);
        fadeFilter.apply(buffer, 0, SIZE);
        expect(buffer).not.toEqual(original);

        expectValuesFaded(original.asShortBuffer(), buffer.asShortBuffer(), 0, FADE_SIZE_SHORT);
        expectValuesEqual(original.asShortBuffer(), buffer.asShortBuffer(), FADE_SIZE_SHORT, SIZE_SHORT - FADE_SIZE_SHORT);
        expectValuesEqual(original.asShortBuffer(), buffer.asShortBuffer(), SIZE_SHORT - FADE_SIZE_SHORT + 1, SIZE_SHORT);
    }

    @Test
    public void shouldFadeEnd() throws Exception {
        FadeFilter fadeFilter = new FadeFilter(FadeFilter.FADE_TYPE_END, FADE_SIZE);

        ByteBuffer buffer = getFilledBuffer(SIZE);
        ByteBuffer original = BufferUtils.clone(buffer);

        expect(buffer).toEqual(original);
        fadeFilter.apply(buffer, 0, SIZE);
        expect(buffer).not.toEqual(original);

        expectValuesEqual(original.asShortBuffer(), buffer.asShortBuffer(), 0, FADE_SIZE_SHORT);
        expectValuesEqual(original.asShortBuffer(), buffer.asShortBuffer(), FADE_SIZE_SHORT, SIZE_SHORT - FADE_SIZE_SHORT);
        expectValuesFaded(original.asShortBuffer(), buffer.asShortBuffer(), SIZE_SHORT - FADE_SIZE_SHORT + 1, SIZE_SHORT);
    }

    private void expectValuesEqual(ShortBuffer original, ShortBuffer faded, int start, int end) {
        for (int i = start; i < end; i++) {
            expect(original.get(i)).toEqual(faded.get(i));
        }
    }

    private void expectValuesFaded(ShortBuffer original, ShortBuffer faded, int start, int end) {
        for (int i = start; i < end; i++) {
            expect(original.get(i)).toBeGreaterThan(faded.get(i));
        }
    }

    private ByteBuffer getFilledBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (int i = 0; i < size / 2; i++) {
            buffer.putShort((short) (Math.random() * 32768));
        }
        buffer.rewind();
        return buffer;
    }

}

package com.soundcloud.android.audio;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.reader.WavReader;
import com.soundcloud.android.record.SoundRecorderTest;
import com.soundcloud.android.record.WavHeaderTest;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;


@RunWith(DefaultTestRunner.class)
public class PlaybackStreamTest {
    PlaybackStream stream;
    private File wavFile = new File(SoundRecorderTest.class.getResource(WavHeaderTest.PCM16_8000_1_WAV).getFile());

    @Before
    public void before() throws IOException {
        stream = new PlaybackStream(new WavReader(wavFile));
    }

    @Test
    public void testSetStartPosition() throws Exception {
        stream.setStartPositionByPercent(.1f, 1);
        expect(stream.getStartPos()).toEqual(555l);
    }

    @Test
    public void testSetEndPosition() throws Exception {
        stream.setEndPositionByPercent(.9f, 1);
        expect(stream.getEndPos()).toEqual(4994l);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidStartPosition() throws Exception {
        stream.setStartPositionByPercent(-1d, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInvalidEndPosition() throws Exception {
        stream.setEndPositionByPercent(2d, 1);
    }

    @Test
    public void testReset() throws Exception {
        stream.setStartPositionByPercent(.1f, 1);
        stream.setEndPositionByPercent(.9f, 1);
        stream.setOptimize(true);
        stream.setFading(true);

        stream.reset();

        expect(stream.getStartPos()).toEqual(0l);
        expect(stream.getEndPos()).toEqual(-1l);
        expect(stream.isOptimized()).toBeFalse();
        expect(stream.isFading()).toBeFalse();
    }

    @Test
    public void testIsModified() throws Exception {
        expect(stream.isModified()).toBeFalse();
        /*
        TODO : re-enable once we add fading to processing

        stream.setOptimize(true);
        expect(stream.isModified()).toBeTrue();

        stream.reset();
        expect(stream.isModified()).toBeFalse();

        stream.setFading(true);
        expect(stream.isModified()).toBeTrue();
        */

        stream.reset();
        expect(stream.isModified()).toBeFalse();

        stream.setStartPositionByPercent(.1f, 1);
        expect(stream.isModified()).toBeTrue();

        stream.reset();
        expect(stream.isModified()).toBeFalse();

        stream.setEndPositionByPercent(.1f, 1);
        expect(stream.isModified()).toBeTrue();

        stream.setEndPositionByPercent(1f, 1);
        expect(stream.isModified()).toBeFalse();
    }

    @Ignore
    @Test
    public void testAvgAmplitude() throws Exception {
        expect(stream.getDuration()).toEqual(5550l);

        final int chunkSize = stream.getConfig().sampleSize * 10; //arbitrary for now
        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);

        long totalRead = 0;
        int n;
        while ((n = stream.readForPlayback(buffer, chunkSize)) > -1) {
            totalRead += n;
            final ShortBuffer sb = buffer.asShortBuffer();
            int read = 0;
            long total = 0;
            while (sb.hasRemaining()) {
                total += Math.abs(sb.get());
                read++;
            }
            System.out.println("avg chunk amplitude is " + (total / read) + " at " + totalRead / ((float) stream.getConfig().bytesPerSecond));
            buffer.clear();
        }

    }
}

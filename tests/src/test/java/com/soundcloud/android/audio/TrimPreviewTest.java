package com.soundcloud.android.audio;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.reader.WavReader;
import com.soundcloud.android.record.WavHeaderTest;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class TrimPreviewTest {
    private File wavFile = new File(getClass().getResource(WavHeaderTest.MONO_TEST_WAV).getFile());
    private static int MAX_PLAYBACK_RATE = 48000;

    @Test
    public void shouldMakeCompleteRealtimePreview() throws Exception {
        final long duration = TrimPreview.MAX_PREVIEW_DURATION;
        final long startPosition = 0;
        final long endPosition = TrimPreview.MAX_PREVIEW_DURATION;

        final TrimPreview trimPreview = getTrimPreview(startPosition, endPosition, duration);

        expect(trimPreview.duration).toEqual(duration);
        expect(trimPreview.startPos).toEqual(startPosition);
        expect(trimPreview.endPos).toEqual(endPosition);
    }

    @Test
    public void shouldMakeCompleteFastPreview() throws Exception {
        final long duration = TrimPreview.MAX_PREVIEW_DURATION;
        final double seconds = duration / 1000d;

        final long startPosition = 0;
        final long endPosition = AudioConfig.PCM16_44100_1.bytesToMs((long) (MAX_PLAYBACK_RATE * AudioConfig.PCM16_44100_1.sampleSize * seconds));

        final TrimPreview trimPreview = getTrimPreview(startPosition, endPosition, duration);

        expect(trimPreview.duration).toEqual(duration);
        expect(trimPreview.startPos).toEqual(startPosition);
        expect(trimPreview.endPos).toEqual(endPosition);
    }

    @Test
    public void shouldMakeShortenedFastPreview() throws Exception {
        final long duration = TrimPreview.MAX_PREVIEW_DURATION;
        final double seconds = duration / 1000d;

        final long startPosition = 0;
        final long endPosition = AudioConfig.PCM16_44100_1.bytesToMs((long) (MAX_PLAYBACK_RATE * AudioConfig.PCM16_44100_1.sampleSize * seconds)) + 100;

        final TrimPreview trimPreview = getTrimPreview(startPosition, endPosition, duration);

        expect(trimPreview.duration).toEqual(duration);
        expect(trimPreview.startPos).toBeGreaterThan(startPosition);
        expect(trimPreview.endPos).toEqual(endPosition);
    }

    @Test
    public void shouldMakeCompleteSlowPreview() throws Exception {
        final long duration = TrimPreview.MAX_PREVIEW_DURATION;
        final double seconds = duration / 1000d;

        final long startPosition = 0;
        final long endPosition = AudioConfig.PCM16_8000_1.bytesToMs((long) (AudioConfig.PCM16_8000_1.bytesPerSecond * seconds));

        final TrimPreview trimPreview = getTrimPreview(startPosition, endPosition, duration);

        expect(trimPreview.duration).toEqual(duration);
        expect(trimPreview.startPos).toEqual(startPosition);
        expect(trimPreview.endPos).toEqual(endPosition);
    }

    @Test
    public void shouldTruncateSlowPreview() throws Exception {
        final int playbackRate = 1000;
        final long duration = TrimPreview.MAX_PREVIEW_DURATION + 100;
        final double seconds = duration / 1000d;

        final long startPosition = 0;
        final long endPosition = AudioConfig.PCM16_8000_1.bytesToMs((long) (AudioConfig.PCM16_8000_1.bytesPerSecond * seconds));

        final TrimPreview trimPreview = getTrimPreview(startPosition, endPosition, duration);

        expect(trimPreview.duration).toBeLessThan(duration);
        expect(trimPreview.startPos).toBeGreaterThan(startPosition);
        expect(trimPreview.endPos).toEqual(endPosition);
    }

    private TrimPreview getTrimPreview(long startPosition, long endPosition, long duration) throws IOException {
        return new TrimPreview(
                new PlaybackStream(new WavReader(wavFile)),
                startPosition,
                endPosition,
                duration,
                MAX_PLAYBACK_RATE
        );
    }
}

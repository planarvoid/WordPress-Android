package com.soundcloud.android.record;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.audio.AudioConfig;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.TrimPreview;
import com.soundcloud.android.audio.WavFile;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class TrimPreviewTest {
    private File wavFile = new File(getClass().getResource(WavHeaderTest.MONO_TEST_WAV).getFile());

    @Test
    public void shouldMakeCompletePreview() throws Exception {
        expect(new TrimPreview(new PlaybackStream(new WavFile(wavFile)), 0, 44100 * AudioConfig.PCM16_44100_1.sampleSize, TrimPreview.MAX_PREVIEW_DURATION).duration).toEqual(TrimPreview.MAX_PREVIEW_DURATION);
    }
}

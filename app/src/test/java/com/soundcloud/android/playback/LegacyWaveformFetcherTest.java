package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(DefaultTestRunner.class)
public class LegacyWaveformFetcherTest {
    @Test
    public void shouldFetchAndDeserializeData() throws Exception {
        LegacyWaveformFetcher fetcher = new LegacyWaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://wis.sndcdn.com/H9uGzKOYK5Ph_m.png")).get();

        expect(data).not.toBeNull();
        expect(data.maxAmplitude).toEqual(140);
        expect(data.samples.length).toEqual(1800);
    }

    @Test
    public void shouldHandleInvalidData() throws Exception {
        LegacyWaveformFetcher fetcher = new LegacyWaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://waveforms.soundcloud.com/H9uGzKOYK5Ph_m.png")).get();
        expect(data).toBeNull();
    }

    @Test
    public void shouldHandleMissingWaveform() throws Exception {
        LegacyWaveformFetcher fetcher = new LegacyWaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://wis.sndcdn.com/i_am_not_here.png")).get();
        expect(data).toBeNull();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfNoParametersSupplied() throws Exception {
        new LegacyWaveformFetcher().execute().get();
    }
}

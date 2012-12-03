package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(DefaultTestRunner.class)
public class WaveformFetcherTest {
    @Test
    public void shouldFetchAndDeserializeData() throws Exception {
        WaveformFetcher fetcher = new WaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://wis.sndcdn.com/H9uGzKOYK5Ph_m.png")).get();

        expect(data).not.toBeNull();
        expect(data.maxAmplitude).toEqual(140);
        expect(data.samples.length).toEqual(1800);
    }

    @Test
    public void shouldHandleInvalidData() throws Exception {
        WaveformFetcher fetcher = new WaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://waveforms.soundcloud.com/H9uGzKOYK5Ph_m.png")).get();
        expect(data).toBeNull();
    }

    @Test
    public void shouldHandleMissingWaveform() throws Exception {
        WaveformFetcher fetcher = new WaveformFetcher();
        WaveformData data = fetcher.execute(new URL("http://wis.sndcdn.com/i_am_not_here.png")).get();
        expect(data).toBeNull();
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowIfNoParametersSupplied() throws Exception {
        new WaveformFetcher().execute().get();
    }
}

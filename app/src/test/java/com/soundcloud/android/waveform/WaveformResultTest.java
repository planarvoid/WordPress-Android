package com.soundcloud.android.waveform;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class WaveformResultTest {

    @Mock
    private WaveformData waveformData;

    @Test
    public void getWaveformDataReturnsWaveformDataFromConstructor() throws Exception {
        expect(WaveformResult.fromCache(waveformData).getWaveformData()).toBe(waveformData);
    }

    @Test
    public void isFromCacheReturnsTrueForResultBuiltFromCacheConstructor() throws Exception {
        expect(WaveformResult.fromCache(waveformData).isFromCache()).toBeTrue();
    }

    @Test
    public void isFromCacheReturnsFalseForResultBuiltFromNetworkConstructor() throws Exception {
        expect(WaveformResult.fromNetwork(waveformData).isFromCache()).toBeFalse();
    }

    @Test
    public void isFromCacheReturnsFalseForResultBuiltFromErrorConstructor() throws Exception {
        expect(WaveformResult.fromError(waveformData).isFromCache()).toBeFalse();
    }
}
package com.soundcloud.android.waveform;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.support.v4.util.LruCache;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class WaveformOperationsTest {

    private final Urn trackUrn = Urn.forTrack(1L);
    private final String waveformUrl = "http://waveform.png";

    private WaveformOperations waveformOperations;

    @Mock
    private WaveformFetcher waveformFetcher;
    @Mock
    private WaveformData waveformData;
    @Mock
    private Observer<WaveformData> observer;
    @Mock
    private WaveformView waveformView;

    @Before
    public void setUp() throws Exception {
        LruCache<Urn, WaveformData> waveformCache = new LruCache<>(1);
        waveformOperations = new WaveformOperations(waveformCache, waveformFetcher);
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.<WaveformData>empty());
    }

    @Test
    public void emitsWaveformDataInResultFromWaveformFetcher() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        final WaveformData actual = waveformOperations.waveformDataFor(trackUrn, waveformUrl).toBlocking().lastOrDefault(null);
        expect(actual).toBe(waveformData);
    }

    @Test
    public void emitsDefaultWaveformFromWaveformFetcherOnError() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.<WaveformData>error(new IOException("WaveformError")));
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.just(waveformData));

        final WaveformData actual = waveformOperations.waveformDataFor(trackUrn, waveformUrl).toBlocking().lastOrDefault(null);
        expect(actual).toBe(waveformData);
    }

    @Test
    public void emitsCachedWaveformIfAlreadyDownloaded() {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        Observable<WaveformData> waveformObservable = waveformOperations.waveformDataFor(trackUrn, waveformUrl);
        WaveformData unCachedResult = waveformObservable.toBlocking().lastOrDefault(null);
        WaveformData cachedResult = waveformObservable.toBlocking().lastOrDefault(null);

        verify(waveformFetcher, times(1)).fetch(waveformUrl);
        expect(unCachedResult).toBe(waveformData);
        expect(cachedResult).toBe(waveformData);
    }
}
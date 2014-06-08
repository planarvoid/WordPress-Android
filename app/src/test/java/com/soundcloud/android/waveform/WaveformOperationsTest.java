package com.soundcloud.android.waveform;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.support.v4.util.LruCache;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class WaveformOperationsTest {

    private static final TrackUrn trackUrn = Urn.forTrack(1L);
    private static final String waveformUrl = "http://waveform.png";

    private WaveformOperations waveformOperations;
    private LruCache<TrackUrn, WaveformData> waveformCache;

    @Mock
    private WaveformFetcher waveformFetcher;
    @Mock
    private WaveformData waveformData;
    @Mock
    private Observer<WaveformResult> observer;
    @Mock
    private Track track;
    @Mock
    private WaveformView waveformView;

    @Before
    public void setUp() throws Exception {
        waveformCache = new LruCache<TrackUrn, WaveformData>(1);
        waveformOperations = new WaveformOperations(waveformCache, waveformFetcher);
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.<WaveformData>empty());
        when(track.getUrn()).thenReturn(trackUrn);
        when(track.getWaveformUrl()).thenReturn(waveformUrl);
    }

    @Test
    public void emitsWaveformDataInResultFromWaveformFetcher() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.getWaveformData()).toBe(waveformData);
    }

    @Test
    public void emitsWaveformResultWithIsFromCacheFalseFromWaveformFetcher() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.isFromCache()).toBeFalse();
    }

    @Test
    public void emitsWaveformDataFromCacheOnConsecutiveFetch() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        waveformOperations.waveformFor(track).subscribe(observer);

        waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        verify(waveformFetcher).fetch(waveformUrl); // only happens once, second time is cached
    }

    @Test
    public void emitsWaveformResultWithWaveformDataFromCacheOnConsecutiveFetch() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        waveformOperations.waveformFor(track).subscribe(observer);

        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.getWaveformData()).toBe(waveformData);
    }

    @Test
    public void emitsWaveformResultWithIsFromCacheTrueOnConsecutiveFetch() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        waveformOperations.waveformFor(track).subscribe(observer);

        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.isFromCache()).toBeTrue();
    }

    @Test
    public void emitsDefaultWaveformFromWaveformFetcherOnError() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.<WaveformData>error(new IOException("WaveformError")));
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.just(waveformData));

        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.getWaveformData()).toBe(waveformData);
    }

    @Test
    public void emitsResultWithIsFromCacheFalseOnError() throws Exception {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.<WaveformData>error(new IOException("WaveformError")));
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.just(waveformData));

        final WaveformResult actual = waveformOperations.waveformFor(track).toBlockingObservable().lastOrDefault(null);
        expect(actual.isFromCache()).toBeFalse();
    }

    @Test
    public void displaySetsWaveformDataOnWaveformViewFromWaveformFetcher() {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        waveformOperations.display(trackUrn, waveformUrl, waveformView);

        verify(waveformView).setWaveform(waveformData);
    }

    @Test
    public void displaySetsWaveformFromCacheOnConsecutiveFetch() {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));
        waveformOperations.display(trackUrn, waveformUrl, waveformView);

        waveformOperations.display(trackUrn, waveformUrl, waveformView);
        verify(waveformFetcher).fetch(waveformUrl);
    }

    @Test
    public void displayWithDifferentTrackUrnIsNotOverriddenByPreviousLoad() {
        // first waveform request, capture subscriber
        TestObservables.MockObservable<WaveformData> observable = TestObservables.endlessMockObservableFromSubscription(Subscriptions.empty());
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(observable);
        waveformOperations.display(trackUrn, waveformUrl, waveformView);

        // second waveform request return immediately
        final String secondWaveformUrl = "http://diffwaveformurl.png";
        final WaveformData waveformDataForSecondRequest = Mockito.mock(WaveformData.class);
        when(waveformFetcher.fetch(secondWaveformUrl)).thenReturn(Observable.just(waveformDataForSecondRequest));
        waveformOperations.display(Urn.forTrack(987L), secondWaveformUrl, waveformView);

        observable.subscribers().get(0).onNext(waveformData);

        verify(waveformView).setWaveform(waveformDataForSecondRequest);
        verify(waveformView, never()).setWaveform(waveformData);
    }

}
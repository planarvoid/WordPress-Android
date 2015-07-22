package com.soundcloud.android.waveform;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.ChangeResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class WaveformOperationsTest {

    private final Urn trackUrn = Urn.forTrack(1L);
    private final String waveformUrl = "http://waveform.png";

    private WaveformOperations waveformOperations;
    private WaveformData waveformData;
    private TestObserver<WaveformData> observer;

    @Mock private WaveformStorage waveFormStorage;
    @Mock private WaveformFetcher waveformFetcher;
    @Mock private WaveformView waveformView;
    @Mock private ClearTableCommand clearTableCommand;

    @Before
    public void setUp() throws Exception {
        observer = new TestObserver<>();
        waveformData = new WaveformData(12, new int[]{12, 123, 124});
        waveformOperations = new WaveformOperations(
                waveformFetcher, waveFormStorage,
                clearTableCommand, Schedulers.immediate());

        when(waveformFetcher.fetchDefault()).thenReturn(Observable.<WaveformData>empty());
        when(waveFormStorage.load(trackUrn)).thenReturn(Observable.<WaveformData>empty());
        when(waveFormStorage.store(trackUrn, waveformData)).thenReturn(Observable.<ChangeResult>empty());
    }

    @Test
    public void emitsWaveformDataInResultFromWaveformFetcher() {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsDefaultWaveformFromWaveformFetcherOnError() {
        when(waveformFetcher.fetch(waveformUrl))
                .thenReturn(Observable.<WaveformData>error(new IOException("WaveformError")));
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsDefaultWaveformOnNullWaveformUrl() {
        when(waveformFetcher.fetch(null))
                .thenReturn(Observable.<WaveformData>error(new IllegalArgumentException("null waveform")));
        when(waveformFetcher.fetchDefault()).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, null).subscribe(observer);

        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsCachedWaveformIfAlreadyDownloaded() {
        when(waveformFetcher.fetch(waveformUrl)).thenReturn(Observable.just(WaveformData.EMPTY));
        when(waveFormStorage.load(trackUrn)).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);

        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void clearWaveformCallsClearWaveformTableCommand() {
        waveformOperations.clearWaveforms();

        verify(clearTableCommand).call(Table.Waveforms);
    }
}

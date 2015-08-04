package com.soundcloud.android.waveform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.ChangeResult;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class WaveformOperationsTest extends AndroidUnitTest {

    private final Urn trackUrn = Urn.forTrack(1L);
    private final String waveformUrl = "http://waveform.png";

    private WaveformOperations waveformOperations;
    private WaveformData waveformData;
    private TestObserver<WaveformData> observer;

    @Mock private WaveformStorage waveFormStorage;
    @Mock private WaveformFetchCommand waveformFetchCommand;
    @Mock private WaveformView waveformView;
    @Mock private ClearTableCommand clearTableCommand;
    @Mock private WaveformParser waveformParser;

    @Before
    public void setUp() throws Exception {
        observer = new TestObserver<>();
        waveformData = new WaveformData(12, new int[]{12, 123, 124});
        waveformOperations = new WaveformOperations(
                context(), waveformFetchCommand, waveFormStorage, waveformParser,
                clearTableCommand, Schedulers.immediate());

        when(waveFormStorage.load(trackUrn)).thenReturn(Observable.<WaveformData>empty());
        when(waveFormStorage.store(trackUrn, waveformData)).thenReturn(Observable.<ChangeResult>empty());
    }

    @Test
    public void emitsWaveformDataInResultFromWaveformFetcher() {
        when(waveformFetchCommand.toObservable(waveformUrl)).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsDefaultWaveformFromWaveformFetcherOnError() throws IOException, JSONException {
        when(waveformFetchCommand.toObservable(waveformUrl))
                .thenReturn(Observable.<WaveformData>error(new IOException("WaveformError")));
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsDefaultWaveformOnNullWaveformUrl() throws IOException, JSONException {
        when(waveformFetchCommand.toObservable(null))
                .thenReturn(Observable.<WaveformData>error(new IllegalArgumentException("null waveform")));
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.waveformDataFor(trackUrn, null).subscribe(observer);

        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void emitsCachedWaveformIfAlreadyDownloaded() {
        when(waveformFetchCommand.toObservable(waveformUrl)).thenReturn(Observable.just(WaveformData.EMPTY));
        when(waveFormStorage.load(trackUrn)).thenReturn(Observable.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);

        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void clearWaveformCallsClearWaveformTableCommand() {
        waveformOperations.clearWaveforms();

        verify(clearTableCommand).call(Table.Waveforms);
    }

    @Test
    public void fetchDefaultEmitsWaveform() throws Exception {
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.fetchDefault().subscribe(observer);

        observer.assertReceivedOnNext(Collections.singletonList(waveformData));
    }

    @Test
    public void fetchDefaultEmitsError() throws Exception {
        Throwable exception = new IOException("asset read error");
        when(waveformParser.parse(any(InputStream.class))).thenThrow(exception);

        waveformOperations.fetchDefault().subscribe(observer);

        assertThat(observer.getOnNextEvents()).isEmpty();
        assertThat(observer.getOnErrorEvents()).containsExactly(exception);
    }
}

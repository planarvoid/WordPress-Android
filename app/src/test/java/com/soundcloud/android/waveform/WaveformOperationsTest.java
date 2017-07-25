package com.soundcloud.android.waveform;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;

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
                Schedulers.trampoline());

        when(waveFormStorage.waveformData(trackUrn)).thenReturn(Maybe.empty());
    }

    @Test
    public void emitsWaveformDataInResultFromWaveformFetcher() {
        when(waveformFetchCommand.toSingle(waveformUrl)).thenReturn(Single.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertValue(waveformData);
    }

    @Test
    public void emitsDefaultWaveformFromWaveformFetcherOnError() throws IOException, JSONException {
        when(waveformFetchCommand.toSingle(waveformUrl))
                .thenReturn(Single.error(new IOException("WaveformError")));
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);
        observer.assertValue(waveformData);
    }

    @Test
    public void emitsDefaultWaveformOnNullWaveformUrl() throws IOException, JSONException {
        when(waveformFetchCommand.toSingle(null))
                .thenReturn(Single.error(new IllegalArgumentException("null waveform")));
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.waveformDataFor(trackUrn, null).subscribe(observer);

        observer.assertValue(waveformData);
    }

    @Test
    public void emitsCachedWaveformIfAlreadyDownloaded() {
        when(waveformFetchCommand.toSingle(waveformUrl)).thenReturn(Single.just(WaveformData.EMPTY));
        when(waveFormStorage.waveformData(trackUrn)).thenReturn(Maybe.just(waveformData));

        waveformOperations.waveformDataFor(trackUrn, waveformUrl).subscribe(observer);

        observer.assertValue(waveformData);
    }

    @Test
    public void clearWaveformCallsClearOnStorage() {
        waveformOperations.clearWaveforms();

        verify(waveFormStorage).clear();
    }

    @Test
    public void fetchDefaultEmitsWaveform() throws Exception {
        when(waveformParser.parse(any(InputStream.class))).thenReturn(waveformData);

        waveformOperations.fetchDefault().subscribe(observer);

        observer.assertValue(waveformData);
    }

    @Test
    public void fetchDefaultEmitsError() throws Exception {
        Throwable exception = new IOException("asset read error");
        when(waveformParser.parse(any(InputStream.class))).thenThrow(exception);

        waveformOperations.fetchDefault().subscribe(observer);

        observer.assertNoValues();
        observer.assertError(exception);
    }
}

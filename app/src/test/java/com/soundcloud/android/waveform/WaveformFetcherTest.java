package com.soundcloud.android.waveform;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

@RunWith(SoundCloudTestRunner.class)
public class WaveformFetcherTest {

    private static final String WAVEFORM_URL = "https://w1.sndcdn.com/H9uGzKOYK5Ph_m.png";
    private static final String TRANSFORMED_WAVEFORM_URL = "http://wis.sndcdn.com/H9uGzKOYK5Ph_m.png";
    private static final String VALID_WAVEFORM_DATA = "{\"width\": 6,\"height\": 140,\"samples\": [3,6,8,24,63,140]}";
    private static final String EMPTY_WAVEFORM_DATA = "{\"width\": 0,\"height\": 0,\"samples\": []}";
    private static final String INVALID_WIDTH_WAVEFORM_DATA = "{\"width\": 5,\"height\": 140,\"samples\": [3,6,8,24,63,140]}";

    private WaveformFetcher fetcher;

    @Mock
    private Context context;
    @Mock
    private AssetManager assets;
    @Mock
    private WaveformConnectionFactory waveformConnectionFactory;
    @Mock
    private HttpURLConnection urlConnection;
    @Mock
    private Observer<WaveformData> observer;

    @Before
    public void setUp() throws Exception {
        fetcher = new WaveformFetcher(Schedulers.immediate(), context, waveformConnectionFactory);
        when(waveformConnectionFactory.create(TRANSFORMED_WAVEFORM_URL)).thenReturn(urlConnection);
        when(context.getAssets()).thenReturn(assets);
    }

    @Test
    public void invalidResponseCodeEmitsIOException() throws Exception {
        when(urlConnection.getResponseCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        fetcher.fetch(WAVEFORM_URL).subscribe(observer);
        verify(observer).onError(any(IOException.class));
        verify(observer, Mockito.never()).onNext(any(WaveformData.class));
    }

    @Test
    public void emittedWaveformIsNotNull() throws Exception {
        setupValidWaveformResponse();

        WaveformData waveformData = fetcher.fetch(WAVEFORM_URL).toBlockingObservable().lastOrDefault(null);
        expect(waveformData).not.toBeNull();
    }

    @Test
    public void emittedWaveformSetsMaxAmplitude() throws Exception {
        setupValidWaveformResponse();

        WaveformData waveformData = fetcher.fetch(WAVEFORM_URL).toBlockingObservable().lastOrDefault(null);
        expect(waveformData.maxAmplitude).toEqual(140);
    }

    @Test
    public void emittedWaveformSetsSamples() throws Exception {
        setupValidWaveformResponse();

        WaveformData waveformData = fetcher.fetch(WAVEFORM_URL).toBlockingObservable().lastOrDefault(null);
        assertArrayEquals(waveformData.samples, new int[]{3, 6, 8, 24, 63, 140});
    }

    @Test
    public void invalidWidthOnWaveformDataCausesIOException() throws Exception {
        setupWaveformResponse(INVALID_WIDTH_WAVEFORM_DATA);

        fetcher.fetch(WAVEFORM_URL).subscribe(observer);
        verify(observer).onError(any(IOException.class));
        verify(observer, Mockito.never()).onNext(any(WaveformData.class));
    }

    @Test
    public void emptyWaveformDataCausesIOException() throws Exception {
        setupWaveformResponse(EMPTY_WAVEFORM_DATA);

        fetcher.fetch(WAVEFORM_URL).subscribe(observer);
        verify(observer).onError(any(IOException.class));
        verify(observer, Mockito.never()).onNext(any(WaveformData.class));
    }

    @Test
    public void fetchDefaultEmitsWaveform() throws Exception {
        final byte[] waveformBytes = VALID_WAVEFORM_DATA.getBytes(Charsets.UTF_8.name());
        when(assets.open("default_waveform.json")).thenReturn(new ByteArrayInputStream(waveformBytes));

        expect(fetcher.fetchDefault().toBlockingObservable().lastOrDefault(null)).not.toBe(null);
    }

    @Test
    public void fetchDefaultEmitsError() throws Exception {
        when(assets.open(anyString())).thenThrow(new IOException("asset read error"));

        fetcher.fetchDefault().subscribe(observer);
        verify(observer).onError(any(IOException.class));
        verify(observer, Mockito.never()).onNext(any(WaveformData.class));
    }

    private void setupValidWaveformResponse() throws IOException {
        final String validWaveformData = VALID_WAVEFORM_DATA;
        setupWaveformResponse(validWaveformData);
    }

    private void setupWaveformResponse(String waveformData) throws IOException {
        final byte[] waveformBytes = waveformData.getBytes(Charsets.UTF_8.name());
        when(urlConnection.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(urlConnection.getInputStream()).thenReturn(new ByteArrayInputStream(waveformBytes));
    }
}

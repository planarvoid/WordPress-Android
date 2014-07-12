package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.propeller.TxnResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class AdsOperationsTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    private AdsOperations adsOperations;
    private AudioAd audioAd;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private TrackWriteStorage trackWriteStorage;


    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(rxHttpClient, trackWriteStorage);
        audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
    }

    @Test
    public void audioAdReturnsAudioAdFromMobileApi() throws Exception {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), TRACK_URN.toEncodedString());
        when(rxHttpClient.<AudioAd>fetchModels(argThat(isMobileApiRequestTo("GET", endpoint)))).thenReturn(Observable.just(audioAd));
        when(trackWriteStorage.storeTrackAsync(audioAd.getApiTrack())).thenReturn(Observable.<TxnResult>empty());

        expect(adsOperations.audioAd(TRACK_URN).toBlocking().first()).toBe(audioAd);
    }

    @Test
    public void audioAdWritesEmbeddedTrackToStorage() throws Exception {
        when(rxHttpClient.<AudioAd>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(audioAd));
        when(trackWriteStorage.storeTrackAsync(audioAd.getApiTrack())).thenReturn(Observable.<TxnResult>empty());

        adsOperations.audioAd(TRACK_URN).subscribe();

        verify(trackWriteStorage).storeTrackAsync(audioAd.getApiTrack());
    }
}
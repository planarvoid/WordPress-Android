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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;

import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class AdsOperationsTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    private AdsOperations adsOperations;
    private AudioAd audioAd;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private TrackWriteStorage trackWriteStorage;
    @Mock private DeviceHelper deviceHelper;
    @Mock private PlayQueueManager playQueueManager;
    private PlaySessionSource playSessionSource;


    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(rxHttpClient, trackWriteStorage, deviceHelper, playQueueManager);
        audioAd = ModelFixtures.create(AudioAd.class);
        playSessionSource = new PlaySessionSource("origin");
        playSessionSource.setExploreVersion("1.0");
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

    @Test
    public void audioAdRequestIncludesUniqueDeviceId() {
        final ArgumentCaptor<APIRequest> captor = ArgumentCaptor.forClass(APIRequest.class);
        when(rxHttpClient.<AudioAd>fetchModels(captor.capture())).thenReturn(Observable.just(audioAd));
        when(trackWriteStorage.storeTrackAsync(audioAd.getApiTrack())).thenReturn(Observable.<TxnResult>empty());
        when(deviceHelper.getUniqueDeviceID()).thenReturn("is google watching?");

        adsOperations.audioAd(TRACK_URN).subscribe();

        final APIRequest apiRequest = captor.getValue();
        Map<String, String> headers = apiRequest.getHeaders();
        expect(headers.containsKey("SC-UDID")).toBeTrue();
        expect(headers.get("SC-UDID")).toEqual("is google watching?");
    }


    @Test
    public void shouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getMetaDataAt(0)).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(123L)));

        expect(adsOperations.isAudioAdAtPosition(0)).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfCurrentItemIsNotAudioAd() {
        when(playQueueManager.getMetaDataAt(1)).thenReturn(PropertySet.create());
        expect(adsOperations.isAudioAdAtPosition(1)).toBeFalse();
    }

}
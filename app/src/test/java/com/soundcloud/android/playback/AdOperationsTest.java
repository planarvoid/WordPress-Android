package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.ads.AudioAd;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.BulkStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.Collection;

@RunWith(SoundCloudTestRunner.class)
public class AdOperationsTest {

    private static final TrackUrn TRACK_URN = Urn.forTrack(123L);

    private AdOperations adOperations;
    private AudioAd audioAd;

    @Mock private RxHttpClient rxHttpClient;
    @Mock private BulkStorage bulkStorage;


    @Before
    public void setUp() throws Exception {
        adOperations = new AdOperations(rxHttpClient, bulkStorage);
        audioAd = TestHelper.getModelFactory().createModel(AudioAd.class);
    }

    @Test
    public void audioAdReturnsAudioAdFromMobileApi() throws Exception {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), TRACK_URN.toEncodedString());
        when(rxHttpClient.<AudioAd>fetchModels(argThat(isMobileApiRequestTo("GET", endpoint)))).thenReturn(Observable.just(audioAd));
        when(bulkStorage.bulkInsertAsync(any(Collection.class))).thenReturn(Observable.<Collection>empty());

        expect(adOperations.getAudioAd(TRACK_URN).toBlocking().first()).toBe(audioAd);
    }

    @Test
    public void audioAdWritesEmbeddedTrackToStorage() throws Exception {
        when(rxHttpClient.<AudioAd>fetchModels(any(APIRequest.class))).thenReturn(Observable.just(audioAd));
        when(bulkStorage.bulkInsertAsync(any(Collection.class))).thenReturn(Observable.<Collection>empty());

        adOperations.getAudioAd(TRACK_URN).subscribe();

        verify(bulkStorage).bulkInsertAsync(eq(Lists.newArrayList(new Track(audioAd.getTrackSummary()))));
    }
}
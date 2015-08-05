package com.soundcloud.android.ads;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;

public class AdsOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private AdsOperations adsOperations;
    private ApiAdsForTrack fullAdsForTrack;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private PlayQueueManager playQueueManager;
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(storeTracksCommand, playQueueManager, apiClientRx, Schedulers.immediate());
        fullAdsForTrack = AdFixtures.fullAdsForTrack();
        playSessionSource = PlaySessionSource.forExplore("origin", "1.0");
    }

    @Test
    public void audioAdReturnsAudioAdFromMobileApi() throws Exception {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), TRACK_URN.toEncodedString());
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", endpoint)), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        assertThat(adsOperations.ads(TRACK_URN).toBlocking().first()).isEqualTo(fullAdsForTrack);
    }

    @Test
    public void audioAdWritesEmbeddedTrackToStorage() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        adsOperations.ads(TRACK_URN).subscribe();

        verify(storeTracksCommand).call(Arrays.asList(fullAdsForTrack.audioAd().getApiTrack()));
    }

    @Test
    public void shouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getMetaDataAt(0)).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(123L)));

        assertThat(adsOperations.isAudioAdAtPosition(0)).isTrue();
    }

    @Test
    public void shouldReturnFalseIfCurrentItemIsNotAudioAd() {
        when(playQueueManager.getMetaDataAt(1)).thenReturn(PropertySet.create());
        assertThat(adsOperations.isAudioAdAtPosition(1)).isFalse();
    }

    @Test
    public void applyAdMergesInterstitialWhenNoAudioAdIsAvailable() throws Exception {
        final ApiAdsForTrack adsWithOnlyInterstitial = AdFixtures.interstitial();
        adsOperations.applyAdToTrack(TRACK_URN, adsWithOnlyInterstitial);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(TRACK_URN);
        final PropertySet expectedProperties = adsWithOnlyInterstitial.interstitialAd().toPropertySet();
        expectedProperties.put(AdOverlayProperty.META_AD_DISMISSED, false);
        expectedProperties.put(TrackProperty.URN, TRACK_URN);
        assertThat(playQueue.getMetaData(0)).isEqualTo(expectedProperties);
    }

    @Test
    public void applyInterstitialMergesInterstitial() throws Exception {
        final ApiAdsForTrack ads = AdFixtures.fullAdsForTrack();
        adsOperations.applyInterstitialToTrack(TRACK_URN, ads);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(TRACK_URN);
        final PropertySet expectedProperties = ads.interstitialAd().toPropertySet();
        expectedProperties.put(AdOverlayProperty.META_AD_DISMISSED, false);
        expectedProperties.put(TrackProperty.URN, TRACK_URN);
        assertThat(playQueue.getMetaData(0)).isEqualTo(expectedProperties);
    }

    @Test
    public void applyAdInsertsAudioAdWhenOnlyAudioAdIsAvailable() throws Exception {
        ApiAdsForTrack adsWithOnlyAudioAd = AdFixtures.audioAd();
        adsOperations.applyAdToTrack(TRACK_URN, adsWithOnlyAudioAd);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(adsWithOnlyAudioAd.audioAd().getApiTrack().getUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(TRACK_URN);
        assertThat(playQueue.getMetaData(1)).isEqualTo(adsWithOnlyAudioAd.audioAd().getLeaveBehind()
                .toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(LeaveBehindProperty.AUDIO_AD_TRACK_URN, adsWithOnlyAudioAd.audioAd().getApiTrack().getUrn()));
    }

    @Test
    public void applyAdPrefersAudioAdWhenBothAreAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = AdFixtures.fullAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, fullAdsForTrack);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        captor1.getValue().execute(playQueue);
        captor2.getValue().execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(fullAdsForTrack.audioAd().getApiTrack().getUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(TRACK_URN);
        assertThat(playQueue.getMetaData(1)).isEqualTo(fullAdsForTrack.audioAd().getLeaveBehind()
                .toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(LeaveBehindProperty.AUDIO_AD_TRACK_URN, fullAdsForTrack.audioAd().getApiTrack().getUrn()));
    }

    @Test
    public void applyAdIsNoOpIfNoAdsAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = new ApiAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, fullAdsForTrack);

        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);

        verify(playQueueManager, never()).performPlayQueueUpdateOperations(any(PlayQueueManager.QueueUpdateOperation.class));
        assertThat(playQueue.getUrn(0)).isEqualTo(TRACK_URN);
    }

    @Test
    public void insertAudioAdShouldInsertAudioAd() throws Exception {
        ApiAudioAd audioAdWithoutLeaveBehind = Mockito.mock(ApiAudioAd.class);
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        when(audioAdWithoutLeaveBehind.hasApiLeaveBehind()).thenReturn(false);
        when(audioAdWithoutLeaveBehind.getApiTrack()).thenReturn(apiTrack);
        when(audioAdWithoutLeaveBehind.toPropertySet()).thenReturn(PropertySet.create());

        adsOperations.applyAdToTrack(TRACK_URN, new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(audioAdWithoutLeaveBehind))));

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(audioAdWithoutLeaveBehind.getApiTrack().getUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(TRACK_URN);
    }

    @Test
    public void insertAudioAdShouldInsertAudioAdAndLeaveBehind() throws Exception {
        ApiAdsForTrack noInterstitial = new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(ModelFixtures.create(ApiAudioAd.class))));
        adsOperations.applyAdToTrack(TRACK_URN, noInterstitial);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(noInterstitial.audioAd().getApiTrack().getUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(TRACK_URN);
        assertThat(playQueue.getMetaData(1)).isEqualTo(noInterstitial.audioAd().getLeaveBehind()
                .toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(LeaveBehindProperty.AUDIO_AD_TRACK_URN, noInterstitial.audioAd().getApiTrack().getUrn()));
    }

    @Test
    public void applyAdInsertsAudioAdWithNoLeaveBehindWhenNoInterstitialOrLeaveBehindIsAvailable() throws Exception {
        final ApiAudioAd apiAudioAd = Mockito.mock(ApiAudioAd.class);
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

        when(apiAudioAd.hasApiLeaveBehind()).thenReturn(false);
        when(apiAudioAd.toPropertySet()).thenReturn(PropertySet.create());
        when(apiAudioAd.getApiTrack()).thenReturn(apiTrack);
        adsOperations.applyAdToTrack(TRACK_URN, new ApiAdsForTrack(Arrays.asList(new ApiAdWrapper(apiAudioAd))));

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN), playSessionSource);
        value1.execute(playQueue);

        assertThat(playQueue.getUrn(0)).isEqualTo(apiTrack.getUrn());
        assertThat(playQueue.getUrn(1)).isEqualTo(TRACK_URN);
        assertThat(playQueue.getMetaData(1)).isEmpty();
    }
}

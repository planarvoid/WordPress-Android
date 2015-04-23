package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
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
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class AdsOperationsTest {

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
        playSessionSource = new PlaySessionSource("origin");
        playSessionSource.setExploreVersion("1.0");
    }

    @Test
    public void audioAdReturnsAudioAdFromMobileApi() throws Exception {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), TRACK_URN.toEncodedString());
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", endpoint)), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        expect(adsOperations.ads(TRACK_URN).toBlocking().first()).toBe(fullAdsForTrack);
    }

    @Test
    public void audioAdWritesEmbeddedTrackToStorage() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        adsOperations.ads(TRACK_URN).subscribe();

        expect(storeTracksCommand.getInput()).toEqual(Arrays.asList(fullAdsForTrack.audioAd().getApiTrack()));
        verify(storeTracksCommand).call();
    }

    @Test
    public void shouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getMetaDataAt(0)).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(123L)));

        expect(adsOperations.isAudioAdAtPosition(0)).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfCurrentItemIsNotAudioAd() {
        when(playQueueManager.getMetaDataAt(1)).thenReturn(PropertySet.create());
        expect(adsOperations.isAudioAdAtPosition(1)).toBeFalse();
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

        expect(playQueue.getUrn(0)).toEqual(TRACK_URN);
        final PropertySet expectedProperties = adsWithOnlyInterstitial.interstitialAd().toPropertySet();
        expectedProperties.put(AdOverlayProperty.META_AD_DISMISSED, false);
        expectedProperties.put(TrackProperty.URN, TRACK_URN);
        expect(playQueue.getMetaData(0)).toEqual(expectedProperties);
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

        expect(playQueue.getUrn(0)).toEqual(TRACK_URN);
        final PropertySet expectedProperties = ads.interstitialAd().toPropertySet();
        expectedProperties.put(AdOverlayProperty.META_AD_DISMISSED, false);
        expectedProperties.put(TrackProperty.URN, TRACK_URN);
        expect(playQueue.getMetaData(0)).toEqual(expectedProperties);
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

        expect(playQueue.getUrn(0)).toEqual(adsWithOnlyAudioAd.audioAd().getApiTrack().getUrn());
        expect(playQueue.getUrn(1)).toEqual(TRACK_URN);
        expect(playQueue.getMetaData(1)).toEqual(adsWithOnlyAudioAd.audioAd().getLeaveBehind()
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

        expect(playQueue.getUrn(0)).toEqual(fullAdsForTrack.audioAd().getApiTrack().getUrn());
        expect(playQueue.getUrn(1)).toEqual(TRACK_URN);
        expect(playQueue.getMetaData(1)).toEqual(fullAdsForTrack.audioAd().getLeaveBehind()
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
        expect(playQueue.getUrn(0)).toEqual(TRACK_URN);
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

        expect(playQueue.getUrn(0)).toEqual(audioAdWithoutLeaveBehind.getApiTrack().getUrn());
        expect(playQueue.getUrn(1)).toEqual(TRACK_URN);
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

        expect(playQueue.getUrn(0)).toEqual(noInterstitial.audioAd().getApiTrack().getUrn());
        expect(playQueue.getUrn(1)).toEqual(TRACK_URN);
        expect(playQueue.getMetaData(1)).toEqual(noInterstitial.audioAd().getLeaveBehind()
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

        expect(playQueue.getUrn(0)).toEqual(apiTrack.getUrn());
        expect(playQueue.getUrn(1)).toEqual(TRACK_URN);
        expect(playQueue.getMetaData(1)).toBeEmpty();
    }
}

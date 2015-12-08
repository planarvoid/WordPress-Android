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
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.optional.Optional;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;

public class AdsOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private AdsOperations adsOperations;
    private ApiAdsForTrack fullAdsForTrack;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private FeatureFlags featureFlags;
    private PlaySessionSource playSessionSource;

    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(storeTracksCommand, playQueueManager, apiClientRx, Schedulers.immediate(), featureFlags);
        fullAdsForTrack = AdFixtures.fullAdsForTrack();
        playSessionSource = PlaySessionSource.forExplore("origin", "1.0");

        when(featureFlags.isEnabled(Flag.VIDEO_ADS)).thenReturn(false);
    }

    @Test
    public void adsReturnsAdsFromMobileApi() throws Exception {
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

        verify(storeTracksCommand).call(Arrays.asList(fullAdsForTrack.audioAd().get().getApiTrack()));
    }

    @Test
    public void isCurrentItemAdShouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnTrueIfCurrentItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseIfCurrentItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseOnEmptyPlayQueueItem() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0)).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

   @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(1))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(1))
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(1))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsEmptyPlayQueueItem() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(2);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(1)).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isTrue();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getQueueSize()).thenReturn(1);
        when(playQueueManager.getCurrentPosition()).thenReturn(0);
        when(playQueueManager.getPlayQueueItemAtPosition(0))
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void applyAdMergesInterstitialWhenNoAudioAdIsAvailable() throws Exception {
        final ApiAdsForTrack adsWithOnlyInterstitial = AdFixtures.interstitialAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, adsWithOnlyInterstitial);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(TRACK_URN);
        final InterstitialAd expectedInterstitial = AdFixtures.getInterstitialAd(TRACK_URN);
        assertThat(playQueue.getAdData(0)).isEqualTo(Optional.of(expectedInterstitial));
    }

    @Test
    public void applyInterstitialMergesInterstitial() throws Exception {
        final ApiAdsForTrack ads = AdFixtures.fullAdsForTrack();
        adsOperations.applyInterstitialToTrack(TRACK_URN, ads);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(TRACK_URN);
        final InterstitialAd expectedInterstitial = AdFixtures.getInterstitialAd(TRACK_URN);
        assertThat(playQueue.getAdData(0)).isEqualTo(Optional.of(expectedInterstitial));
    }

    @Test
    public void applyAdInsertsAudioAdWhenOnlyAudioAdIsAvailable() throws Exception {
        ApiAdsForTrack adsWithOnlyAudioAd = AdFixtures.audioAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, adsWithOnlyAudioAd);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(adsWithOnlyAudioAd.audioAd().get().getApiTrack().getUrn());
        assertThat(playQueue.getPlayQueueItem(1).getUrn()).isEqualTo(TRACK_URN);
        final LeaveBehindAd expectedLeaveBehind = AdFixtures.getLeaveBehindAd(adsWithOnlyAudioAd.audioAd().get().getApiTrack().getUrn());
        assertThat(playQueue.getAdData(1)).isEqualTo(Optional.of(expectedLeaveBehind));
    }

    @Test
    public void applyAdPrefersInterstitialWhenBothAreAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = AdFixtures.fullAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, fullAdsForTrack);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(TRACK_URN);
        final InterstitialAd expectedInterstitial = AdFixtures.getInterstitialAd(TRACK_URN);
        assertThat(playQueue.getAdData(0)).isEqualTo(Optional.of(expectedInterstitial));
    }

    @Test
    public void applyAdIsNoOpIfNoAdsAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = new ApiAdsForTrack(Collections.<ApiAdWrapper>emptyList());
        adsOperations.applyAdToTrack(TRACK_URN, fullAdsForTrack);

        final PlayQueue playQueue = createPlayQueue();

        verify(playQueueManager, never()).performPlayQueueUpdateOperations(any(PlayQueueManager.QueueUpdateOperation.class));
        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void insertAudioAdShouldInsertAudioAd() throws Exception {
        final ApiAudioAd audioAdWithoutLeaveBehind = AdFixtures.getApiAudioAdWithoutLeaveBehind();

        adsOperations.applyAdToTrack(TRACK_URN, new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(audioAdWithoutLeaveBehind))));

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(audioAdWithoutLeaveBehind.getApiTrack().getUrn());
        assertThat(playQueue.getPlayQueueItem(1).getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void insertAudioAdShouldInsertAudioAdAndLeaveBehind() throws Exception {
        final ApiAdsForTrack noInterstitial = AdFixtures.audioAdsForTrack();
        adsOperations.applyAdToTrack(TRACK_URN, noInterstitial);

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(noInterstitial.audioAd().get().getApiTrack().getUrn());
        assertThat(playQueue.getPlayQueueItem(1).getUrn()).isEqualTo(TRACK_URN);
        final LeaveBehindAd expectedLeaveBehind = AdFixtures.getLeaveBehindAd(noInterstitial.audioAd().get().getApiTrack().getUrn()) ;
        assertThat(playQueue.getAdData(1)).isEqualTo(Optional.of(expectedLeaveBehind));
    }

    @Test
    public void applyAdInsertsAudioAdWithNoLeaveBehindWhenNoOtherAdIsAvailable() throws Exception {
        final ApiAudioAd apiAudioAd = AdFixtures.getApiAudioAdWithoutLeaveBehind();

        adsOperations.applyAdToTrack(TRACK_URN, new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(apiAudioAd))));

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).getUrn()).isEqualTo(apiAudioAd.getApiTrack().getUrn());
        assertThat(playQueue.getPlayQueueItem(1).getUrn()).isEqualTo(TRACK_URN);
        assertThat(playQueue.getAdData(1).isPresent()).isFalse();
    }

    public void insertVideoAdShouldInsertVideoAd() throws Exception {
        when(featureFlags.isEnabled(Flag.VIDEO_ADS)).thenReturn(true);
        final ApiVideoAd videoAd = AdFixtures.getApiVideoAd();

        adsOperations.applyAdToTrack(TRACK_URN, new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(videoAd))));

        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor1 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        ArgumentCaptor<PlayQueueManager.QueueUpdateOperation> captor2 = ArgumentCaptor.forClass(PlayQueueManager.QueueUpdateOperation.class);
        verify(playQueueManager).performPlayQueueUpdateOperations(captor1.capture(), captor2.capture());

        final PlayQueueManager.QueueUpdateOperation value1 = captor1.getValue();
        final PlayQueueManager.QueueUpdateOperation value2 = captor2.getValue();
        final PlayQueue playQueue = createPlayQueue();
        value1.execute(playQueue);
        value2.execute(playQueue);

        assertThat(playQueue.getPlayQueueItem(0).isVideo()).isTrue();
        assertThat(playQueue.getPlayQueueItem(0).getAdData()).isEqualTo(Optional.of(VideoAd.create(videoAd, TRACK_URN)));
        assertThat(playQueue.getPlayQueueItem(1).getUrn()).isEqualTo(TRACK_URN);
    }

    @NonNull
    private PlayQueue createPlayQueue() {
        return TestPlayQueue.fromUrns(playSessionSource, TRACK_URN);
    }
}

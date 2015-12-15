package com.soundcloud.android.ads;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;

public class AdsOperationsTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private final TrackQueueItem trackQueueItem = TestPlayQueueItem.createTrack(TRACK_URN);

    private AdsOperations adsOperations;
    private ApiAdsForTrack fullAdsForTrack;

    @Mock private ApiClientRx apiClientRx;
    @Mock private StoreTracksCommand storeTracksCommand;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(storeTracksCommand, playQueueManager, apiClientRx, Schedulers.immediate(), featureFlags, eventBus);
        fullAdsForTrack = AdFixtures.fullAdsForTrack();
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(trackQueueItem);

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
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnTrueIfCurrentItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseIfCurrentItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseOnEmptyPlayQueueItem() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

   @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsEmptyPlayQueueItem() throws CreateModelException {
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnTrueIfCurrentItemIsAudioAd() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isTrue();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsVideoAd() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsRegularTrack() throws CreateModelException {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void applyAdMergesInterstitialWhenNoAudioAdIsAvailable() throws Exception {
        final ApiAdsForTrack adsWithOnlyInterstitial = AdFixtures.interstitialAdsForTrack();
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(trackQueueItem);

        adsOperations.applyAdToUpcomingTrack(adsWithOnlyInterstitial);

        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.of(AdFixtures.getInterstitialAd(TRACK_URN)));
    }

    @Test
    public void applyInterstitialMergesInterstitial() throws Exception {
        adsOperations.applyInterstitialToTrack(trackQueueItem, AdFixtures.fullAdsForTrack());

        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.of(AdFixtures.getInterstitialAd(TRACK_URN)));
    }

    @Test
    public void applyAdInsertsAudioAdWhenOnlyAudioAdIsAvailable() throws Exception {
        ApiAdsForTrack adsWithOnlyAudioAd = AdFixtures.audioAdsForTrack();
        final LeaveBehindAd expectedLeavebehding = LeaveBehindAd.create(adsWithOnlyAudioAd.audioAd().get().getLeaveBehind(),
                adsWithOnlyAudioAd.audioAd().get().getApiTrack().getUrn());

        adsOperations.applyAdToUpcomingTrack(adsWithOnlyAudioAd);

        verifyAudioAdInserted(adsWithOnlyAudioAd, Optional.of(expectedLeavebehding));
    }

    @Test
    public void applyAdPrefersInterstitialWhenBothAreAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = AdFixtures.fullAdsForTrack();
        adsOperations.applyAdToUpcomingTrack(fullAdsForTrack);

        verify(playQueueManager, never()).insertAudioAd(any(PlayQueueItem.class), any(Urn.class), any(AudioAd.class), anyBoolean());
        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.of(AdFixtures.getInterstitialAd(TRACK_URN)));
    }

    @Test
    public void applyAdIsNoOpIfNoAdsAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = new ApiAdsForTrack(Collections.<ApiAdWrapper>emptyList());
        adsOperations.applyAdToUpcomingTrack(fullAdsForTrack);

        verify(playQueueManager, never()).insertAudioAd(any(PlayQueueItem.class), any(Urn.class), any(AudioAd.class), anyBoolean());
        verify(playQueueManager, never()).insertVideo(any(PlayQueueItem.class), any(VideoAd.class));
        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.absent());
    }

    @Test
    public void insertAudioAdShouldInsertAudioAd() throws Exception {
        final ApiAudioAd audioAdWithoutLeaveBehind = AdFixtures.getApiAudioAdWithoutLeaveBehind();

        adsOperations.applyAdToUpcomingTrack(new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(audioAdWithoutLeaveBehind))));

        verify(playQueueManager).insertAudioAd(trackQueueItem, audioAdWithoutLeaveBehind.getApiTrack().getUrn(),
                AudioAd.create(audioAdWithoutLeaveBehind, TRACK_URN), false);
        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.absent());
    }

    @Test
    public void insertAudioAdShouldInsertAudioAdAndLeaveBehind() throws Exception {
        final ApiAdsForTrack noInterstitial = AdFixtures.audioAdsForTrack();
        final LeaveBehindAd expectedLeaveBehind = AdFixtures.getLeaveBehindAd(noInterstitial.audioAd().get().getApiTrack().getUrn()) ;

        adsOperations.applyAdToUpcomingTrack(noInterstitial);

        verifyAudioAdInserted(noInterstitial, Optional.of(expectedLeaveBehind));
    }

    @Test
    public void applyAdInsertsAudioAdWithNoLeaveBehindWhenNoOtherAdIsAvailable() throws Exception {
        final ApiAudioAd apiAudioAd = AdFixtures.getApiAudioAdWithoutLeaveBehind();
        final ApiAdsForTrack ads = new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(apiAudioAd)));

        adsOperations.applyAdToUpcomingTrack(ads);

        verifyAudioAdInserted(ads, Optional.<LeaveBehindAd>absent());
    }

    public void insertVideoAdShouldInsertVideoAd() throws Exception {
        when(featureFlags.isEnabled(Flag.VIDEO_ADS)).thenReturn(true);
        final ApiVideoAd videoAd = AdFixtures.getApiVideoAd();
        final ApiAdsForTrack ads = new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(videoAd)));

        adsOperations.applyAdToUpcomingTrack(ads);

        verify(playQueueManager).insertVideo(trackQueueItem,VideoAd.create(videoAd, TRACK_URN));
        assertThat(trackQueueItem.getAdData()).isEqualTo(Optional.of(VideoAd.create(videoAd, TRACK_URN)));
    }

    private void verifyAudioAdInserted(ApiAdsForTrack noInterstitial, Optional<LeaveBehindAd> leaveBehindAdOptional) {
        verify(playQueueManager).insertAudioAd(trackQueueItem, noInterstitial.audioAd().get().getApiTrack().getUrn(),
                AudioAd.create(noInterstitial.audioAd().get(), TRACK_URN), false);
        assertThat(trackQueueItem.getAdData()).isEqualTo(leaveBehindAdOptional);
    }
}

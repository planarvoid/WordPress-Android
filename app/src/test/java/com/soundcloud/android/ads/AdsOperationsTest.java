package com.soundcloud.android.ads;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueItemsEqual;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.ads.AdsOperations.AdRequestData;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AdsOperationsTest extends AndroidUnitTest {

    private static final Date CURRENT_DATE = new Date();
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private final TrackQueueItem trackQueueItem = TestPlayQueueItem.createTrack(TRACK_URN);

    private AdsOperations adsOperations;
    private ApiAdsForTrack fullAdsForTrack;
    private ApiAdsForStream fullAdsForStream;

    @Mock private FeatureOperations featureOperations;
    @Mock private KruxSegmentProvider kruxSegmentProvider;
    @Mock private ApiClientRx apiClientRx;
    @Mock private PlayQueueManager playQueueManager;
    @Captor private ArgumentCaptor<List> listArgumentCaptor;
    private TestDateProvider dateProvider = new TestDateProvider(CURRENT_DATE);

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        adsOperations = new AdsOperations(playQueueManager,
                                          featureOperations,
                                          apiClientRx,
                                          Schedulers.immediate(),
                                          eventBus,
                                          InjectionSupport.lazyOf(kruxSegmentProvider),
                                          dateProvider);
        fullAdsForTrack = AdFixtures.fullAdsForTrack();
        fullAdsForStream = AdFixtures.fullAdsForStream();
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(trackQueueItem);
    }

    @Test
    public void kruxSegmentProviderUsedIfShouldUseKruxForAdTargetingIsTrue() {
        when(featureOperations.shouldUseKruxForAdTargeting()).thenReturn(true);
        when(kruxSegmentProvider.getSegments()).thenReturn(Optional.absent());

        adsOperations.kruxSegments().subscribe();

        verify(kruxSegmentProvider).getSegments();
    }

    @Test
    public void kruxSegmentProviderNotUsedIfShouldUseKruxForAdTargetingIsFalse() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));
        when(featureOperations.shouldUseKruxForAdTargeting()).thenReturn(false);

        adsOperations.kruxSegments().subscribe();

        verify(kruxSegmentProvider, never()).getSegments();
    }

    @Test
    public void kruxSegmentsAddedToAdsRequestEndpoint() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        final AdRequestData requestData = AdRequestData.forPlayerAd(TRACK_URN, Optional.of("123,321"));
        adsOperations.ads(requestData, true, true).subscribe();

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(captor.capture(), eq(ApiAdsForTrack.class));

        assertThat(captor.getValue().getQueryParameters().get("krux_segments")).contains("123,321");
    }

    @Test
    public void kruxSegmentsNotAddedToAdsRequestEndpointIfNoneExist() {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        final AdRequestData requestData = AdRequestData.forPlayerAd(TRACK_URN, Optional.absent());
        adsOperations.ads(requestData, true, true).subscribe();

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(captor.capture(), eq(ApiAdsForTrack.class));

        assertThat(captor.getValue().getQueryParameters().keySet()).doesNotContain("krux_segments");
    }

    @Test
    public void kruxSegmentsAddedToStreamAdsRequestEndpoint() {
        when(apiClientRx
                .mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.INLAY_ADS.path())), eq(ApiAdsForStream.class)))
                .thenReturn(Observable.just(fullAdsForStream));

        final AdRequestData requestData = AdRequestData.forStreamAds(Optional.of("123,321"));
        adsOperations.inlayAds(requestData).subscribe();

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(captor.capture(), eq(ApiAdsForStream.class));

        assertThat(captor.getValue().getQueryParameters().get("krux_segments")).contains("123,321");
    }

    @Test
    public void kruxSegmentsNotAddedToStreamAdsRequestEndpointIfNoneExist() {
        when(apiClientRx
                .mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.INLAY_ADS.path())), eq(ApiAdsForStream.class)))
                .thenReturn(Observable.just(fullAdsForStream));

        final AdRequestData requestData = AdRequestData.forStreamAds(Optional.absent());
        adsOperations.inlayAds(requestData).subscribe();

        ArgumentCaptor<ApiRequest> captor = ArgumentCaptor.forClass(ApiRequest.class);
        verify(apiClientRx).mappedResponse(captor.capture(), eq(ApiAdsForStream.class));

        assertThat(captor.getValue().getQueryParameters().keySet()).doesNotContain("krux_segments");
    }

    @Test
    public void inlayAdsReturnsAdsFromMobileApi() {
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.INLAY_ADS.path())), eq(ApiAdsForStream.class)))
                .thenReturn(Observable.just(fullAdsForStream));

        final AdRequestData requestData = AdRequestData.forStreamAds(Optional.absent());
        final List<AdData> actual = adsOperations.inlayAds(requestData).toBlocking().first();
        final List<AdData> expected = fullAdsForStream.getAds(dateProvider);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void inlayAdsEmitsAdRequestEventOnSuccessfulResponse() {
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.INLAY_ADS.path())), eq(ApiAdsForStream.class)))
                .thenReturn(Observable.just(fullAdsForStream));

        final AdRequestData requestData = AdRequestData.forStreamAds(Optional.absent());
        adsOperations.inlayAds(requestData).subscribe();

        final AdRequestEvent trackingEvent = (AdRequestEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.adsRequestSuccess()).isEqualTo(true);
    }

    @Test
    public void adsReturnsAdsFromMobileApi() throws Exception {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), TRACK_URN.toEncodedString());
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", endpoint)), eq(ApiAdsForTrack.class)))
                .thenReturn(Observable.just(fullAdsForTrack));

        final AdRequestData requestData = AdRequestData.forPlayerAd(TRACK_URN, Optional.absent());
        assertThat(adsOperations.ads(requestData, true, true).toBlocking().first()).isEqualTo(fullAdsForTrack);
    }

    @Test
    public void isCurrentItemAdShouldReturnTrueIfCurrentItemIsAudioAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnTrueIfCurrentItemIsVideoAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAd()).isTrue();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseIfCurrentItemIsRegularTrack() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAdShouldReturnFalseOnEmptyPlayQueueItem() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isCurrentItemAd()).isFalse();
    }

    @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsAudioAd() {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnTrueIfNextItemIsVideoAd() {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isNextItemAd()).isTrue();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsRegularTrack() {
        when(playQueueManager.getNextPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isNextItemAdShouldReturnFalseIfNextItemIsEmptyPlayQueueItem() {
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);

        assertThat(adsOperations.isNextItemAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnTrueIfCurrentItemIsAudioAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isTrue();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsVideoAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void isCurrentItemAudioAdShouldReturnFalseIfCurrentItemIsRegularTrack() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemAudioAd()).isFalse();
    }

    @Test
    public void isCurrentItemVideoAdShouldReturnTrueIfCurrentItemIsVideoAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemVideoAd()).isTrue();
    }

    @Test
    public void isCurrentItemVideoAdShouldReturnFalseIfCurrentItemIsAudioAd() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L))));

        assertThat(adsOperations.isCurrentItemVideoAd()).isFalse();
    }

    @Test
    public void isCurrentItemVideoAdShouldReturnFalseIfCurrentItemIsRegularTrack() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L)));

        assertThat(adsOperations.isCurrentItemVideoAd()).isFalse();
    }

    @Test
    public void isCurrentItemLetterboxVideoAdShouldReturnTrueIfCurrentItemIsLetterbox() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L),
                                                                                AdFixtures.getApiVideoSource(600,
                                                                                                             300))));

        assertThat(adsOperations.isCurrentItemLetterboxVideoAd()).isTrue();
    }

    @Test
    public void isCurrentItemLetterboxVideoAdShouldReturnTrueIfCurrentItemIsVertical() {
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L),
                                                                                AdFixtures.getApiVideoSource(300,
                                                                                                             600))));

        assertThat(adsOperations.isCurrentItemLetterboxVideoAd()).isFalse();
    }

    @Test
    public void applyAdMergesInterstitialWhenNoAudioAdIsAvailable() throws Exception {
        final ApiAdsForTrack adsWithOnlyInterstitial = AdFixtures.interstitialAdsForTrack();
        when(playQueueManager.getNextPlayQueueItem()).thenReturn(trackQueueItem);

        adsOperations.applyAdToUpcomingTrack(adsWithOnlyInterstitial);

        verifyInterstitialInserted(adsWithOnlyInterstitial.interstitialAd().get());
    }

    @Test
    public void applyInterstitialMergesInterstitial() throws Exception {
        final ApiAdsForTrack allAds = AdFixtures.fullAdsForTrack();
        adsOperations.applyInterstitialToTrack(trackQueueItem, allAds);

        verifyInterstitialInserted(allAds.interstitialAd().get());
    }

    @Test
    public void applyAdInsertsAudioAdWhenOnlyAudioAdIsAvailable() throws Exception {
        ApiAdsForTrack adsWithOnlyAudioAd = AdFixtures.audioAdsForTrack();

        adsOperations.applyAdToUpcomingTrack(adsWithOnlyAudioAd);

        verifyAudioAdInserted(adsWithOnlyAudioAd);
    }

    @Test
    public void applyAdPrefersInterstitialWhenAudioAndInterstitialAreAvailable() throws Exception {
        ApiAdsForTrack adsForTrack = new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(AdFixtures.getApiAudioAd()),
                ApiAdWrapper.create(AdFixtures.getApiInterstitial())));
        adsOperations.applyAdToUpcomingTrack(adsForTrack);

        verifyInterstitialInserted(adsForTrack.interstitialAd().get());
    }

    @Test
    public void applyAdPrefersVideoWhenAllAdTypesAreAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = AdFixtures.fullAdsForTrack();
        adsOperations.applyAdToUpcomingTrack(fullAdsForTrack);

        verifyVideoInserted(fullAdsForTrack.videoAd().get());
    }

    @Test
    public void applyAdIsNoOpIfNoAdsAvailable() throws Exception {
        ApiAdsForTrack fullAdsForTrack = new ApiAdsForTrack(Collections.emptyList());
        adsOperations.applyAdToUpcomingTrack(fullAdsForTrack);

        verify(playQueueManager, never()).replace(any(PlayQueueItem.class), any(List.class));
    }

    @Test
    public void insertAudioAdShouldInsertAudioAdAndLeaveBehind() throws Exception {
        final ApiAdsForTrack noInterstitial = AdFixtures.audioAdsForTrack();

        adsOperations.applyAdToUpcomingTrack(noInterstitial);

        verifyAudioAdInserted(noInterstitial);
        PlayQueueItem playQueueItem = (PlayQueueItem) listArgumentCaptor.getValue().get(1);
        assertThat(playQueueItem.getAdData().isPresent()).isTrue();
        assertThat(playQueueItem.getAdData().get()).isInstanceOf(LeaveBehindAd.class);
    }

    @Test
    public void applyAdInsertsAudioAdWithNoLeaveBehindWhenNoOtherAdIsAvailable() throws Exception {
        final ApiAudioAd apiAudioAd = AdFixtures.getApiAudioAdWithoutLeaveBehind();
        final ApiAdsForTrack ads = new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(apiAudioAd)));

        adsOperations.applyAdToUpcomingTrack(ads);

        verifyAudioAdInserted(ads);
    }

    @Test
    public void replaceVideoAdReplacesVideoAdWithAudioAdIfAllAdsAvailable() {
        final ApiAdsForTrack allAds = AdFixtures.fullAdsForTrack();
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));

        adsOperations.replaceUpcomingVideoAd(allAds, videoItem);

        verify(playQueueManager).removeUpcomingItem(videoItem, false);
        verify(playQueueManager).replace(same(trackQueueItem), listArgumentCaptor.capture());

        final AudioAd audioAdData = AudioAd.create(allAds.audioAd().get(), trackQueueItem.getUrn());
        final AudioAdQueueItem audioAdItem = new AudioAdQueueItem(audioAdData);
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(trackQueueItem)
                .withAdData(LeaveBehindAd.create(allAds.audioAd().get().getLeaveBehind(), audioAdData.getAdUrn())).build();

        assertPlayQueueItemsEqual(listArgumentCaptor.getValue(), Arrays.asList(audioAdItem, newMonetizableItem));
    }

    @Test
    public void replaceVideoAdReplacesVideoAdWithInterstitialIfNoAudioAdAvailable() {
        final ApiAdsForTrack interstitialAdForTrack = AdFixtures.interstitialAdsForTrack();
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));

        adsOperations.replaceUpcomingVideoAd(interstitialAdForTrack, videoItem);

        verify(playQueueManager).removeUpcomingItem(videoItem, false);
        verify(playQueueManager).replace(same(trackQueueItem), listArgumentCaptor.capture());

        final List actualItems = listArgumentCaptor.getValue();
        final TrackQueueItem interstitialItem = new TrackQueueItem.Builder(trackQueueItem)
                .withAdData(InterstitialAd.create(interstitialAdForTrack.interstitialAd().get(),
                                                  trackQueueItem.getUrn()))
                .build();

        assertPlayQueueItemsEqual(actualItems, Arrays.asList(interstitialItem));
    }

    @Test
    public void replaceVideoAdReplacesVideoAdWithNothingIfNoOtherAdTypesExist() {
        final ApiAdsForTrack emptyAds = new ApiAdsForTrack(Collections.emptyList());
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L)));

        adsOperations.replaceUpcomingVideoAd(emptyAds, videoItem);
        verify(playQueueManager).removeUpcomingItem(videoItem, true);
    }

    @Test
    public void insertVideoAdShouldInsertVideoAd() throws Exception {
        final ApiVideoAd videoAd = AdFixtures.getApiVideoAd();
        final ApiAdsForTrack ads = new ApiAdsForTrack(Arrays.asList(ApiAdWrapper.create(videoAd)));

        adsOperations.applyAdToUpcomingTrack(ads);

        final ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(playQueueManager).replace(same(trackQueueItem), listCaptor.capture());
        final List value = listCaptor.getValue();
        assertPlayQueueItemsEqual(value,
                                  Arrays.asList(new VideoAdQueueItem(VideoAd.createWithMonetizableTrack(videoAd, CURRENT_DATE.getTime(), TRACK_URN)),
                                                TestPlayQueueItem.createTrack(TRACK_URN)));
    }

    @Test
    public void sendsEventWhenClearingAds() {
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.removeItems(any())).thenReturn(true);

        adsOperations.clearAllAdsFromQueue();

        verify(playQueueManager).removeItems(eq(AdUtils.IS_PLAYER_AD_ITEM));
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE).adsRemoved()).isTrue();
    }

    @Test
    public void doesNotSendEventWhenNoAdsCleared() {
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.removeItems(any())).thenReturn(false);

        adsOperations.clearAllAdsFromQueue();

        verify(playQueueManager).removeItems(eq(AdUtils.IS_PLAYER_AD_ITEM));
        eventBus.verifyNoEventsOn(EventQueue.PLAY_QUEUE);
    }

    private void verifyAudioAdInserted(ApiAdsForTrack adsForTrack) {
        verify(playQueueManager).replace(same(trackQueueItem), listArgumentCaptor.capture());

        final AudioAd audioAdData = AudioAd.create(adsForTrack.audioAd().get(), TRACK_URN);
        final AudioAdQueueItem audioAdItem = new AudioAdQueueItem(audioAdData);

        TrackQueueItem.Builder builder = new TrackQueueItem.Builder(trackQueueItem);
        if (adsForTrack.audioAd().get().getLeaveBehind() != null) {
            builder = builder.withAdData(LeaveBehindAd.create(adsForTrack.audioAd().get().getLeaveBehind(),
                                                              audioAdData.getAdUrn()));
        }
        assertPlayQueueItemsEqual(listArgumentCaptor.getValue(), Arrays.asList(audioAdItem, builder.build()));
    }

    private void verifyVideoInserted(ApiVideoAd apiVideoAd) {
        verify(playQueueManager).replace(same(trackQueueItem), listArgumentCaptor.capture());
        final VideoAd videoAd = VideoAd.createWithMonetizableTrack(apiVideoAd, CURRENT_DATE.getTime(), TRACK_URN);
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(videoAd);
        assertPlayQueueItemsEqual(listArgumentCaptor.getValue(),
                                  Arrays.asList(videoItem, TestPlayQueueItem.createTrack(TRACK_URN)));
    }

    private void verifyInterstitialInserted(ApiInterstitial apiInterstitial) {
        verify(playQueueManager).replace(same(trackQueueItem), listArgumentCaptor.capture());
        final InterstitialAd interstitialAd = InterstitialAd.create(apiInterstitial, TRACK_URN);
        final TrackQueueItem interstitialItem = new TrackQueueItem.Builder(trackQueueItem).withAdData(interstitialAd)
                                                                                          .build();
        assertPlayQueueItemsEqual(listArgumentCaptor.getValue(), Arrays.asList(interstitialItem));
    }

}


package com.soundcloud.android.ads;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;
import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AdsOperations {

    private final FeatureOperations featureOperations;
    private final Lazy<KruxSegmentProvider> kruxSegmentProvider;
    private final PlayQueueManager playQueueManager;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final EventBus eventBus;
    private final CurrentDateProvider dateProvider;

    @Inject
    AdsOperations(PlayQueueManager playQueueManager, FeatureOperations featureOperations,
                  ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                  EventBus eventBus, Lazy<KruxSegmentProvider> kruxSegmentProvider, CurrentDateProvider dateProvider) {
        this.playQueueManager = playQueueManager;
        this.featureOperations = featureOperations;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.kruxSegmentProvider = kruxSegmentProvider;
        this.dateProvider = dateProvider;
    }

    Observable<Optional<String>> kruxSegments() {
        if (featureOperations.shouldUseKruxForAdTargeting()) {
            return Observable.just(kruxSegmentProvider.get().getSegments());
        }
        return Observable.just(Optional.<String>absent());
    }

    public Observable<ApiAdsForTrack> ads(AdRequestData requestData, boolean playerVisible, boolean inForeground) {
        final Urn sourceUrn = requestData.getMonetizableTrackUrn().get();
        final String endpoint = String.format(ApiEndpoints.ADS.path(), sourceUrn.toEncodedString());
        return apiClientRx.mappedResponse(buildApiRequest(endpoint, requestData), ApiAdsForTrack.class)
                          .subscribeOn(scheduler)
                          .doOnError(onRequestFailure(requestData, endpoint, playerVisible, inForeground))
                          .doOnNext(onRequestSuccess(requestData, endpoint, playerVisible, inForeground));
    }

    Observable<List<AdData>> inlayAds(AdRequestData requestData) {
        final String endpoint = ApiEndpoints.INLAY_ADS.path();
        final ApiRequest request = buildApiRequest(endpoint, requestData);
        return apiClientRx.mappedResponse(request, ApiAdsForStream.class)
                .subscribeOn(scheduler)
                .doOnError(onRequestFailure(requestData, endpoint, false, true))
                .doOnNext(onRequestSuccess(requestData, endpoint, false, true))
                .map(adsForStream -> adsForStream.getAds(dateProvider));
    }

    private ApiRequest buildApiRequest(String endpoint, AdRequestData requestData) {
        final ApiRequest.Builder request = ApiRequest.get(endpoint).forPrivateApi()
                .addQueryParam(AdConstants.CORRELATOR_PARAM, requestData.getRequestId());

        if (requestData.getKruxSegments().isPresent()) {
            request.addQueryParam(AdConstants.KRUX_SEGMENT_PARAM, requestData.getKruxSegments().get());
        }

        return request.build();
    }

    private Action1<AdsCollection> onRequestSuccess(final AdRequestData requestData, final String endpoint,
                                                    final boolean playerVisible, final boolean inForeground) {
        return apiAds -> {
            Log.i(ADS_TAG, "Retrieved ads via " + endpoint + ": " + apiAds.contentString());
            logRequestSuccess(apiAds, requestData, endpoint, playerVisible, inForeground);
        };
    }

    private Action1<Throwable> onRequestFailure(final AdRequestData requestData, final String endpoint,
                                                final boolean playerVisible, final boolean inForeground) {
        return throwable -> {
            Log.i(ADS_TAG, "Failed to retrieve ads via " + endpoint, throwable);
            if (throwable instanceof ApiRequestException && ((ApiRequestException) throwable).reason() == NOT_FOUND) {
                final ApiAdsForTrack emptyAdsResponse = new ApiAdsForTrack(Collections.emptyList());
                logRequestSuccess(emptyAdsResponse, requestData, endpoint, playerVisible, inForeground);
            } else {
                eventBus.publish(EventQueue.TRACKING, AdRequestEvent.adRequestFailure(requestData.getRequestId(),
                                                                                      requestData.getMonetizableTrackUrn(),
                                                                                      endpoint,
                                                                                      playerVisible,
                                                                                      inForeground));
            }
        };
    }

    private void logRequestSuccess(final AdsCollection apiAds, final AdRequestData requestData,
                                   final String endpoint, final boolean playerVisible, final boolean inForeground) {
        eventBus.publish(EventQueue.TRACKING, AdRequestEvent.adRequestSuccess(requestData.getRequestId(),
                                                                              requestData.getMonetizableTrackUrn(),
                                                                              endpoint,
                                                                              apiAds.toAdsReceived(),
                                                                              playerVisible,
                                                                              inForeground));
    }

    void applyAdToUpcomingTrack(ApiAdsForTrack ads) {
        final PlayQueueItem monetizableItem = playQueueManager.getNextPlayQueueItem();

        if (monetizableItem instanceof TrackQueueItem) {
            TrackQueueItem trackQueueItem = (TrackQueueItem) monetizableItem;
            if (ads.videoAd().isPresent()) {
                insertVideoAd(trackQueueItem, ads.videoAd().get());
            } else if (ads.interstitialAd().isPresent()) {
                applyInterstitialAd(ads.interstitialAd().get(), trackQueueItem);
            } else if (ads.audioAd().isPresent()) {
                insertAudioAd(trackQueueItem, ads.audioAd().get());
            }
        }
    }

    void applyInterstitialToTrack(PlayQueueItem playQueueItem, ApiAdsForTrack ads) {
        if (playQueueItem instanceof TrackQueueItem && ads.interstitialAd().isPresent()) {
            applyInterstitialAd(ads.interstitialAd().get(), (TrackQueueItem) playQueueItem);
        }
    }

    private void applyInterstitialAd(ApiInterstitial apiInterstitial, TrackQueueItem monetizableItem) {
        final InterstitialAd interstitialData = InterstitialAd.create(apiInterstitial, monetizableItem.getUrn());
        final TrackQueueItem interstitialItem = new TrackQueueItem.Builder(monetizableItem)
                .withAdData(interstitialData).build();
        playQueueManager.replace(monetizableItem, Collections.singletonList(interstitialItem));
    }

    private void insertVideoAd(TrackQueueItem monetizableItem, ApiVideoAd apiVideoAd) {
        final VideoAd videoData = VideoAd.createWithMonetizableTrack(apiVideoAd, dateProvider.getCurrentTime(), monetizableItem.getUrn());
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem).build();
        final VideoAdQueueItem videoItem = new VideoAdQueueItem(videoData);
        playQueueManager.replace(monetizableItem, Arrays.asList(videoItem, newMonetizableItem));
    }

    void insertAudioAd(TrackQueueItem monetizableItem, ApiAudioAd apiAudioAd) {
        final AudioAd audioAdData = AudioAd.create(apiAudioAd, monetizableItem.getUrn());

        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd.getLeaveBehind(), audioAdData, monetizableItem);
        } else {
            insertAudioAdWithoutLeaveBehind(monetizableItem, audioAdData);
        }
    }

    private void insertAudioAdWithoutLeaveBehind(TrackQueueItem monetizableItem,
                                                 AudioAd audioAdData) {
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem).build();
        final AudioAdQueueItem audioAdItem = new AudioAdQueueItem(audioAdData);

        playQueueManager.replace(monetizableItem, Arrays.asList(audioAdItem, newMonetizableItem));
    }

    private void insertAudioAdWithLeaveBehind(ApiLeaveBehind apiLeaveBehind,
                                              AudioAd audioAdData,
                                              TrackQueueItem monetizableItem) {
        final LeaveBehindAd leaveBehindAd = LeaveBehindAd.create(apiLeaveBehind, audioAdData.getAdUrn());
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem)
                .withAdData(leaveBehindAd).build();
        final AudioAdQueueItem audioAdItem = new AudioAdQueueItem(audioAdData);

        playQueueManager.replace(monetizableItem, Arrays.asList(audioAdItem, newMonetizableItem));
    }

    void replaceUpcomingVideoAd(ApiAdsForTrack ads, VideoAdQueueItem videoItem) {
        final boolean hasAudioAd = ads.audioAd().isPresent();
        final boolean hasInterstitial = ads.interstitialAd().isPresent();
        // Don't publish queue change if we can swap another ad in. Queue change will be published on insert.
        final boolean shouldPublishQueueChange = !hasAudioAd && !hasInterstitial;
        playQueueManager.removeUpcomingItem(videoItem, shouldPublishQueueChange);
        if (hasAudioAd) {
            insertAudioAd((TrackQueueItem) playQueueManager.getNextPlayQueueItem(), ads.audioAd().get());
        } else if (hasInterstitial) {
            applyInterstitialToTrack(playQueueManager.getNextPlayQueueItem(), ads);
        }
    }

    public boolean isCurrentItemAd() {
        return playQueueManager.getCurrentPlayQueueItem().isAd();
    }

    boolean isNextItemAd() {
        return playQueueManager.getNextPlayQueueItem().isAd();
    }

    public boolean isCurrentItemAudioAd() {
        return playQueueManager.getCurrentPlayQueueItem().isAudioAd();
    }

    public boolean isCurrentItemVideoAd() {
        return playQueueManager.getCurrentPlayQueueItem().isVideoAd();
    }

    boolean isCurrentItemLetterboxVideoAd() {
        return isCurrentItemVideoAd() && !((VideoAdQueueItem) playQueueManager.getCurrentPlayQueueItem()).isVerticalVideo();
    }

    public void clearAllAdsFromQueue() {
        if (playQueueManager.removeItems(AdUtils.IS_PLAYER_AD_ITEM)) {
            eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAdsRemoved(playQueueManager.getCollectionUrn()));
        }
    }

    public Optional<AdData> getCurrentTrackAdData() {
        return playQueueManager.getCurrentPlayQueueItem().getAdData();
    }

    public Optional<AdData> getNextTrackAdData() {
        return playQueueManager.getNextPlayQueueItem().getAdData();
    }

    @AutoValue
    public static abstract class AdRequestData {

        private static AdRequestData create(Optional<Urn> monetizableTrackUrn, Optional<String> kruxSegments)  {
            return new AutoValue_AdsOperations_AdRequestData(UUID.randomUUID().toString(), monetizableTrackUrn, kruxSegments);
        }

        static AdRequestData forPlayerAd(Urn monetizableTrackUrn, Optional<String> kruxSegments) {
            return create(Optional.of(monetizableTrackUrn), kruxSegments);
        }

        static AdRequestData forStreamAds(Optional<String> kruxSegments) {
            return create(Optional.absent(), kruxSegments);
        }

        public abstract String getRequestId();

        public abstract Optional<Urn> getMonetizableTrackUrn();

        public abstract Optional<String> getKruxSegments();
    }
}

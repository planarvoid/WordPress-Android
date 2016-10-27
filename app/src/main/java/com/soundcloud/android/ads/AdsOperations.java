
package com.soundcloud.android.ads;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;
import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.ads.AdsController.AdRequestData;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.playback.AudioAdQueueItem;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import dagger.Lazy;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdsOperations {

    private static final Func1<ApiAdsForStream, List<AppInstallAd>> GET_APP_INSTALLS = new Func1<ApiAdsForStream, List<AppInstallAd>>() {
        @Override
        public List<AppInstallAd> call(ApiAdsForStream adsForStream) {
            return adsForStream.getAppInstalls();
        }
    };

    private final FeatureOperations featureOperations;
    private final FeatureFlags featureFlags;
    private final Lazy<KruxSegmentProvider> kruxSegmentProvider;
    private final PlayQueueManager playQueueManager;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final EventBus eventBus;


    @Inject
    AdsOperations(PlayQueueManager playQueueManager, FeatureOperations featureOperations, FeatureFlags featureFlags,
                  ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                  EventBus eventBus, Lazy<KruxSegmentProvider> kruxSegmentProvider) {
        this.playQueueManager = playQueueManager;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
        this.kruxSegmentProvider = kruxSegmentProvider;
    }

    Observable<Optional<String>> kruxSegments() {
        if (featureOperations.shouldUseKruxForAdTargeting()) {
            return Observable.just(kruxSegmentProvider.get().getSegments());
        }
        return Observable.just(Optional.<String>absent());
    }

    Observable<ApiAdsForTrack> ads(AdRequestData requestData, boolean playerVisible, boolean inForeground) {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), requestData.monetizableTrackUrn.toEncodedString());
        return apiClientRx.mappedResponse(buildAdRequest(requestData, endpoint), ApiAdsForTrack.class)
                          .subscribeOn(scheduler)
                          .doOnError(onRequestFailure(requestData, endpoint, playerVisible, inForeground))
                          .doOnNext(onRequestSuccess(requestData, endpoint, playerVisible, inForeground));
    }

    public Observable<List<AppInstallAd>> inlaysAds() {
        if (featureFlags.isEnabled(Flag.APP_INSTALLS)) {
            final ApiRequest.Builder request = ApiRequest.get(ApiEndpoints.INLAY_ADS.path()).forPrivateApi();
            return apiClientRx.mappedResponse(request.build(), ApiAdsForStream.class)
                              .subscribeOn(scheduler)
                              .map(GET_APP_INSTALLS);
        }
        return Observable.just(Collections.<AppInstallAd>emptyList());
    }

    private ApiRequest buildAdRequest(AdRequestData requestData, String endpoint) {
        final ApiRequest.Builder request = ApiRequest.get(endpoint).forPrivateApi()
                .addQueryParam(AdConstants.CORRELATOR_PARAM, requestData.requestId);

        if (requestData.kruxSegments.isPresent()) {
            request.addQueryParam(AdConstants.KRUX_SEGMENT_PARAM, requestData.kruxSegments.get());
        }

        return request.build();
    }

    private Action1<? super ApiAdsForTrack> onRequestSuccess(final AdRequestData requestData, final String endpoint,
                                                             final boolean playerVisible, final boolean inForeground) {
        return new Action1<ApiAdsForTrack>() {
            @Override
            public void call(ApiAdsForTrack apiAds) {
                Log.i(ADS_TAG, "Retrieved ads for " + requestData.monetizableTrackUrn + ": " + apiAds.contentString());
                logRequestSuccess(apiAds, requestData, endpoint, playerVisible, inForeground);

            }
        };
    }

    private Action1<Throwable> onRequestFailure(final AdRequestData requestData, final String endpoint,
                                                final boolean playerVisible, final boolean inForeground) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.i(ADS_TAG, "Failed to retrieve ads for " + requestData.monetizableTrackUrn, throwable);
                if (throwable instanceof ApiRequestException && ((ApiRequestException) throwable).reason() == NOT_FOUND) {
                    final ApiAdsForTrack emptyAdsResponse = new ApiAdsForTrack(Collections.<ApiAdWrapper>emptyList());
                    logRequestSuccess(emptyAdsResponse, requestData, endpoint, playerVisible, inForeground);
                } else {
                    eventBus.publish(EventQueue.TRACKING, AdRequestEvent.adRequestFailure(requestData.requestId,
                                                                                          requestData.monetizableTrackUrn,
                                                                                          endpoint,
                                                                                          playerVisible,
                                                                                          inForeground));
                }
            }
        };
    }

    private void logRequestSuccess(final ApiAdsForTrack apiAds, final AdRequestData requestData,
                                   final String endpoint, final boolean playerVisible, final boolean inForeground) {
        eventBus.publish(EventQueue.TRACKING, AdRequestEvent.adRequestSuccess(requestData.requestId,
                                                                              requestData.monetizableTrackUrn,
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
        playQueueManager.replace(monetizableItem, Collections.<PlayQueueItem>singletonList(interstitialItem));
    }

    private void insertVideoAd(TrackQueueItem monetizableItem, ApiVideoAd apiVideoAd) {
        final VideoAd videoData = VideoAd.create(apiVideoAd, monetizableItem.getUrn());
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

    void clearAllAdsFromQueue() {
        playQueueManager.removeAds(PlayQueueEvent.fromAdsRemoved(playQueueManager.getCollectionUrn()));
    }

    public Optional<AdData> getCurrentTrackAdData() {
        return playQueueManager.getCurrentPlayQueueItem().getAdData();
    }

    public Optional<AdData> getNextTrackAdData() {
        return playQueueManager.getNextPlayQueueItem().getAdData();
    }
}

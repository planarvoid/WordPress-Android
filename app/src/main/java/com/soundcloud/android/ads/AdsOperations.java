package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.VideoQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;

public class AdsOperations {

    private final StoreTracksCommand storeTracksCommand;
    private final PlayQueueManager playQueueManager;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;
    private final Action1<ApiAdsForTrack> cacheAudioAdTrack = new Action1<ApiAdsForTrack>() {
        @Override
        public void call(ApiAdsForTrack apiAdsForTrack) {
            final Optional<ApiAudioAd> audioAd = apiAdsForTrack.audioAd();
            if (audioAd.isPresent()) {
                final ApiTrack track = audioAd.get().getApiTrack();
                storeTracksCommand.toAction().call(Arrays.asList(track));
            }
        }
    };

    @Inject
    AdsOperations(StoreTracksCommand storeTracksCommand, PlayQueueManager playQueueManager,
                  ApiClientRx apiClientRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                  FeatureFlags featureFlags, EventBus eventBus) {
        this.storeTracksCommand = storeTracksCommand;
        this.playQueueManager = playQueueManager;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
        this.eventBus = eventBus;
    }

    public Observable<ApiAdsForTrack> ads(Urn sourceUrn) {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), sourceUrn.toEncodedString());
        final ApiRequest.Builder request = ApiRequest.get(endpoint).forPrivateApi(1);

        return apiClientRx.mappedResponse(request.build(), ApiAdsForTrack.class)
                .subscribeOn(scheduler)
                .doOnError(logFailedAds(sourceUrn))
                .doOnNext(logAds(sourceUrn))
                .doOnNext(cacheAudioAdTrack);
    }

    private Action1<? super ApiAdsForTrack> logAds(final Urn sourceUrn) {
        return new Action1<ApiAdsForTrack>() {
            @Override
            public void call(ApiAdsForTrack apiAdWrappers) {
                Log.i(ADS_TAG, "Retrieved ads for " + sourceUrn.toString() + ": " + apiAdWrappers.contentString());
            }
        };
    }

    private Action1<Throwable> logFailedAds(final Urn sourceUrn) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.i(ADS_TAG, "Failed to retrieve ads for " + sourceUrn.toString(), throwable);
            }
        };
    }

    public void applyAdToUpcomingTrack(ApiAdsForTrack ads) {
        final PlayQueueItem monetizableItem = playQueueManager.getNextPlayQueueItem();

        if (featureFlags.isEnabled(Flag.VIDEO_ADS) && ads.videoAd().isPresent()) {
            insertVideoAd(monetizableItem, ads.videoAd().get());
        } else if (ads.interstitialAd().isPresent()) {
            applyInterstitialAd(ads.interstitialAd().get(), monetizableItem);
        } else if (ads.audioAd().isPresent()) {
            insertAudioAd(monetizableItem, ads.audioAd().get());
        }
    }

    public void applyInterstitialToTrack(PlayQueueItem playQueueItem, ApiAdsForTrack ads) {
        if (ads.interstitialAd().isPresent()) {
            applyInterstitialAd(ads.interstitialAd().get(), playQueueItem);
        }
    }

    private void applyInterstitialAd(ApiInterstitial apiInterstitial, PlayQueueItem monetizableItem) {
        final InterstitialAd interstitialData = InterstitialAd.create(apiInterstitial, monetizableItem.getUrn());
        monetizableItem.setAdData(Optional.<AdData>of(interstitialData));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(playQueueManager.getCollectionUrn()));
    }

    void insertVideoAd(PlayQueueItem monetizableItem, ApiVideoAd apiVideoAd) {
        final VideoAd videoData = VideoAd.create(apiVideoAd, monetizableItem.getUrn());
        monetizableItem.setAdData(Optional.<AdData>absent());
        playQueueManager.insertVideo(monetizableItem, videoData);
    }

    void insertAudioAd(PlayQueueItem monetizableItem, ApiAudioAd apiAudioAd) {
        final AudioAd audioAdData = AudioAd.create(apiAudioAd, monetizableItem.getUrn());

        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd, audioAdData, monetizableItem);
        } else {
            monetizableItem.setAdData(Optional.<AdData>absent());
            playQueueManager.insertAudioAd(monetizableItem, apiAudioAd.getApiTrack().getUrn(), audioAdData, false);
        }
    }

    private void insertAudioAdWithLeaveBehind(ApiAudioAd apiAudioAd, AudioAd audioAdData, PlayQueueItem monetizableItem) {
        final Urn audioAdTrack = apiAudioAd.getApiTrack().getUrn();
        final LeaveBehindAd leaveBehindAd = LeaveBehindAd.create(apiAudioAd.getLeaveBehind(), apiAudioAd.getApiTrack().getUrn());
        monetizableItem.setAdData(Optional.<AdData>of(leaveBehindAd));
        playQueueManager.insertAudioAd(monetizableItem, audioAdTrack, audioAdData, false);
    }

    void replaceUpcomingVideoAd(ApiAdsForTrack ads, VideoQueueItem videoItem) {
        final boolean hasAudioAd = ads.audioAd().isPresent();
        final boolean hasInterstitial = ads.interstitialAd().isPresent();
        // Don't publish queue change if we can swap another ad in. Queue change will be published on insert.
        final boolean shouldPublishQueueChange = !hasAudioAd && !hasInterstitial;
        playQueueManager.removeUpcomingItem(videoItem, shouldPublishQueueChange);
        if (hasAudioAd) {
            insertAudioAd(playQueueManager.getNextPlayQueueItem(), ads.audioAd().get());
        } else if (hasInterstitial) {
            applyInterstitialToTrack(playQueueManager.getNextPlayQueueItem(), ads);
        }
    }

    public boolean isCurrentItemAd() {
        return isAd(playQueueManager.getCurrentPlayQueueItem());
    }

    public boolean isNextItemAd() {
        return isAd(playQueueManager.getNextPlayQueueItem());
    }

    public boolean isCurrentItemAudioAd() {
        return isAudioAd(playQueueManager.getCurrentPlayQueueItem());
    }

    public boolean isCurrentItemVideoAd() {
        return isVideoAd(playQueueManager.getCurrentPlayQueueItem());
    }

    public static boolean isAd(PlayQueueItem playQueueItem) {
        return isAudioAd(playQueueItem) || isVideoAd(playQueueItem);
    }

    public static boolean isAudioAd(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent() && playQueueItem.getAdData().get() instanceof AudioAd;
    }

    public static boolean isVideoAd(PlayQueueItem playQueueItem) {
        return playQueueItem.isVideo();
    }

    public static boolean hasAdOverlay(PlayQueueItem playQueueItem) {
        return playQueueItem.getAdData().isPresent() && playQueueItem.getAdData().get() instanceof OverlayAdData;
    }

    public void clearAllAdsFromQueue() {
        playQueueManager.removeAds(PlayQueueEvent.fromAudioAdRemoved(playQueueManager.getCollectionUrn()));
    }

    public Optional<AdData> getNextTrackAdData() {
        return playQueueManager.getNextPlayQueueItem().getAdData();
    }
}

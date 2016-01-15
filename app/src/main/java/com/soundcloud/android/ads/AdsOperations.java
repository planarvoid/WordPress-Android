package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
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
import java.util.Collections;

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

        if (monetizableItem instanceof TrackQueueItem) {
            TrackQueueItem trackQueueItem = (TrackQueueItem) monetizableItem;
            if (featureFlags.isEnabled(Flag.VIDEO_ADS) && ads.videoAd().isPresent()) {
                insertVideoAd(trackQueueItem, ads.videoAd().get());
            } else if (ads.interstitialAd().isPresent()) {
                applyInterstitialAd(ads.interstitialAd().get(), trackQueueItem);
            } else if (ads.audioAd().isPresent()) {
                insertAudioAd(trackQueueItem, ads.audioAd().get());
            }
        }

    }

    public void applyInterstitialToTrack(PlayQueueItem playQueueItem, ApiAdsForTrack ads) {
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

    void insertVideoAd(TrackQueueItem monetizableItem, ApiVideoAd apiVideoAd) {
        final VideoAd videoData = VideoAd.create(apiVideoAd, monetizableItem.getUrn());
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem).build();
        final VideoQueueItem videoItem = new VideoQueueItem(videoData);
        playQueueManager.replace(monetizableItem, Arrays.asList(videoItem, newMonetizableItem));
    }

    void insertAudioAd(TrackQueueItem monetizableItem, ApiAudioAd apiAudioAd) {
        final AudioAd audioAdData = AudioAd.create(apiAudioAd, monetizableItem.getUrn());

        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd, audioAdData, monetizableItem);
        } else {
            insertAudioAdiWithoutLeaveBehind(monetizableItem, apiAudioAd, audioAdData);
        }
    }

    private void insertAudioAdiWithoutLeaveBehind(TrackQueueItem monetizableItem, ApiAudioAd apiAudioAd, AudioAd audioAdData) {
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem).build();
        final TrackQueueItem audioAdItem = new TrackQueueItem.Builder(apiAudioAd.getApiTrack().getUrn())
                .withAdData(audioAdData)
                .persist(false)
                .build();
        playQueueManager.replace(monetizableItem, Arrays.<PlayQueueItem>asList(audioAdItem, newMonetizableItem));
    }

    private void insertAudioAdWithLeaveBehind(ApiAudioAd apiAudioAd, AudioAd audioAdData, TrackQueueItem monetizableItem) {
        final LeaveBehindAd leaveBehindAd = LeaveBehindAd.create(apiAudioAd.getLeaveBehind(), apiAudioAd.getApiTrack().getUrn());
        final TrackQueueItem newMonetizableItem = new TrackQueueItem.Builder(monetizableItem)
                .withAdData(leaveBehindAd).build();
        final TrackQueueItem audioAdItem = new TrackQueueItem.Builder(apiAudioAd.getApiTrack().getUrn())
                .withAdData(audioAdData)
                .persist(false)
                .build();

        playQueueManager.replace(monetizableItem, Arrays.<PlayQueueItem>asList(audioAdItem, newMonetizableItem));
    }

    void replaceUpcomingVideoAd(ApiAdsForTrack ads, VideoQueueItem videoItem) {
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

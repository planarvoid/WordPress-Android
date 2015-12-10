package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;
import static com.soundcloud.java.checks.Preconditions.checkState;

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
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.LocaleProvider;
import com.soundcloud.java.optional.Optional;
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
                  FeatureFlags featureFlags) {
        this.storeTracksCommand = storeTracksCommand;
        this.playQueueManager = playQueueManager;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.featureFlags = featureFlags;
    }

    public Observable<ApiAdsForTrack> ads(Urn sourceUrn) {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), sourceUrn.toEncodedString());
        final ApiRequest.Builder request = ApiRequest.get(endpoint).forPrivateApi(1);

        final String locale = LocaleProvider.getFormattedLocale();
        if (!locale.isEmpty()) {
            request.addQueryParam(ApiRequest.Param.LOCALE, locale);
        }

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

    public void applyAdToTrack(Urn monetizableTrack, ApiAdsForTrack ads) {
        final int currentMonetizablePosition = playQueueManager.getUpcomingPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");

        if (featureFlags.isEnabled(Flag.VIDEO_ADS) && ads.videoAd().isPresent()) {
            insertVideoAd(monetizableTrack, ads.videoAd().get(), currentMonetizablePosition);
        } else if (ads.interstitialAd().isPresent()) {
            applyInterstitialAd(ads.interstitialAd().get(), currentMonetizablePosition, monetizableTrack);
        } else if (ads.audioAd().isPresent()) {
            insertAudioAd(monetizableTrack, ads.audioAd().get(), currentMonetizablePosition);
        }
    }

    public void applyInterstitialToTrack(Urn monetizableTrack, ApiAdsForTrack ads) {
        final int currentMonetizablePosition = playQueueManager.getUpcomingPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");
        if (ads.interstitialAd().isPresent()) {
            applyInterstitialAd(ads.interstitialAd().get(), currentMonetizablePosition, monetizableTrack);
        }
    }

    private void applyInterstitialAd(ApiInterstitial apiInterstitial, int currentMonetizablePosition, Urn monetizableTrack) {
        final InterstitialAd interstitialData = InterstitialAd.create(apiInterstitial, monetizableTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.SetAdDataOperation(currentMonetizablePosition, Optional.<AdData>of(interstitialData))
        );
    }

    void insertVideoAd(Urn monetizableTrack, ApiVideoAd apiVideoAd, int currentMonetizablePosition) {
        final VideoAd videoData = VideoAd.create(apiVideoAd, monetizableTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.SetAdDataOperation(currentMonetizablePosition, Optional.<AdData>absent()),
                new PlayQueueManager.InsertVideoOperation(currentMonetizablePosition, videoData)
        );
    }

    void insertAudioAd(Urn monetizableTrack, ApiAudioAd apiAudioAd, int currentMonetizablePosition) {
        final AudioAd audioAdData = AudioAd.create(apiAudioAd, monetizableTrack);

        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd, audioAdData, currentMonetizablePosition);
        } else {
            playQueueManager.performPlayQueueUpdateOperations(
                    new PlayQueueManager.SetAdDataOperation(currentMonetizablePosition, Optional.<AdData>absent()),
                    new PlayQueueManager.InsertAudioOperation(currentMonetizablePosition, apiAudioAd.getApiTrack().getUrn(), audioAdData, false)
            );
        }
    }

    private void insertAudioAdWithLeaveBehind(ApiAudioAd apiAudioAd, AudioAd audioAdData, int currentMonetizablePosition) {
        final Urn audioAdTrack = apiAudioAd.getApiTrack().getUrn();
        final LeaveBehindAd leaveBehindAd = LeaveBehindAd.create(apiAudioAd.getLeaveBehind(), apiAudioAd.getApiTrack().getUrn());
        final int newMonetizablePosition = currentMonetizablePosition + 1;

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.InsertAudioOperation(currentMonetizablePosition, audioAdTrack, audioAdData, false),
                new PlayQueueManager.SetAdDataOperation(newMonetizablePosition, Optional.<AdData>of(leaveBehindAd))
        );
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

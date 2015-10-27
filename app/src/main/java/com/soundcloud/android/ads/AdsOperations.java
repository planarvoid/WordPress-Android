package com.soundcloud.android.ads;

import android.util.Log;

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
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.LocaleProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import static com.soundcloud.android.utils.Log.ADS_TAG;
import static com.soundcloud.java.checks.Preconditions.checkState;

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

    private void applyInterstitialAd(ApiInterstitial interstitial, int currentMonetizablePosition, Urn monetizableTrack) {
        PropertySet interstitialPropertySet = interstitial.toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(TrackProperty.URN, monetizableTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.SetMetadataOperation(currentMonetizablePosition, interstitialPropertySet)
        );
    }

    void insertVideoAd(Urn monetizableTrack, ApiVideoAd apiVideoAd, int currentMonetizablePosition) {
        PropertySet adMetaData = apiVideoAd.toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, monetizableTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.SetMetadataOperation(currentMonetizablePosition, PropertySet.create()),
                new PlayQueueManager.InsertVideoOperation(currentMonetizablePosition, adMetaData)
        );
    }

    void insertAudioAd(Urn monetizableTrack, ApiAudioAd apiAudioAd, int currentMonetizablePosition) {
        PropertySet adMetaData = apiAudioAd
                .toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, monetizableTrack);

        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd, adMetaData, currentMonetizablePosition);
        } else {
            playQueueManager.performPlayQueueUpdateOperations(
                    new PlayQueueManager.SetMetadataOperation(currentMonetizablePosition, PropertySet.create()),
                    new PlayQueueManager.InsertAudioOperation(currentMonetizablePosition, apiAudioAd.getApiTrack().getUrn(), adMetaData, false)
            );
        }
    }

    private void insertAudioAdWithLeaveBehind(ApiAudioAd apiAudioAd, PropertySet adMetaData, int currentMonetizablePosition) {
        int newMonetizablePosition = currentMonetizablePosition + 1;
        final Urn audioAdTrack = apiAudioAd.getApiTrack().getUrn();
        final PropertySet leaveBehindProperties = apiAudioAd
                .getLeaveBehind()
                .toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(LeaveBehindProperty.AUDIO_AD_TRACK_URN, audioAdTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.InsertAudioOperation(currentMonetizablePosition, audioAdTrack, adMetaData, false),
                new PlayQueueManager.SetMetadataOperation(newMonetizablePosition, leaveBehindProperties)
        );
    }

    public boolean isCurrentItemAd() {
        return isAdAtPosition(playQueueManager.getCurrentPosition());
    }

    public boolean isNextItemAd() {
        return isAdAtPosition(playQueueManager.getCurrentPosition() + 1);
    }

    public boolean isAdAtPosition(int position) {
        return isAudioAdAtPosition(position) || isVideoAdAtPosition(position);
    }

    public boolean isCurrentItemAudioAd() {
        return !playQueueManager.isQueueEmpty() && isAudioAdAtPosition(playQueueManager.getCurrentPosition());
    }

    private boolean isAudioAdAtPosition(int position) {
        final PlayQueueItem playQueueItem = playQueueManager.getPlayQueueItemAtPosition(position);
        return playQueueItem.getMetaData().contains(AdProperty.AD_URN) &&
                playQueueItem.getMetaData().get(AdProperty.AD_TYPE).equals(AdProperty.AD_TYPE_AUDIO);
    }

    private boolean isVideoAdAtPosition(int position) {
        final PlayQueueItem playQueueItem = playQueueManager.getPlayQueueItemAtPosition(position);
        return playQueueItem.isVideo();
    }

    public void clearAllAdsFromQueue() {
        playQueueManager.removeTracksWithMetaData(AdFunctions.HAS_AD_URN,
                PlayQueueEvent.fromAudioAdRemoved(playQueueManager.getCollectionUrn()));
    }

    public PropertySet getMonetizableTrackMetaData() {
        final int monetizableTrackPosition = playQueueManager.getCurrentPosition() + 1;
        return playQueueManager.getPlayQueueItemAtPosition(monetizableTrackPosition).getMetaData();
    }
}

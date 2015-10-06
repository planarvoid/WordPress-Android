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
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.LocaleProvider;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

public class AdsOperations {

    private final StoreTracksCommand storeTracksCommand;
    private final PlayQueueManager playQueueManager;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final Predicate<PropertySet> hasAdUrn = new Predicate<PropertySet>() {
        @Override
        public boolean apply(PropertySet input) {
            return input.contains(AdProperty.AD_URN);
        }
    };
    private final Action1<ApiAdsForTrack> cacheAudioAdTrack = new Action1<ApiAdsForTrack>() {
        @Override
        public void call(ApiAdsForTrack apiAdsForTrack) {
            if (apiAdsForTrack.hasAudioAd()) {
                final ApiTrack track = apiAdsForTrack.audioAd().getApiTrack();
                storeTracksCommand.toAction().call(Arrays.asList(track));
            }
        }
    };

    @Inject
    AdsOperations(StoreTracksCommand storeTracksCommand, PlayQueueManager playQueueManager, ApiClientRx apiClientRx,
                  @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeTracksCommand = storeTracksCommand;
        this.playQueueManager = playQueueManager;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
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
                StringBuilder msg = new StringBuilder(100);
                msg.append("Retrieved ads for ")
                        .append(sourceUrn.toString())
                        .append(": ");
                if (apiAdWrappers.hasAudioAd()) {
                    msg.append("audio ad, ");
                    if (apiAdWrappers.audioAd().hasApiLeaveBehind()) {
                        msg.append("leave behind, ");
                    }
                }
                if (apiAdWrappers.hasInterstitialAd()) {
                    msg.append("interstitial");
                }
                Log.i(ADS_TAG, msg.toString());
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
        final int currentMonetizablePosition = playQueueManager.getPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");
        if (ads.hasInterstitialAd()) {
            applyInterstitialAd(ads.interstitialAd(), currentMonetizablePosition, monetizableTrack);
        } else if (ads.hasAudioAd()) {
            insertAudioAd(monetizableTrack, ads.audioAd(), currentMonetizablePosition);
        }
    }

    public void applyInterstitialToTrack(Urn monetizableTrack, ApiAdsForTrack ads) {
        final int currentMonetizablePosition = playQueueManager.getPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");
        if (ads.hasInterstitialAd()) {
            applyInterstitialAd(ads.interstitialAd(), currentMonetizablePosition, monetizableTrack);
        }
    }

    private void insertAudioAd(Urn monetizableTrack, ApiAudioAd apiAudioAd, int currentMonetizablePosition) {
        PropertySet adMetaData = apiAudioAd
                .toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, monetizableTrack);


        if (apiAudioAd.hasApiLeaveBehind()) {
            insertAudioAdWithLeaveBehind(apiAudioAd, adMetaData, currentMonetizablePosition);
        } else {
            playQueueManager.performPlayQueueUpdateOperations(
                    new PlayQueueManager.InsertOperation(currentMonetizablePosition, apiAudioAd.getApiTrack().getUrn(), adMetaData, false)
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
                new PlayQueueManager.InsertOperation(currentMonetizablePosition, audioAdTrack, adMetaData, false),
                new PlayQueueManager.MergeMetadataOperation(newMonetizablePosition, leaveBehindProperties)
        );
    }

    public boolean isNextTrackAudioAd() {
       return playQueueManager.hasNextTrack() &&
               getMonetizableTrackMetaData().contains(AdProperty.AD_URN) &&
               getMonetizableTrackMetaData().get(AdProperty.AD_TYPE).equals(AdProperty.AD_TYPE_AUDIO);
    }

    public boolean isCurrentTrackAudioAd() {
        return isAudioAdAtPosition(playQueueManager.getCurrentPosition());
    }

    public boolean isAudioAdAtPosition(int position) {
        return !playQueueManager.isQueueEmpty() && position < playQueueManager.getQueueSize() &&
                playQueueManager.getMetaDataAt(position).contains(AdProperty.AD_URN) &&
                playQueueManager.getMetaDataAt(position).get(AdProperty.AD_TYPE).equals(AdProperty.AD_TYPE_AUDIO);
    }

    public void clearAllAds() {
        playQueueManager.removeTracksWithMetaData(hasAdUrn, PlayQueueEvent.fromAudioAdRemoved(playQueueManager.getCollectionUrn()));
    }

    public List<Urn> getAdUrnsInQueue() {
        return playQueueManager.filterTrackUrnsWithMetadata(hasAdUrn);
    }

    public PropertySet getMonetizableTrackMetaData() {
        final int monetizableTrackPosition = playQueueManager.getCurrentPosition() + 1;
        return playQueueManager.getMetaDataAt(monetizableTrackPosition);
    }

    private void applyInterstitialAd(ApiInterstitial interstitial, int currentMonetizablePosition, Urn monetizableTrack) {
        PropertySet interstitialPropertySet = interstitial.toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(TrackProperty.URN, monetizableTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.MergeMetadataOperation(currentMonetizablePosition, interstitialPropertySet)
        );
    }

}

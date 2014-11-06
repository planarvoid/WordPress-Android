package com.soundcloud.android.ads;

import static com.google.common.base.Preconditions.checkState;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;

public class AdsOperations {

    private static final String UNIQUE_ID_HEADER = "SC-UDID";
    private final TrackWriteStorage trackWriteStorage;
    private final DeviceHelper deviceHelper;
    private final PlayQueueManager playQueueManager;
    private final FeatureFlags featureFlags;
    private final ApiScheduler apiScheduler;
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
                fireAndForget(trackWriteStorage.storeTrackAsync(apiAdsForTrack.audioAd().getApiTrack()));
            }
        }
    };

    @Inject
    AdsOperations(TrackWriteStorage trackWriteStorage, DeviceHelper deviceHelper,
                  PlayQueueManager playQueueManager, FeatureFlags featureFlags, ApiScheduler apiScheduler) {
        this.trackWriteStorage = trackWriteStorage;
        this.deviceHelper = deviceHelper;
        this.playQueueManager = playQueueManager;
        this.featureFlags = featureFlags;
        this.apiScheduler = apiScheduler;
    }

    public Observable<ApiAdsForTrack> ads(Urn sourceUrn) {
        final String endpoint = String.format(ApiEndpoints.ADS.path(), sourceUrn.toEncodedString());
        final ApiRequest<ApiAdsForTrack> request = ApiRequest.Builder.<ApiAdsForTrack>get(endpoint)
                .forPrivateApi(1)
                .forResource(TypeToken.of(ApiAdsForTrack.class))
                .withHeader(UNIQUE_ID_HEADER, deviceHelper.getUniqueDeviceID())
                .build();

        return apiScheduler.mappedResponse(request).doOnNext(cacheAudioAdTrack);
    }

    public void applyAdToTrack(Urn monetizableTrack, ApiAdsForTrack ads) {
        final int currentMonetizablePosition = playQueueManager.getPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");
        if (ads.hasAudioAd()) {
            insertAudioAd(monetizableTrack, ads.audioAd(), currentMonetizablePosition);
        } else if (ads.hasInterstitialAd() && featureFlags.isEnabled(Feature.INTERSTITIAL)) {
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
                .getApiLeaveBehind()
                .toPropertySet()
                .put(AdOverlayProperty.META_AD_DISMISSED, false)
                .put(LeaveBehindProperty.AD_URN, adMetaData.get(AdProperty.AD_URN))
                .put(LeaveBehindProperty.AUDIO_AD_TRACK_URN, audioAdTrack);

        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.InsertOperation(currentMonetizablePosition, audioAdTrack, adMetaData, false),
                new PlayQueueManager.MergeMetadataOperation(newMonetizablePosition, leaveBehindProperties)
        );
    }

    public boolean isNextTrackAudioAd() {
        if (playQueueManager.hasNextTrack()) {
            return getMonetizableTrackMetaData().contains(AdProperty.AD_URN);
        }
        return false;
    }

    public boolean isCurrentTrackAudioAd() {
        return isAudioAdAtPosition(playQueueManager.getCurrentPosition());
    }

    public boolean isAudioAdAtPosition(int position) {
        return !playQueueManager.isQueueEmpty() && position < playQueueManager.getQueueSize() &&
                playQueueManager.getMetaDataAt(position).contains(AdProperty.AD_URN);
    }

    public void clearAllAds() {
        playQueueManager.removeTracksWithMetaData(hasAdUrn, PlayQueueEvent.fromAudioAdRemoved());
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

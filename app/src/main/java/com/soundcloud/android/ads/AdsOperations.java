package com.soundcloud.android.ads;

import static com.google.common.base.Preconditions.checkState;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;

public class AdsOperations {

    private static final String UNIQUE_ID_HEADER = "SC-UDID";
    private final RxHttpClient rxHttpClient;
    private final TrackWriteStorage trackWriteStorage;
    private final DeviceHelper deviceHelper;
    private final PlayQueueManager playQueueManager;
    private final FeatureFlags featureFlags;
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
    AdsOperations(RxHttpClient rxHttpClient, TrackWriteStorage trackWriteStorage, DeviceHelper deviceHelper, PlayQueueManager playQueueManager, FeatureFlags featureFlags) {
        this.rxHttpClient = rxHttpClient;
        this.trackWriteStorage = trackWriteStorage;
        this.deviceHelper = deviceHelper;
        this.playQueueManager = playQueueManager;
        this.featureFlags = featureFlags;
    }

    public Observable<ApiAdsForTrack> audioAd(Urn sourceUrn) {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), sourceUrn.toEncodedString());
        final APIRequest<ApiAdsForTrack> request = SoundCloudAPIRequest.RequestBuilder.<ApiAdsForTrack>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(ApiAdsForTrack.class))
                .withHeader(UNIQUE_ID_HEADER, deviceHelper.getUniqueDeviceID())
                .build();

        return rxHttpClient.<ApiAdsForTrack>fetchModels(request).doOnNext(cacheAudioAdTrack);
    }

    public void applyAdToTrack(Urn monetizableTrack, ApiAdsForTrack ads) {
        final int currentMonetizablePosition = playQueueManager.getPositionForUrn(monetizableTrack);
        checkState(currentMonetizablePosition != -1, "Failed to find the monetizable track");
        if (ads.hasInterstitialAd() && featureFlags.isEnabled(Feature.INTERSTITIAL)) {
            applyInterstitialAd(ads.interstitialAd(), currentMonetizablePosition);
        } else if (ads.hasAudioAd()) {
            insertAudioAd(monetizableTrack, ads.audioAd(), currentMonetizablePosition);
        }
    }

    public void insertAudioAd(Urn monetizableTrack, ApiAudioAd apiAudioAd, int currentMonetizablePosition) {
        PropertySet adMetaData = apiAudioAd
                .toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, monetizableTrack);

        final int newMonetizablePosition = currentMonetizablePosition + 1;
        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.InsertOperation(currentMonetizablePosition, apiAudioAd.getApiTrack().getUrn(), adMetaData, false),
                new PlayQueueManager.MergeMetadataOperation(newMonetizablePosition, apiAudioAd.getApiLeaveBehind().toPropertySet())
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

    private void applyInterstitialAd(ApiLeaveBehind interstitial, int currentMonetizablePosition) {
        PropertySet interstitialPropertySet = interstitial.toPropertySet();
        interstitialPropertySet.put(LeaveBehindProperty.IS_INTERSTITIAL, true);
        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.MergeMetadataOperation(currentMonetizablePosition, interstitialPropertySet)
        );
    }

}

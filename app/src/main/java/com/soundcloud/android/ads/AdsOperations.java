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
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;

public class AdsOperations {

    public static final String UNIQUE_ID_HEADER = "SC-UDID";
    private final RxHttpClient rxHttpClient;
    private final TrackWriteStorage trackWriteStorage;
    private final DeviceHelper deviceHelper;
    private final PlayQueueManager playQueueManager;
    private final Predicate<PropertySet> hasAdUrn = new Predicate<PropertySet>() {
        @Override
        public boolean apply(PropertySet input) {
            return input.contains(AdProperty.AD_URN);
        }
    };

    @Inject
    AdsOperations(RxHttpClient rxHttpClient, TrackWriteStorage trackWriteStorage, DeviceHelper deviceHelper, PlayQueueManager playQueueManager) {
        this.rxHttpClient = rxHttpClient;
        this.trackWriteStorage = trackWriteStorage;
        this.deviceHelper = deviceHelper;
        this.playQueueManager = playQueueManager;
    }

    private final Action1<AudioAd> cacheAudioAdTrack = new Action1<AudioAd>() {
        @Override
        public void call(AudioAd audioAd) {
            fireAndForget(trackWriteStorage.storeTrackAsync(audioAd.getApiTrack()));
        }
    };

    public Observable<AudioAd> audioAd(Urn sourceUrn) {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), sourceUrn.toEncodedString());
        final APIRequest<AudioAd> request = SoundCloudAPIRequest.RequestBuilder.<AudioAd>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(AudioAd.class))
                .withHeader(UNIQUE_ID_HEADER, deviceHelper.getUniqueDeviceID())
                .build();

        return rxHttpClient.<AudioAd>fetchModels(request).doOnNext(cacheAudioAdTrack);
    }

    public void insertAudioAd(Urn monetizableTrack, AudioAd audioAd){
        PropertySet adMetaData = audioAd
                .toPropertySet()
                .put(AdProperty.MONETIZABLE_TRACK_URN, monetizableTrack);

        final int insertPosition = playQueueManager.getPositionForUrn(monetizableTrack);
        checkState(insertPosition != -1, "Failed to find the monetizable track");
        final int monetizablePosition = insertPosition + 1;
        playQueueManager.performPlayQueueUpdateOperations(
                new PlayQueueManager.InsertOperation(insertPosition, audioAd.getApiTrack().getUrn(), adMetaData, false),
                new PlayQueueManager.MergeMetatdataOperation(monetizablePosition, audioAd.getLeaveBehind().toPropertySet())
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
}

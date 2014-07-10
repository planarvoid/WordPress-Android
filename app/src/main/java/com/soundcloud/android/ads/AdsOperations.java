package com.soundcloud.android.ads;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.track.TrackWriteStorage;
import rx.Observable;
import rx.functions.Action1;

import javax.inject.Inject;

class AdsOperations {

    private final RxHttpClient rxHttpClient;
    private final TrackWriteStorage trackWriteStorage;

    @Inject
    AdsOperations(RxHttpClient rxHttpClient, TrackWriteStorage trackWriteStorage) {
        this.rxHttpClient = rxHttpClient;
        this.trackWriteStorage = trackWriteStorage;
    }

    private final Action1<AudioAd> cacheAudioAdTrack = new Action1<AudioAd>() {
        @Override
        public void call(AudioAd audioAd) {
            fireAndForget(trackWriteStorage.storeTrackAsync(audioAd.getTrackSummary()));
        }
    };

    public Observable<AudioAd> audioAd(TrackUrn sourceUrn) {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), sourceUrn.toEncodedString());
        final APIRequest<AudioAd> request = SoundCloudAPIRequest.RequestBuilder.<AudioAd>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(AudioAd.class)).build();

        return rxHttpClient.<AudioAd>fetchModels(request).doOnNext(cacheAudioAdTrack);
    }
}

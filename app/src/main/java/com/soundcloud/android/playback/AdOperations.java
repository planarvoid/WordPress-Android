package com.soundcloud.android.playback;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.ads.AudioAd;
import com.soundcloud.android.storage.BulkStorage;
import rx.Observable;
import rx.functions.Action1;

public class AdOperations {

    private final RxHttpClient rxHttpClient;
    private final BulkStorage bulkStorage;

    public AdOperations(RxHttpClient rxHttpClient, BulkStorage bulkStorage) {
        this.rxHttpClient = rxHttpClient;
        this.bulkStorage = bulkStorage;
    }

    @Deprecated
    // matthias is working on write architecture for api-mobile models. This is temporary
    private final Action1<AudioAd> cacheAudioAdTrack = new Action1<AudioAd>() {
        @Override
        public void call(AudioAd audioAd) {
            final TrackSummary trackSummary = audioAd.getTrackSummary();
            fireAndForget(bulkStorage.bulkInsertAsync(Lists.transform(Lists.newArrayList(trackSummary), TrackSummary.TO_TRACK)));
        }
    };

    public Observable<AudioAd> getAudioAd(TrackUrn sourceUrn) {
        final String endpoint = String.format(APIEndpoints.AUDIO_AD.path(), sourceUrn.toEncodedString());
        final APIRequest<AudioAd> request = SoundCloudAPIRequest.RequestBuilder.<AudioAd>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(AudioAd.class)).build();

        return rxHttpClient.<AudioAd>fetchModels(request).doOnNext(cacheAudioAdTrack);
    }
}

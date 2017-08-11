package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;
import io.reactivex.Scheduler;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

public class PlayPublisher {

    private static final String TAG = "PlayPublisher";

    private final Resources resources;
    private final GcmStorage gcmStorage;
    private final DateProvider dateProvider;
    private final Scheduler scheduler;
    private final ApiClientRxV2 apiClient;

    @Inject
    PlayPublisher(Resources resources,
                         GcmStorage gcmStorage,
                         CurrentDateProvider dateProvider,
                         @Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                         ApiClientRxV2 apiClient) {
        this.resources = resources;
        this.gcmStorage = gcmStorage;
        this.dateProvider = dateProvider;
        this.scheduler = scheduler;
        this.apiClient = apiClient;
    }

    void onPlaybackStateChanged(PlayStateEvent stateTransition) {
        final Payload payload = createPayload(stateTransition);
        final ApiRequest apiRequest = ApiRequest
                .post(ApiEndpoints.PLAY_PUBLISH.path())
                .forPublicApi()
                .withContent(payload)
                .build();
        apiClient.response(apiRequest)
                 .subscribeOn(scheduler)
                 .subscribe(new ResponseLogger());
    }

    @NonNull
    private Payload createPayload(PlayStateEvent playStateEvent) {
        return new Payload(resources.getString(R.string.gcm_gateway_id),
                           gcmStorage.getToken(),
                           dateProvider.getCurrentTime(),
                           playStateEvent.getPlayingItemUrn());
    }

    private static class ResponseLogger extends DefaultSingleObserver<ApiResponse> {
        @Override
        public void onSuccess(ApiResponse response) {
            super.onSuccess(response);
            Log.d(TAG, "Posted play with response code " + response.getStatusCode());
        }
    }

    @SuppressWarnings("unused")
    static class Payload {
        @JsonProperty("gateway_id")
        public final String gatewayId;
        @JsonProperty("registration_id")
        public final String registrationId;
        public final long timestamp;
        public final Urn track;

        Payload(String gatewayId, String registrationId, long timestamp, Urn track) {
            this.gatewayId = gatewayId;
            this.registrationId = registrationId;
            this.timestamp = timestamp;
            this.track = track;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Payload)) return false;
            Payload payload = (Payload) o;
            return MoreObjects.equal(timestamp, payload.timestamp) &&
                    MoreObjects.equal(registrationId, payload.registrationId) &&
                    MoreObjects.equal(track, payload.track);
        }

        @Override
        public int hashCode() {
            return MoreObjects.hashCode(registrationId, timestamp, track);
        }
    }
}

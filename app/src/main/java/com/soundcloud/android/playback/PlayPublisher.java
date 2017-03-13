package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

public class PlayPublisher {

    private static final String TAG = "PlayPublisher";

    private final Resources resources;
    private final GcmStorage gcmStorage;
    private final DateProvider dateProvider;
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final ApiClient apiClient;

    private static final Func1<PlayStateEvent, Boolean> IS_PLAYER_PLAYING_A_TRACK =
            playStateEvent -> !playStateEvent.getPlayingItemUrn().isAd() && playStateEvent.isPlayerPlaying();

    private Func1<PlayStateEvent, Observable<ApiResponse>> toApiResponse = new Func1<PlayStateEvent, Observable<ApiResponse>>() {
        @Override
        public Observable<ApiResponse> call(final PlayStateEvent stateTransition) {
            return Observable
                    .defer(() -> {
                        final Payload payload = createPayload(stateTransition);
                        final ApiRequest apiRequest = ApiRequest
                                .post(ApiEndpoints.PLAY_PUBLISH.path())
                                .forPublicApi()
                                .withContent(payload)
                                .build();
                        return Observable.just(apiClient.fetchResponse(apiRequest));
                    })
                    .subscribeOn(scheduler);
        }
    };

    @Inject
    public PlayPublisher(Resources resources,
                         GcmStorage gcmStorage,
                         CurrentDateProvider dateProvider,
                         EventBus eventBus,
                         @Named(HIGH_PRIORITY) Scheduler scheduler,
                         ApiClient apiClient) {
        this.resources = resources;
        this.gcmStorage = gcmStorage;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.apiClient = apiClient;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(IS_PLAYER_PLAYING_A_TRACK)
                .flatMap(toApiResponse)
                .subscribe(new ResponseLogger());
    }

    @NonNull
    private Payload createPayload(PlayStateEvent playStateEvent) {
        return new Payload(resources.getString(R.string.gcm_gateway_id),
                           gcmStorage.getToken(),
                           dateProvider.getCurrentTime(),
                           playStateEvent.getPlayingItemUrn());
    }

    private static class ResponseLogger extends DefaultSubscriber<ApiResponse> {
        @Override
        public void onNext(ApiResponse response) {
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

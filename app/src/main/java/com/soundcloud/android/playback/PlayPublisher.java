package com.soundcloud.android.playback;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;

public class PlayPublisher {

    private static final String TAG = "PlayPublisher";

    private final GcmStorage gcmStorage;
    private final DateProvider dateProvider;
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private final ApiClientRx apiClient;

    private static final Func1<Player.StateTransition, Boolean> IS_PLAYER_PLAYING_EVENT =
            new Func1<Player.StateTransition, Boolean>() {
                @Override
                public Boolean call(Player.StateTransition stateTransition) {
                    return stateTransition.isPlayerPlaying();
                }
            };

    private Func1<Player.StateTransition, Observable<ApiResponse>> toApiResponse = new Func1<Player.StateTransition, Observable<ApiResponse>>() {
        @Override
        public Observable<ApiResponse> call(Player.StateTransition stateTransition) {
            final ApiRequest apiRequest = ApiRequest
                    .post(ApiEndpoints.PLAY_PUBLISH.path())
                    .forPublicApi()
                    .withContent(createPayload(stateTransition))
                    .build();

            return apiClient.response(apiRequest).subscribeOn(scheduler);
        }
    };

    @Inject
    public PlayPublisher(GcmStorage gcmStorage, DateProvider dateProvider, EventBus eventBus,
                         @Named(HIGH_PRIORITY) Scheduler scheduler, ApiClientRx apiClient) {
        this.gcmStorage = gcmStorage;
        this.dateProvider = dateProvider;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.apiClient = apiClient;
    }

    public void subscribe() {
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(IS_PLAYER_PLAYING_EVENT)
                .flatMap(toApiResponse)
                .subscribe(new ResponseLogger());
    }

    @NonNull
    private Payload createPayload(Player.StateTransition stateTransition) {
        return new Payload(
                gcmStorage.getToken(),
                dateProvider.getCurrentTime(),
                stateTransition.getTrackUrn());
    }

    private static class ResponseLogger extends DefaultSubscriber<ApiResponse> {
        @Override
        public void onNext(ApiResponse response) {
            Log.d(TAG, "Posted play with response code " + response.getStatusCode());
        }
    }

    @SuppressWarnings("unused")
    static class Payload {
        public final String gatewayId = "android/prod"; // this may move to properties if its variable
        public final String registrationId;
        public final long timestamp;
        public final Urn track;

        Payload(String registrationId, long timestamp, Urn track) {
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

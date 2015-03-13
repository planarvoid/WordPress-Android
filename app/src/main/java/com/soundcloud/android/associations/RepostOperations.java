package com.soundcloud.android.associations;

import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class RepostOperations {

    private final Action1<PropertySet> publishEntityStateChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newRepostState) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(newRepostState));
        }
    };

    private final RepostStorage repostStorage;
    private final ApiScheduler apiScheduler;
    private final Scheduler storageScheduler;
    private final EventBus eventBus;

    @Inject
    public RepostOperations(RepostStorage repostStorage, ApiScheduler apiScheduler,
                            @Named("Storage") Scheduler storageScheduler, EventBus eventBus) {
        this.repostStorage = repostStorage;
        this.apiScheduler = apiScheduler;
        this.storageScheduler = storageScheduler;
        this.eventBus = eventBus;
    }

    public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        if (addRepost) {
            return repostStorage.addRepost().toObservable(soundUrn)
                    .subscribeOn(storageScheduler)
                    .flatMap(sendApiRequest(buildAddRepostRequest(soundUrn)))
                    .map(returning(repostProperties(soundUrn, true)))
                    .doOnNext(publishEntityStateChanged)
                    .doOnError(rollbackRepost(soundUrn));
        } else {
            return repostStorage.removeRepost().toObservable(soundUrn)
                    .subscribeOn(storageScheduler)
                    .flatMap(sendApiRequest(buildRemoveRepostRequest(soundUrn)))
                    .map(returning(repostProperties(soundUrn, false)))
                    .doOnNext(publishEntityStateChanged)
                    .doOnError(rollbackRepostRemoval(soundUrn));
        }
    }

    private Action1<Throwable> rollbackRepost(final Urn soundUrn) {
        return new Action1<Throwable>(){
            @Override
            public void call(Throwable throwable) {
                repostStorage.removeRepost().call(soundUrn);
                publishEntityStateChanged.call(repostProperties(soundUrn, false));
            }
        };
    }

    private Action1<Throwable> rollbackRepostRemoval(final Urn soundUrn) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                repostStorage.addRepost().call(soundUrn);
                publishEntityStateChanged.call(repostProperties(soundUrn, true));
            }
        };
    }

    private PropertySet repostProperties(Urn soundUrn, boolean addRepost) {
        return PropertySet.from(
                PlayableProperty.URN.bind(soundUrn),
                PlayableProperty.IS_REPOSTED.bind(addRepost));
    }

    private Func1<WriteResult, Observable<ApiResponse>> sendApiRequest(final ApiRequest request) {
        return new Func1<WriteResult, Observable<ApiResponse>>() {
            @Override
            public Observable<ApiResponse> call(WriteResult ignored) {
                return apiScheduler.response(request);
            }
        };
    }

    private ApiRequest buildAddRepostRequest(Urn soundUrn) {
        return ApiRequest.Builder.put(getRepostEndpoint(soundUrn)
                .path(soundUrn.getNumericId()))
                .forPublicApi()
                .build();
    }

    private ApiRequest buildRemoveRepostRequest(Urn soundUrn) {
        return ApiRequest.Builder.delete(getRepostEndpoint(soundUrn)
                .path(soundUrn.getNumericId()))
                .forPublicApi()
                .build();
    }

    private ApiEndpoints getRepostEndpoint(Urn soundUrn) {
        return (soundUrn.isTrack() ? ApiEndpoints.MY_TRACK_REPOSTS : ApiEndpoints.MY_PLAYLIST_REPOSTS);
    }

}

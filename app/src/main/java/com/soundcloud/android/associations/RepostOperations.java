package com.soundcloud.android.associations;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

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
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final EventBus eventBus;

    @Inject
    RepostOperations(RepostStorage repostStorage, ApiClientRx apiClientRx,
                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler, EventBus eventBus) {
        this.repostStorage = repostStorage;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
    }

    public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        if (addRepost) {
            return repostStorage.addRepost().toObservable(soundUrn)
                    .subscribeOn(scheduler)
                    .flatMap(continueWith(pushAddRepost(soundUrn)))
                    .map(returning(repostProperties(soundUrn, true)))
                    .doOnNext(publishEntityStateChanged)
                    .doOnError(rollbackRepost(soundUrn));
        } else {
            return repostStorage.removeRepost().toObservable(soundUrn)
                    .subscribeOn(scheduler)
                    .flatMap(continueWith(pushRemoveRepost(soundUrn)))
                    .map(returning(repostProperties(soundUrn, false)))
                    .doOnNext(publishEntityStateChanged)
                    .doOnError(rollbackRepostRemoval(soundUrn));
        }
    }

    private Action1<Throwable> rollbackRepost(final Urn soundUrn) {
        return new Action1<Throwable>() {
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

    private Observable<ApiResponse> pushAddRepost(Urn soundUrn) {
        final ApiRequest request = ApiRequest.put(getRepostEndpoint(soundUrn)
                .path(soundUrn.getNumericId()))
                .forPublicApi()
                .build();
        return apiClientRx.response(request);
    }

    private Observable<ApiResponse> pushRemoveRepost(Urn soundUrn) {
        final ApiRequest request = ApiRequest.delete(getRepostEndpoint(soundUrn)
                .path(soundUrn.getNumericId()))
                .forPublicApi()
                .build();
        return apiClientRx.response(request);
    }

    private ApiEndpoints getRepostEndpoint(Urn soundUrn) {
        return (soundUrn.isTrack() ? ApiEndpoints.MY_TRACK_REPOSTS : ApiEndpoints.MY_PLAYLIST_REPOSTS);
    }

}

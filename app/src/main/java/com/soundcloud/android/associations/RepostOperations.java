package com.soundcloud.android.associations;

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
            return addRepostLocally(soundUrn).flatMap(pushAddRepostAndRevertWhenFailed());
        } else {
            return removeRepostLocally(soundUrn).flatMap(pushRemoveAndRevertWhenFailed());
        }
    }

    private Func1<PropertySet, Observable<PropertySet>> pushAddRepostAndRevertWhenFailed() {
        return new Func1<PropertySet, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(final PropertySet propertySet) {
                final Urn soundUrn = propertySet.get(PlayableProperty.URN);
                return pushAddRepost(soundUrn)
                        .map(returning(propertySet))
                        .onErrorResumeNext(removeRepostLocally(soundUrn));
            }
        };
    }

    private Func1<PropertySet, Observable<PropertySet>> pushRemoveAndRevertWhenFailed() {
        return new Func1<PropertySet, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(final PropertySet propertySet) {
                final Urn soundUrn = propertySet.get(PlayableProperty.URN);
                return pushRemoveRepost(soundUrn)
                        .map(returning(propertySet))
                        .onErrorResumeNext(addRepostLocally(soundUrn));
            }
        };
    }

    private Observable<PropertySet> addRepostLocally(Urn soundUrn) {
        return repostStorage.addRepost().toObservable(soundUrn)
                .subscribeOn(scheduler)
                .map(toRepostProperties(soundUrn, true))
                .doOnNext(publishEntityStateChanged)
                .doOnError(rollbackRepost(soundUrn, false));
    }

    private Observable<PropertySet> removeRepostLocally(Urn soundUrn) {
        return repostStorage.removeRepost().toObservable(soundUrn)
                .subscribeOn(scheduler)
                .map(toRepostProperties(soundUrn, false))
                .doOnNext(publishEntityStateChanged)
                .doOnError(rollbackRepost(soundUrn, true));
    }

    private Action1<Throwable> rollbackRepost(final Urn soundUrn, final boolean addRepost) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                PropertySet changeSet = PropertySet.from(
                        PlayableProperty.URN.bind(soundUrn),
                        PlayableProperty.IS_REPOSTED.bind(addRepost));
                publishEntityStateChanged.call(changeSet);
            }
        };
    }

    private Func1<Integer, PropertySet> toRepostProperties(final Urn soundUrn, final boolean addRepost) {
        return new Func1<Integer, PropertySet>() {
            @Override
            public PropertySet call(Integer repostCount) {
                return PropertySet.from(
                        PlayableProperty.URN.bind(soundUrn),
                        PlayableProperty.IS_REPOSTED.bind(addRepost),
                        PlayableProperty.REPOSTS_COUNT.bind(repostCount));
            }
        };
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

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
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;

public class RepostOperations implements RepostCreator {

    private final Action1<PropertySet> publishEntityStateChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet newRepostState) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(newRepostState));
        }
    };

    private final RepostStorage repostStorage;
    private final ApiScheduler apiScheduler;
    private final EventBus eventBus;

    @Inject
    public RepostOperations(RepostStorage repostStorage, ApiScheduler apiScheduler, EventBus eventBus) {
        this.repostStorage = repostStorage;
        this.apiScheduler = apiScheduler;
        this.eventBus = eventBus;
    }

    @Override
    public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        if (addRepost) {
            return repostStorage.addRepost(soundUrn)
                    .flatMap(createRemoteRepost(soundUrn))
                    .map(returning(repostProperties(soundUrn, true)))
                    .doOnNext(publishEntityStateChanged)
                    .onErrorResumeNext(rollbackRepost(soundUrn));
        } else {
            return Observable.never();
        }
    }

    private Observable<PropertySet> rollbackRepost(Urn soundUrn) {
        return repostStorage.removeRepost(soundUrn)
                .map(returning(repostProperties(soundUrn, false)))
                .doOnNext(publishEntityStateChanged);
    }

    private PropertySet repostProperties(Urn soundUrn, boolean addRepost) {
        return PropertySet.from(
                PlayableProperty.URN.bind(soundUrn),
                PlayableProperty.IS_REPOSTED.bind(addRepost));
    }

    private Func1<InsertResult, Observable<ApiResponse>> createRemoteRepost(final Urn soundUrn) {
        return new Func1<InsertResult, Observable<ApiResponse>>() {
            @Override
            public Observable<ApiResponse> call(InsertResult propertySet) {
                return apiScheduler.response(buildAddRepostRequest(soundUrn));
            }
        };
    }

    private ApiRequest buildAddRepostRequest(Urn soundUrn) {
        return ApiRequest.Builder.put(ApiEndpoints.MY_TRACK_REPOSTS
                .path(soundUrn.getNumericId()))
                .forPublicApi()
                .build();
    }

}

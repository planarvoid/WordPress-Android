package com.soundcloud.android.associations;

import static com.soundcloud.android.rx.RxUtils.returning;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class RepostOperations {

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

    public Observable<RepostStatus> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        if (addRepost) {
            return addRepostLocally(soundUrn).flatMap(pushAddRepostAndRevertWhenFailed());
        } else {
            return removeRepostLocally(soundUrn).flatMap(pushRemoveAndRevertWhenFailed());
        }
    }

    private Func1<RepostStatus, Observable<RepostStatus>> pushAddRepostAndRevertWhenFailed() {
        return repostStatus -> pushAddRepost(repostStatus.urn())
                .map(returning(repostStatus))
                .onErrorResumeNext(removeRepostLocally(repostStatus.urn()));
    }

    private Func1<RepostStatus, Observable<RepostStatus>> pushRemoveAndRevertWhenFailed() {
        return repostStatus -> pushRemoveRepost(repostStatus.urn())
                .map(returning(repostStatus))
                .onErrorResumeNext(addRepostLocally(repostStatus.urn()));
    }

    private Observable<RepostStatus> addRepostLocally(final Urn soundUrn) {
        return repostStorage.addRepost().toObservable(soundUrn)
                            .subscribeOn(scheduler)
                            .map(repostCount -> RepostStatus.createReposted(soundUrn, repostCount))
                            .doOnNext(repostStatus -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(repostStatus)))
                            .doOnError(throwable -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createUnposted(soundUrn)));
    }

    private Observable<RepostStatus> removeRepostLocally(final Urn soundUrn) {
        return repostStorage.removeRepost().toObservable(soundUrn)
                            .subscribeOn(scheduler)
                            .map(repostCount -> RepostStatus.createUnposted(soundUrn, repostCount))
                            .doOnNext(repostStatus -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(repostStatus)))
                            .doOnError(throwable -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createReposted(soundUrn)));
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

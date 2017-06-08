package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.model.Urn;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;

public class RepostOperations {

    public enum RepostResult {
        REPOST_SUCCEEDED, REPOST_FAILED, UNREPOST_SUCCEEDED, UNREPOST_FAILED
    }

    private final RepostStorage repostStorage;
    private final ApiClientRxV2 apiClientRx;
    private final Scheduler scheduler;
    private final EventBusV2 eventBus;

    @Inject
    RepostOperations(RepostStorage repostStorage, ApiClientRxV2 apiClientRx, @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler, EventBusV2 eventBus) {
        this.repostStorage = repostStorage;
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.eventBus = eventBus;
    }

    public Single<RepostResult> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        if (addRepost) {
            return addRepostLocally(soundUrn).flatMap(pushAddRepostAndRevertWhenFailed());
        } else {
            return removeRepostLocally(soundUrn).flatMap(pushRemoveAndRevertWhenFailed());
        }
    }

    private Function<RepostStatus, Single<RepostResult>> pushAddRepostAndRevertWhenFailed() {
        return repostStatus -> pushAddRepost(repostStatus.urn())
                .map(o -> RepostResult.REPOST_SUCCEEDED)
                .onErrorResumeNext(removeRepostLocally(repostStatus.urn())
                                           .map(repostStatus1 -> RepostResult.REPOST_FAILED));
    }

    private Function<RepostStatus, Single<RepostResult>> pushRemoveAndRevertWhenFailed() {
        return repostStatus -> pushRemoveRepost(repostStatus.urn())
                .map(o -> RepostResult.UNREPOST_SUCCEEDED)
                .onErrorResumeNext(addRepostLocally(repostStatus.urn())
                                           .map(repostStatus1 -> RepostResult.UNREPOST_FAILED));
    }

    private Single<RepostStatus> addRepostLocally(final Urn soundUrn) {
        return repostStorage.addRepost().toSingle(soundUrn)
                            .subscribeOn(scheduler)
                            .map(repostCount -> RepostStatus.createReposted(soundUrn, repostCount))
                            .doOnSuccess(repostStatus -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(repostStatus)))
                            .doOnError(throwable -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createUnposted(soundUrn)));
    }

    private Single<RepostStatus> removeRepostLocally(final Urn soundUrn) {
        return repostStorage.removeRepost().toSingle(soundUrn)
                            .subscribeOn(scheduler)
                            .map(repostCount -> RepostStatus.createUnposted(soundUrn, repostCount))
                            .doOnSuccess(repostStatus -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(repostStatus)))
                            .doOnError(throwable -> eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.createReposted(soundUrn)));
    }

    private Single<ApiResponse> pushAddRepost(Urn soundUrn) {
        final ApiRequest request = ApiRequest.put(getRepostEndpoint(soundUrn)
                                                          .path(soundUrn.getNumericId()))
                                             .forPublicApi()
                                             .build();
        return apiClientRx.response(request);
    }

    private Single<ApiResponse> pushRemoveRepost(Urn soundUrn) {
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

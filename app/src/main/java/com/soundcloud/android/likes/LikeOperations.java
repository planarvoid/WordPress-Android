package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class LikeOperations {

    private final UpdateLikeCommand storeLikeCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;

    public enum LikeResult {
        LIKE_SUCCEEDED, LIKE_FAILED, UNLIKE_SUCCEEDED, UNLIKE_FAILED
    }

    @Inject
    public LikeOperations(UpdateLikeCommand storeLikeCommand,
                          SyncInitiator syncInitiator,
                          EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeLikeCommand = storeLikeCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Observable<LikeResult> toggleLike(final Urn targetUrn, final boolean addLike) {
        final UpdateLikeParams params = new UpdateLikeParams(targetUrn, addLike);
        return storeLikeCommand
                .toObservable(params)
                .map(likesCount -> LikesStatusEvent.create(targetUrn, addLike, likesCount))
                .doOnNext(likesStatusEvent -> eventBus.publish(EventQueue.LIKE_CHANGED, likesStatusEvent))
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler)
                .map(likesStatusEvent -> addLike ? LikeResult.LIKE_SUCCEEDED : LikeResult.UNLIKE_SUCCEEDED)
                .onErrorReturn(throwable -> addLike ? LikeResult.LIKE_FAILED : LikeResult.UNLIKE_FAILED);

    }

}

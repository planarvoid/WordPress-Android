package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import javax.inject.Inject;
import javax.inject.Named;

public class LikeOperations {

    private final UpdateLikeCommand storeLikeCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBusV2 eventBus;

    public enum LikeResult {
        LIKE_SUCCEEDED, LIKE_FAILED, UNLIKE_SUCCEEDED, UNLIKE_FAILED
    }

    @Inject
    public LikeOperations(UpdateLikeCommand storeLikeCommand,
                          SyncInitiator syncInitiator,
                          EventBusV2 eventBus,
                          @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.storeLikeCommand = storeLikeCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Single<LikeResult> toggleLike(final Urn targetUrn, final boolean addLike) {
        final UpdateLikeParams params = new UpdateLikeParams(targetUrn, addLike);
        return storeLikeCommand
                .toSingle(params)
                .flatMap(likesCount -> syncInitiator.requestSystemSync().toSingle(() -> LikesStatusEvent.create(targetUrn, addLike, likesCount)))
                .doOnSuccess(likesStatusEvent -> eventBus.publish(EventQueue.LIKE_CHANGED, likesStatusEvent))
                .subscribeOn(scheduler)
                .map(likesStatusEvent -> addLike ? LikeResult.LIKE_SUCCEEDED : LikeResult.UNLIKE_SUCCEEDED)
                .onErrorReturn(throwable -> addLike ? LikeResult.LIKE_FAILED : LikeResult.UNLIKE_FAILED);

    }

    public Disposable toggleLikeAndForget(final Urn targetUrn, final boolean addLike) {
        return toggleLike(targetUrn, addLike).subscribeWith(new DefaultSingleObserver<>());
    }
}

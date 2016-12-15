package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.java.collections.PropertySet;
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

    public Observable<PropertySet> toggleLike(final Urn targetUrn, final boolean addLike) {
        final UpdateLikeParams params = new UpdateLikeParams(targetUrn, addLike);
        return storeLikeCommand
                .toObservable(params)
                .doOnNext(likesCount -> eventBus.publish(EventQueue.LIKE_CHANGED, LikesStatusEvent.create(targetUrn, addLike, likesCount)))
                .map(newLikesCount -> PropertySet.from(
                        PlayableProperty.URN.bind(targetUrn),
                        PlayableProperty.LIKES_COUNT.bind(newLikesCount),
                        PlayableProperty.IS_USER_LIKE.bind(addLike)))
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

}

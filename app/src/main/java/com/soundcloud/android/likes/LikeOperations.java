package com.soundcloud.android.likes;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

public class LikeOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final UpdateLikeCommand storeLikeCommand;
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;


    private final Action1<PropertySet> publishPlayableChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet changeSet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromLike(changeSet));
        }
    };

    @Inject
    public LikeOperations(UpdateLikeCommand storeLikeCommand,
                          SyncInitiator syncInitiator,
                          EventBus eventBus,
                          @Named("Storage") Scheduler scheduler) {
        this.storeLikeCommand = storeLikeCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Observable<PropertySet> addLike(final PropertySet sound) {
        sound.put(PlayableProperty.IS_LIKED, true);
        Date likeTime = new Date();
        sound.put(LikeProperty.CREATED_AT, likeTime);
        sound.put(LikeProperty.ADDED_AT, likeTime);
        return toggleLike(sound);
    }

    public Observable<PropertySet> removeLike(final PropertySet sound) {
        sound.put(PlayableProperty.IS_LIKED, false);
        Date unlikeTime = new Date();
        sound.put(LikeProperty.CREATED_AT, unlikeTime);
        sound.put(LikeProperty.REMOVED_AT, unlikeTime);
        return toggleLike(sound);
    }

    private Observable<PropertySet> toggleLike(PropertySet likeProperties) {
        return storeLikeCommand
                .with(likeProperties)
                .toObservable()
                .doOnNext(publishPlayableChanged)
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

}

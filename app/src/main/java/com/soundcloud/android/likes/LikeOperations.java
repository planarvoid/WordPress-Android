package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;

public class LikeOperations {

    private final UpdateLikeCommand storeLikeCommand;
    private final Scheduler scheduler;
    private final LegacySyncInitiator syncInitiator;
    private final EventBus eventBus;


    private final Action1<PropertySet> publishPlayableChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet changeSet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                    EntityStateChangedEvent.fromLike(Collections.singletonList(changeSet)));
        }
    };

    @Inject
    public LikeOperations(UpdateLikeCommand storeLikeCommand,
                          LegacySyncInitiator syncInitiator,
                          EventBus eventBus,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeLikeCommand = storeLikeCommand;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
        this.syncInitiator = syncInitiator;
    }

    public Observable<PropertySet> toggleLike(Urn targetUrn, boolean addLike) {
        final UpdateLikeParams params = new UpdateLikeParams(targetUrn, addLike);
        return storeLikeCommand
                .toObservable(params)
                .map(toChangeSet(targetUrn, addLike))
                .doOnNext(publishPlayableChanged)
                .doOnCompleted(syncInitiator.requestSystemSyncAction())
                .subscribeOn(scheduler);
    }

    private Func1<Integer, PropertySet> toChangeSet(final Urn targetUrn, final boolean addLike) {
        return new Func1<Integer, PropertySet>() {
            @Override
            public PropertySet call(Integer newLikesCount) {
                return PropertySet.from(
                        PlayableProperty.URN.bind(targetUrn),
                        PlayableProperty.LIKES_COUNT.bind(newLikesCount),
                        PlayableProperty.IS_USER_LIKE.bind(addLike));
            }
        };
    }
}

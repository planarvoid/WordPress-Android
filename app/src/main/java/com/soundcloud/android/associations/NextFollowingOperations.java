package com.soundcloud.android.associations;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class is "work in progress", and it's meant to replace {@link FollowingOperations}
 */
public class NextFollowingOperations {
    private final Scheduler scheduler;
    private final SyncInitiator syncInitiator;
    private final EventBus eventBus;
    private final UpdateFollowingCommand storeFollowingCommand;

    private final Action1<PropertySet> publishFollowingChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet changeSet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromFollowing(changeSet));
        }
    };

    private final Action0 syncFollowings = new Action0() {
        @Override
        public void call() {
            syncInitiator.pushFollowingsToApi();
        }
    };

    @Inject
    public NextFollowingOperations(SyncInitiator syncInitiator,
                                   EventBus eventBus,
                                   UpdateFollowingCommand storeFollowingCommand,
                                   @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.storeFollowingCommand = storeFollowingCommand;
        this.scheduler = scheduler;
    }

    public Observable<PropertySet> toggleFollowing(Urn targetUrn, boolean following) {
        final UpdateFollowingCommand.UpdateFollowingParams params =
                new UpdateFollowingCommand.UpdateFollowingParams(targetUrn, following);

        return storeFollowingCommand
                .toObservable(params)
                .map(toChangeSet(targetUrn, following))
                .doOnNext(publishFollowingChanged)
                .doOnCompleted(syncFollowings)
                .subscribeOn(scheduler);
    }

    private Func1<Integer, PropertySet> toChangeSet(final Urn targetUrn, final boolean following) {
        return new Func1<Integer, PropertySet>() {
            @Override
            public PropertySet call(Integer newFollowersCount) {
                return PropertySet.from(
                        UserProperty.URN.bind(targetUrn),
                        UserProperty.FOLLOWERS_COUNT.bind(newFollowersCount),
                        UserProperty.IS_FOLLOWED_BY_ME.bind(following));
            }
        };
    }
}

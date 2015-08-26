package com.soundcloud.android.associations;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.users.UserAssociationStorage;
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
    private final UserAssociationStorage userAssociationStorage;

    private final Action1<PropertySet> publishFollowingChanged = new Action1<PropertySet>() {
        @Override
        public void call(PropertySet changeSet) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromFollowing(changeSet));
        }
    };

    private final Func1<Urn, Observable<PropertySet>> loadFollowedUser = new Func1<Urn, Observable<PropertySet>>() {
        @Override
        public Observable<PropertySet> call(Urn urn) {
            return userAssociationStorage.loadFollowing(urn);
        }
    };

    private final Action0 syncFollowings = new Action0() {
        @Override
        public void call() {
            syncInitiator.pushFollowingsToApi();
        }
    };

    private static final Func1<EntityStateChangedEvent, Boolean> IS_FOLLOWING_EVENT = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.FOLLOWING &&
                    event.isSingularChange() &&
                    event.getNextChangeSet().getOrElse(UserProperty.IS_FOLLOWED_BY_ME, false);
        }
    };

    private static final Func1<EntityStateChangedEvent, Boolean> IS_UNFOLLOW_EVENT = new Func1<EntityStateChangedEvent, Boolean>() {
        @Override
        public Boolean call(EntityStateChangedEvent event) {
            return event.getKind() == EntityStateChangedEvent.FOLLOWING &&
                    event.isSingularChange() &&
                    !event.getNextChangeSet().getOrElse(UserProperty.IS_FOLLOWED_BY_ME, true);
        }
    };

    @Inject
    public NextFollowingOperations(SyncInitiator syncInitiator,
                                   EventBus eventBus,
                                   UpdateFollowingCommand storeFollowingCommand,
                                   @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                                   UserAssociationStorage userAssociationStorage) {
        this.syncInitiator = syncInitiator;
        this.eventBus = eventBus;
        this.storeFollowingCommand = storeFollowingCommand;
        this.scheduler = scheduler;
        this.userAssociationStorage = userAssociationStorage;
    }

    public Observable<PropertySet> onUserFollowed() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(IS_FOLLOWING_EVENT)
                .map(EntityStateChangedEvent.TO_URN)
                .flatMap(loadFollowedUser);
    }

    public Observable<Urn> onUserUnfollowed() {
        return eventBus.queue(ENTITY_STATE_CHANGED)
                .filter(IS_UNFOLLOW_EVENT)
                .map(EntityStateChangedEvent.TO_URN);
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

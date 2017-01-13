package com.soundcloud.android.associations;

import static com.soundcloud.android.events.EventQueue.FOLLOWING_CHANGED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class is "work in progress", and it's meant to replace {@link FollowingOperations}
 */
public class FollowingOperations {
    private final Scheduler scheduler;
    private final EventBus eventBus;
    private final UpdateFollowingCommand storeFollowingCommand;
    private final UserAssociationStorage userAssociationStorage;
    private final SyncOperations syncOperations;

    private final Func1<Urn, Observable<UserItem>> loadFollowedUser = new Func1<Urn, Observable<UserItem>>() {
        @Override
        public Observable<UserItem> call(Urn urn) {
            return userAssociationStorage.followedUser(urn).subscribeOn(scheduler);
        }
    };

    @Inject
    public FollowingOperations(EventBus eventBus,
                               UpdateFollowingCommand storeFollowingCommand,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               UserAssociationStorage userAssociationStorage,
                               SyncOperations syncOperations) {
        this.eventBus = eventBus;
        this.storeFollowingCommand = storeFollowingCommand;
        this.scheduler = scheduler;
        this.userAssociationStorage = userAssociationStorage;
        this.syncOperations = syncOperations;
    }

    public Observable<UserItem> populatedOnUserFollowed() {
        return onUserFollowed().flatMap(loadFollowedUser);
    }

    public Observable<Urn> onUserFollowed() {
        return eventBus.queue(FOLLOWING_CHANGED)
                       .filter(FollowingStatusEvent::isFollowed)
                       .map(FollowingStatusEvent::urn);
    }

    public Observable<Urn> onUserUnfollowed() {
        return eventBus.queue(FOLLOWING_CHANGED)
                       .filter((event) -> !event.isFollowed())
                       .map(FollowingStatusEvent::urn);
    }

    public Observable<FollowingStatusEvent> toggleFollowing(final Urn targetUrn, final boolean following) {
        final UpdateFollowingCommand.UpdateFollowingParams params =
                new UpdateFollowingCommand.UpdateFollowingParams(targetUrn, following);

        return storeFollowingCommand
                .toObservable(params)
                .flatMap(followingCount -> syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)
                                                         .map(result -> following ?
                                                                        FollowingStatusEvent.createFollowed(targetUrn, followingCount) :
                                                                        FollowingStatusEvent.createUnfollowed(targetUrn, followingCount)))
                .doOnNext(event -> eventBus.publish(EventQueue.FOLLOWING_CHANGED, event))
                .subscribeOn(scheduler);
    }

}

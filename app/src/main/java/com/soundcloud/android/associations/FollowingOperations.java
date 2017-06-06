package com.soundcloud.android.associations;

import static com.soundcloud.android.events.EventQueue.FOLLOWING_CHANGED;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FollowingStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class is "work in progress", and it's meant to replace {@link FollowingOperations}
 */
public class FollowingOperations {
    private final Scheduler scheduler;
    private final EventBusV2 eventBus;
    private final UpdateFollowingCommand storeFollowingCommand;
    private final UserAssociationStorage userAssociationStorage;
    private final EntityItemCreator entityItemCreator;
    private final NewSyncOperations syncOperations;

    @Inject
    public FollowingOperations(EventBusV2 eventBus,
                               UpdateFollowingCommand storeFollowingCommand,
                               @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                               UserAssociationStorage userAssociationStorage,
                               EntityItemCreator entityItemCreator,
                               NewSyncOperations syncOperations) {
        this.eventBus = eventBus;
        this.storeFollowingCommand = storeFollowingCommand;
        this.scheduler = scheduler;
        this.userAssociationStorage = userAssociationStorage;
        this.entityItemCreator = entityItemCreator;
        this.syncOperations = syncOperations;
    }

    public Observable<UserItem> populatedOnUserFollowed() {
        return onUserFollowed().flatMap(urn -> userAssociationStorage.followedUser(urn)
                                                                     .map(entityItemCreator::userItem)
                                                                     .subscribeOn(scheduler)
                                                                     .toObservable());
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

    public Completable toggleFollowing(final Urn targetUrn, final boolean following) {
        final UpdateFollowingCommand.UpdateFollowingParams params =
                new UpdateFollowingCommand.UpdateFollowingParams(targetUrn, following);

        return storeFollowingCommand
                .toSingle(params)
                .flatMap(followingCount -> syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)
                                                         .map(result -> following ?
                                                                        FollowingStatusEvent.createFollowed(targetUrn, followingCount) :
                                                                        FollowingStatusEvent.createUnfollowed(targetUrn, followingCount)))
                .doOnSuccess(event -> eventBus.publish(EventQueue.FOLLOWING_CHANGED, event))
                .subscribeOn(scheduler)
                .toCompletable();
    }

}

package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.rx.Pager;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class MyProfileOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final SyncInitiatorBridge syncInitiatorBridge;
    private final SyncInitiator syncInitiator;

    private final UserAssociationStorage userAssociationStorage;
    private final PostsStorage postsStorage;
    private final Scheduler scheduler;

    @Inject
    public MyProfileOperations(
            PostsStorage postsStorage,
            SyncInitiatorBridge syncInitiatorBridge,
            SyncInitiator syncInitiator,
            UserAssociationStorage userAssociationStorage,
            @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {

        this.postsStorage = postsStorage;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.syncInitiator = syncInitiator;
        this.userAssociationStorage = userAssociationStorage;
        this.scheduler = scheduler;
    }

    Single<List<Following>> followings() {
        return pagedFollowingsFromPosition(Consts.NOT_SET)
                .subscribeOn(scheduler)
                .flatMap(list -> list.isEmpty() ? updatedFollowings() : Single.just(list));
    }

    Pager.PagingFunction<List<Following>> followingsPagingFunction() {
        return result -> {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return RxJava.toV1Observable(pagedFollowingsFromPosition(getLast(result).userAssociation().position()).subscribeOn(scheduler));
            }
        };
    }

    Single<List<Following>> updatedFollowings() {
        return RxJava.toV2Single(syncInitiatorBridge.refreshFollowings())
                     .flatMap(__ -> pagedFollowingsFromPosition(Consts.NOT_SET).subscribeOn(scheduler));
    }

    public Single<List<UserAssociation>> followingsUserAssociations() {
        return loadFollowingUserAssociationsFromStorage()
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(Single.defer(() -> RxJava.toV2Single(syncInitiatorBridge.refreshFollowings())
                                                        .flatMap(o -> loadFollowingUserAssociationsFromStorage()))
                                     .toMaybe())
                .toSingle(Collections.emptyList());
    }

    private Single<List<UserAssociation>> loadFollowingUserAssociationsFromStorage() {
        return userAssociationStorage.followedUserAssociations().subscribeOn(scheduler);
    }

    private Single<List<Following>> pagedFollowingsFromPosition(long fromPosition) {
        return userAssociationStorage
                .followedUserUrns(PAGE_SIZE, fromPosition)
                .flatMap(syncAndReloadFollowings(PAGE_SIZE, fromPosition));
    }

    @NonNull
    private Function<List<Urn>, Single<List<Following>>> syncAndReloadFollowings(final int pageSize,
                                                                                 final long fromPosition) {
        return urns -> {
            if (urns.isEmpty()) {
                return Single.just(Collections.emptyList());
            } else {
                return RxJava.toV2Single(syncInitiator.batchSyncUsers(urns))
                             .flatMap(__ -> userAssociationStorage.followedUsers(pageSize, fromPosition).subscribeOn(scheduler));
            }
        };
    }

    public Observable<LastPostedTrack> lastPublicPostedTrack() {
        return postsStorage.loadLastPublicPostedTrack()
                           .subscribeOn(scheduler);
    }
}

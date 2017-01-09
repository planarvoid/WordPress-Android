package com.soundcloud.android.profile;

import static com.soundcloud.android.rx.RxUtils.IS_NOT_EMPTY_LIST;
import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.users.UserAssociation;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;

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

    private final Func1<Object, Observable<List<PropertySet>>> loadInitialFollowings = new Func1<Object, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Object ignored) {
            return pagedFollowingsFromPosition(Consts.NOT_SET).subscribeOn(scheduler);
        }
    };

    @Inject
    public MyProfileOperations(
            PostsStorage postsStorage,
            SyncInitiatorBridge syncInitiatorBridge,
            SyncInitiator syncInitiator,
            UserAssociationStorage userAssociationStorage,
            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {

        this.postsStorage = postsStorage;
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.syncInitiator = syncInitiator;
        this.userAssociationStorage = userAssociationStorage;
        this.scheduler = scheduler;
    }

    Observable<List<PropertySet>> pagedFollowings() {
        return pagedFollowingsFromPosition(Consts.NOT_SET)
                .subscribeOn(scheduler)
                .filter(IS_NOT_EMPTY_LIST)
                .switchIfEmpty(updatedFollowings());
    }

    Pager.PagingFunction<List<PropertySet>> followingsPagingFunction() {
        return result -> {
            if (result.size() < PAGE_SIZE) {
                return Pager.finish();
            } else {
                return pagedFollowingsFromPosition(getLast(result).get(UserAssociationProperty.POSITION))
                        .subscribeOn(scheduler);
            }
        };
    }

    Observable<List<PropertySet>> updatedFollowings() {
        return syncInitiatorBridge.refreshFollowings()
                                  .flatMap(loadInitialFollowings);
    }

    public Observable<List<UserAssociation>> followingsUserAssociations() {
        return loadFollowingUserAssociationsFromStorage()
                .filter(IS_NOT_EMPTY_LIST)
                .switchIfEmpty(Observable.defer(() -> syncInitiatorBridge.refreshFollowings()
                                                                 .flatMap(continueWith(
                                                  loadFollowingUserAssociationsFromStorage()))));
    }

    private Observable<List<UserAssociation>> loadFollowingUserAssociationsFromStorage() {
        return userAssociationStorage.followedUserAssociations().subscribeOn(scheduler);
    }

    private Observable<List<PropertySet>> pagedFollowingsFromPosition(long fromPosition) {
        return userAssociationStorage
                .followedUserUrns(PAGE_SIZE, fromPosition)
                .flatMap(syncAndReloadFollowings(PAGE_SIZE, fromPosition));
    }

    @NonNull
    private Func1<List<Urn>, Observable<List<PropertySet>>> syncAndReloadFollowings(final int pageSize,
                                                                                    final long fromPosition) {
        return urns -> {
            if (urns.isEmpty()) {
                return Observable.just(Collections.<PropertySet>emptyList());
            } else {
                return syncInitiator.batchSyncUsers(urns)
                                    .flatMap(loadFollowings(pageSize, fromPosition));
            }
        };
    }

    @NonNull
    private Func1<SyncJobResult, Observable<List<PropertySet>>> loadFollowings(final int pageSize,
                                                                               final long fromPosition) {
        return syncJobResult -> userAssociationStorage.followedUsers(pageSize, fromPosition).subscribeOn(scheduler);
    }

    public Observable<PropertySet> lastPublicPostedTrack() {
        return postsStorage.loadLastPublicPostedTrack()
                           .subscribeOn(scheduler);
    }
}

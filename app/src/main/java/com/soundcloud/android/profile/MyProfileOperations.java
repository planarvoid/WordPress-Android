package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MyProfileOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final NetworkConnectionHelper networkConnectionHelper;
    private final SyncStateStorage syncStateStorage;
    private final SyncInitiator syncInitiator;

    private final PlaylistPostStorage playlistPostStorage;
    private final UserAssociationStorage userAssociationStorage;
    private final LikesStorage likesStorage;
    private final PostsStorage postsStorage;
    private final Scheduler scheduler;

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialFollowings = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean ignored) {
            return pagedFollowingsFromPosition(Consts.NOT_SET)
                    .subscribeOn(scheduler);
        }
    };

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean ignored) {
            return postsStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)
                    .subscribeOn(scheduler);
        }
    };

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPlaylistPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean ignored) {
            return initialPlaylistPage();
        }
    };

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialLikes = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean ignored) {
            return likesStorage.loadLikes(PAGE_SIZE, Long.MAX_VALUE)
                    .subscribeOn(scheduler);
        }
    };

    private final Action1<List<PropertySet>> syncPlaylistMetadata = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            if (networkConnectionHelper.isWifiConnected() && !propertySets.isEmpty()) {
                syncInitiator.requestPlaylistSync(propertySets);
            }
        }
    };

    @Inject
    public MyProfileOperations(
            LikesStorage likesStorage,
            PostsStorage postsStorage,
            PlaylistPostStorage playlistPostStorage,
            SyncStateStorage syncStateStorage,
            SyncInitiator syncInitiator,
            NetworkConnectionHelper networkConnectionHelper,
            UserAssociationStorage userAssociationStorage,
            @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {

        this.likesStorage = likesStorage;
        this.postsStorage = postsStorage;
        this.playlistPostStorage = playlistPostStorage;
        this.syncStateStorage = syncStateStorage;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
        this.userAssociationStorage = userAssociationStorage;
        this.scheduler = scheduler;
    }

    Observable<List<PropertySet>> pagedFollowings() {
        return pagedFollowingsFromPosition(Consts.NOT_SET)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedFollowings()));
    }

    Pager.PagingFunction<List<PropertySet>> followingsPagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return pagedFollowingsFromPosition(getLast(result).get(UserAssociationProperty.POSITION))
                            .subscribeOn(scheduler);
                }
            }
        };
    }

    Observable<List<PropertySet>> updatedFollowings() {
        return syncInitiator.refreshFollowings()
                .flatMap(loadInitialFollowings);
    }

    private Observable<List<PropertySet>> pagedFollowingsFromPosition(long fromPosition) {
        return userAssociationStorage
                .loadFollowingsUrns(PAGE_SIZE, fromPosition)
                .flatMap(syncAndReloadFollowings(PAGE_SIZE, fromPosition));
    }

    @NonNull
    private Func1<List<Urn>, Observable<List<PropertySet>>> syncAndReloadFollowings(final int pageSize, final long fromPosition) {
        return new Func1<List<Urn>, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(List<Urn> urns) {
                if (urns.isEmpty()){
                    return Observable.just(Collections.<PropertySet>emptyList());
                } else {
                    return syncInitiator.syncUsers(urns)
                            .flatMap(loadFollowings(pageSize, fromPosition));
                }
            }
        };
    }

    @NonNull
    private Func1<SyncResult, Observable<List<PropertySet>>> loadFollowings(final int pageSize, final long fromPosition) {
        return new Func1<SyncResult, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(SyncResult syncResult) {
                return userAssociationStorage.loadFollowings(pageSize, fromPosition).subscribeOn(scheduler);
            }
        };
    }

    Observable<List<Urn>> likesForPlayback() {
        return likesStorage.loadLikesForPlayback().subscribeOn(scheduler);
    }

    Pager.PagingFunction<List<PropertySet>> likesPagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return likedItems(getLast(result).get(LikeProperty.CREATED_AT).getTime());
                }
            }
        };
    }

    private Observable<List<PropertySet>> likedItems(long beforeTime) {
        return likesStorage.loadLikes(PAGE_SIZE, beforeTime)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedLikes()));
    }

    Observable<List<PropertySet>> updatedLikes() {
        return syncInitiator.refreshLikes()
                .flatMap(loadInitialLikes);
    }

    Observable<List<PropertySet>> pagedLikes() {
        return likedItems(Long.MAX_VALUE);
    }

    Observable<List<PropertySet>> pagedPostItems() {
        return postedItems(Long.MAX_VALUE);
    }

    Pager.PagingFunction<List<PropertySet>> postsPagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return postedItems(getLast(result).get(PostProperty.CREATED_AT).getTime());
                }
            }
        };
    }

    Observable<List<PropertySet>> postsForPlayback() {
        return postsStorage.loadPostsForPlayback().subscribeOn(scheduler);
    }

    Observable<List<PropertySet>> updatedPosts() {
        return syncInitiator.refreshPosts()
                .flatMap(loadInitialPosts);
    }

    private Observable<List<PropertySet>> postedItems(final long beforeTime) {
        return syncStateStorage.hasSyncedMyPostsBefore()
                .flatMap(new Func1<Boolean, Observable<List<PropertySet>>>() {
                    @Override
                    public Observable<List<PropertySet>> call(Boolean hasSynced) {
                        return hasSynced ? postsStorage.loadPosts(PAGE_SIZE, beforeTime) : updatedPosts();
                    }
                }).subscribeOn(scheduler);
    }

    Observable<List<PropertySet>> pagedPlaylistItems() {
        return initialPlaylistPage()
                .lift(new OperatorSwitchOnEmptyList<>(updatedPlaylists()));
    }

    Pager.PagingFunction<List<PropertySet>> playlistPagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    Date oldestPlaylistDate = getLast(result).get(PostProperty.CREATED_AT);
                    return playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, oldestPlaylistDate.getTime())
                            .doOnNext(syncPlaylistMetadata)
                            .subscribeOn(scheduler);
                }
            }
        };
    }

    Observable<List<PropertySet>> updatedPlaylists() {
        return syncInitiator.refreshPostedPlaylists()
                .flatMap(loadInitialPlaylistPosts);
    }

    private Observable<List<PropertySet>> initialPlaylistPage() {
        return playlistPostStorage.loadPostedPlaylists(PAGE_SIZE, Long.MAX_VALUE)
                .doOnNext(syncPlaylistMetadata)
                .subscribeOn(scheduler);
    }
}

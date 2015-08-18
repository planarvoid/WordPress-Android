package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;

public class MyProfileOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PostsStorage postsStorage;
    private final PlaylistPostStorage playlistPostStorage;
    private final SyncInitiator syncInitiator;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final Scheduler scheduler;

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean aBoolean) {
            return postsStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)
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

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPlaylistPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean aBoolean) {
            return initialPlaylistPage();
        }
    };

    @Inject
    public MyProfileOperations(PostsStorage postsStorage,
                               PlaylistPostStorage playlistPostStorage,
                               SyncInitiator syncInitiator,
                               NetworkConnectionHelper networkConnectionHelper, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.postsStorage = postsStorage;
        this.playlistPostStorage = playlistPostStorage;
        this.syncInitiator = syncInitiator;
        this.networkConnectionHelper = networkConnectionHelper;
        this.scheduler = scheduler;
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

    private Observable<List<PropertySet>> postedItems(long beforeTime) {
        return postsStorage.loadPosts(PAGE_SIZE, beforeTime)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedPosts()));
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

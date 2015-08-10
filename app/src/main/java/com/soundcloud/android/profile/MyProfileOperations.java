package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Iterables.getLast;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.OperatorSwitchOnEmptyList;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class MyProfileOperations {

    @VisibleForTesting
    static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private final PostsStorage postsStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    private final Func1<Boolean, Observable<List<PropertySet>>> loadInitialPosts = new Func1<Boolean, Observable<List<PropertySet>>>() {
        @Override
        public Observable<List<PropertySet>> call(Boolean aBoolean) {
            return postsStorage.loadPosts(PAGE_SIZE, Long.MAX_VALUE)
                    .subscribeOn(scheduler);
        }
    };

    @Inject
    public MyProfileOperations(PostsStorage postsStorage,
                               SyncInitiator syncInitiator,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.postsStorage = postsStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<List<PropertySet>> pagedPostItems() {
        return postedPlaylists(Long.MAX_VALUE);
    }

    public Pager.PagingFunction<List<PropertySet>> postsPagingFunction() {
        return new Pager.PagingFunction<List<PropertySet>>() {
            @Override
            public Observable<List<PropertySet>> call(List<PropertySet> result) {
                if (result.size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    return postedPlaylists(getLast(result).get(PlaylistProperty.CREATED_AT).getTime());
                }
            }
        };
    }

    public Observable<List<PropertySet>> postsForPlayback() {
        return postsStorage.loadPostsForPlayback().subscribeOn(scheduler);
    }

    private Observable<List<PropertySet>> postedPlaylists(long beforeTime) {
        return postsStorage.loadPosts(PAGE_SIZE, beforeTime)
                .subscribeOn(scheduler)
                .lift(new OperatorSwitchOnEmptyList<>(updatedPosts()));
    }

    Observable<List<PropertySet>> updatedPosts() {
        return syncInitiator.refreshPosts()
                .flatMap(loadInitialPosts);
    }

}

package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MyPlaylistsOperations {

    private static final int PLAYLIST_LIMIT = 1000; // Arbitrarily high, we don't want to worry about paging

    private static final Func1<List<PropertySet>, List<PropertySet>> REMOVE_DUPLICATE_PLAYLISTS = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Set<Urn> uniquePlaylists = Sets.newHashSetWithExpectedSize(propertySets.size());
            for (Iterator<PropertySet> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                final Urn urn = iterator.next().get(PlaylistProperty.URN);
                if (uniquePlaylists.contains(urn)) {
                    iterator.remove();
                } else {
                    uniquePlaylists.add(urn);
                }
            }
            return propertySets;
        }
    };

    private static final Func1<List<PropertySet>, List<PropertySet>> SORT_BY_CREATION = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Collections.sort(propertySets, new Comparator<PropertySet>() {
                @Override
                public int compare(PropertySet lhs, PropertySet rhs) {
                    // flipped as we want reverse chronological order
                    return getAssociationDate(rhs).compareTo(getAssociationDate(lhs));
                }

                private Date getAssociationDate(PropertySet propertySet) {
                    return propertySet.contains(LikeProperty.CREATED_AT) ?
                           propertySet.get(LikeProperty.CREATED_AT) :
                           propertySet.get(PostProperty.CREATED_AT);
                }
            });
            return propertySets;
        }
    };

    private static final Func1<List<PropertySet>, List<PropertySet>> SORT_BY_TITLE = new Func1<List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> propertySets) {
            Collections.sort(propertySets, new Comparator<PropertySet>() {
                @Override
                public int compare(PropertySet lhs, PropertySet rhs) {
                    return lhs.get(PlaylistProperty.TITLE).compareTo(rhs.get(PlaylistProperty.TITLE));
                }
            });
            return propertySets;
        }
    };


    private static Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>> COMBINE_POSTED_AND_LIKED = new Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> postedPlaylists, List<PropertySet> likedPlaylists) {
            List<PropertySet> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
            all.addAll(postedPlaylists);
            all.addAll(likedPlaylists);
            return all;
        }
    };
    private final SyncInitiatorBridge syncInitiatorBridge;
    private final PlaylistLikesStorage playlistLikesStorage;
    private final PlaylistPostStorage playlistPostStorage;
    private final Scheduler scheduler;

    @Inject
    public MyPlaylistsOperations(SyncInitiatorBridge syncInitiatorBridge,
                                 PlaylistLikesStorage playlistLikesStorage,
                                 PlaylistPostStorage playlistPostStorage,
                                 @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.playlistLikesStorage = playlistLikesStorage;
        this.playlistPostStorage = playlistPostStorage;
        this.scheduler = scheduler;
    }

    public Observable<List<PlaylistItem>> myPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .hasSyncedLikedAndPostedPlaylistsBefore()
                .flatMap(new Func1<Boolean, Observable<List<PlaylistItem>>>() {
                    @Override
                    public Observable<List<PlaylistItem>> call(Boolean hasSynced) {
                        if (hasSynced) {
                            return loadPlaylists(options);
                        } else {
                            return refreshAndLoadPlaylists(options);
                        }
                    }
                }).subscribeOn(scheduler);
    }

    public Observable<List<PlaylistItem>> refreshAndLoadPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .refreshMyPostedAndLikedPlaylists()
                .flatMap(continueWith(loadPlaylists(options)));
    }

    private Observable<List<PlaylistItem>> loadPlaylists(PlaylistsOptions options) {
        return unsortedPlaylists(options)
                .map(offlineOnly(options.showOfflineOnly()))
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(REMOVE_DUPLICATE_PLAYLISTS)
                .map(PlaylistItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

    private Func1<List<PropertySet>, List<PropertySet>> offlineOnly(final boolean offlineOnly) {
        return new Func1<List<PropertySet>, List<PropertySet>>() {
            @Override
            public List<PropertySet> call(List<PropertySet> propertySets) {
                if (offlineOnly) {
                    for (Iterator<PropertySet> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                        OfflineState offlineState = iterator.next()
                                                            .getOrElse(OfflineProperty.OFFLINE_STATE, NOT_OFFLINE);

                        if (offlineState.equals(NOT_OFFLINE)) {
                            iterator.remove();
                        }
                    }
                }
                return propertySets;
            }
        };
    }

    private Observable<List<PropertySet>> unsortedPlaylists(PlaylistsOptions options) {
        final Observable<List<PropertySet>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT,
                                                                                                         Long.MAX_VALUE);
        final Observable<List<PropertySet>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
                                                                                                          Long.MAX_VALUE);
        if (options.showLikes() && !options.showPosts()) {
            return loadLikedPlaylists;
        } else if (options.showPosts() && !options.showLikes()) {
            return loadPostedPlaylists;
        } else {
            return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED);
        }
    }
}

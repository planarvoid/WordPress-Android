package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.java.collections.Lists;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MyPlaylistsOperations {

    private static final int PLAYLIST_LIMIT = 1000; // Arbitrarily high, we don't want to worry about paging

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> REMOVE_DUPLICATE_PLAYLISTS = new Func1<List<PlaylistAssociation>, List<PlaylistAssociation>>() {
        @Override
        public List<PlaylistAssociation> call(List<PlaylistAssociation> playlistAssociations) {
            Set<Urn> uniquePlaylists = Sets.newHashSetWithExpectedSize(playlistAssociations.size());
            for (Iterator<PlaylistAssociation> iterator = playlistAssociations.iterator(); iterator.hasNext(); ) {
                final Urn urn = iterator.next().getPlaylistItem().getUrn();
                if (uniquePlaylists.contains(urn)) {
                    iterator.remove();
                } else {
                    uniquePlaylists.add(urn);
                }
            }
            return playlistAssociations;
        }
    };

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_CREATION = new Func1<List<PlaylistAssociation>, List<PlaylistAssociation>>() {
        @Override
        public List<PlaylistAssociation> call(List<PlaylistAssociation> playlistAssociations) {
            Collections.sort(playlistAssociations, new Comparator<PlaylistAssociation>() {
                @Override
                public int compare(PlaylistAssociation lhs, PlaylistAssociation rhs) {
                    // flipped as we want reverse chronological order
                    return rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
                }
            });
            return playlistAssociations;
        }
    };

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_TITLE = new Func1<List<PlaylistAssociation>, List<PlaylistAssociation>>() {
        @Override
        public List<PlaylistAssociation> call(List<PlaylistAssociation> propertySets) {
            Collections.sort(propertySets, new Comparator<PlaylistAssociation>() {
                @Override
                public int compare(PlaylistAssociation lhs, PlaylistAssociation rhs) {
                    return lhs.getPlaylistItem().getTitle().compareTo(rhs.getPlaylistItem().getTitle());
                }
            });
            return propertySets;
        }
    };


    private static Func2<List<PlaylistAssociation>, List<PlaylistAssociation>, List<PlaylistAssociation>> COMBINE_POSTED_AND_LIKED = new Func2<List<PlaylistAssociation>, List<PlaylistAssociation>, List<PlaylistAssociation>>() {
        @Override
        public List<PlaylistAssociation> call(List<PlaylistAssociation> postedPlaylists, List<PlaylistAssociation> likedPlaylists) {
            List<PlaylistAssociation> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
            all.addAll(postedPlaylists);
            all.addAll(likedPlaylists);
            return all;
        }
    };

    private static final Func1<List<PlaylistAssociation>, List<PlaylistItem>> EXTRACT_PLAYLIST_ITEMS = new Func1<List<PlaylistAssociation>, List<PlaylistItem>>() {
        @Override
        public List<PlaylistItem> call(List<PlaylistAssociation> playlistAssociations) {
            return Lists.transform(playlistAssociations, PlaylistAssociation.GET_PLAYLIST_ITEM);
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
                .map(EXTRACT_PLAYLIST_ITEMS)
                .subscribeOn(scheduler);
    }

    private Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> offlineOnly(final boolean offlineOnly) {
        return new Func1<List<PlaylistAssociation>, List<PlaylistAssociation>>() {
            @Override
            public List<PlaylistAssociation> call(List<PlaylistAssociation> propertySets) {
                if (offlineOnly) {
                    for (Iterator<PlaylistAssociation> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                        OfflineState offlineState = iterator.next().getPlaylistItem().getDownloadState();

                        if (offlineState.equals(NOT_OFFLINE)) {
                            iterator.remove();
                        }
                    }
                }
                return propertySets;
            }
        };
    }

    private Observable<List<PlaylistAssociation>> unsortedPlaylists(PlaylistsOptions options) {
        final Observable<List<PlaylistAssociation>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT,
                                                                                                                 Long.MAX_VALUE);
        final Observable<List<PlaylistAssociation>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
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

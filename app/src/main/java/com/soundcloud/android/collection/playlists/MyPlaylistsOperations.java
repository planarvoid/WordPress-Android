package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;

import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistAssociation;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MyPlaylistsOperations {

    private static final int PLAYLIST_LIMIT = 1000; // Arbitrarily high, we don't want to worry about paging

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> REMOVE_DUPLICATE_PLAYLISTS = playlistAssociations -> {
        Set<Urn> uniquePlaylists = Sets.newHashSetWithExpectedSize(playlistAssociations.size());
        for (Iterator<PlaylistAssociation> iterator = playlistAssociations.iterator(); iterator.hasNext(); ) {
            final Urn urn = iterator.next().getPlaylist().urn();
            if (uniquePlaylists.contains(urn)) {
                iterator.remove();
            } else {
                uniquePlaylists.add(urn);
            }
        }
        return playlistAssociations;
    };

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_CREATION = playlistAssociations -> {
        Collections.sort(playlistAssociations, (lhs, rhs) -> {
            // flipped as we want reverse chronological order
            return rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
        });
        return playlistAssociations;
    };

    private static final Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_TITLE = propertySets -> {
        Collections.sort(propertySets, (lhs, rhs) -> lhs.getPlaylist().title().compareTo(rhs.getPlaylist().title()));
        return propertySets;
    };


    private static Func2<List<PlaylistAssociation>, List<PlaylistAssociation>, List<PlaylistAssociation>> COMBINE_POSTED_AND_LIKED = (postedPlaylists, likedPlaylists) -> {
        List<PlaylistAssociation> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
        all.addAll(postedPlaylists);
        all.addAll(likedPlaylists);
        return all;
    };

    private static final Func1<List<PlaylistAssociation>, List<Playlist>> EXTRACT_PLAYLIST_ITEMS = playlistAssociations -> Lists.transform(playlistAssociations, PlaylistAssociation::getPlaylist);

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

    public Observable<List<Playlist>> myPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .hasSyncedLikedAndPostedPlaylistsBefore()
                .flatMap(new Func1<Boolean, Observable<List<Playlist>>>() {
                    @Override
                    public Observable<List<Playlist>> call(Boolean hasSynced) {
                        if (hasSynced) {
                            return loadPlaylists(options);
                        } else {
                            return refreshAndLoadPlaylists(options);
                        }
                    }
                }).subscribeOn(scheduler);
    }

    public Observable<List<Playlist>> refreshAndLoadPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .refreshMyPostedAndLikedPlaylists()
                .flatMap(o -> loadPlaylists(options));
    }

    private Observable<List<Playlist>> loadPlaylists(PlaylistsOptions options) {
        return unsortedPlaylists(options)
                .map(offlineOnly(options.showOfflineOnly()))
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(REMOVE_DUPLICATE_PLAYLISTS)
                .map(EXTRACT_PLAYLIST_ITEMS)
                .subscribeOn(scheduler);
    }

    private Func1<List<PlaylistAssociation>, List<PlaylistAssociation>> offlineOnly(final boolean offlineOnly) {
        return propertySets -> {
            if (offlineOnly) {
                for (Iterator<PlaylistAssociation> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                    OfflineState offlineState = iterator.next().getPlaylist().offlineState().or(NOT_OFFLINE);

                    if (offlineState.equals(NOT_OFFLINE)) {
                        iterator.remove();
                    }
                }
            }
            return propertySets;
        };
    }

    private Observable<List<PlaylistAssociation>> unsortedPlaylists(PlaylistsOptions options) {
        final Observable<List<PlaylistAssociation>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT,
                                                                                                                 Long.MAX_VALUE,
                                                                                                                 options.textFilter());
        final Observable<List<PlaylistAssociation>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT,
                                                                                                          Long.MAX_VALUE,
                                                                                                                  options.textFilter());
        if (options.showLikes() && !options.showPosts()) {
            return loadLikedPlaylists;
        } else if (options.showPosts() && !options.showLikes()) {
            return loadPostedPlaylists;
        } else {
            return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED);
        }
    }
}

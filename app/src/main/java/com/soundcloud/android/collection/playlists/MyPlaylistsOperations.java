package com.soundcloud.android.collection.playlists;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;
import static com.soundcloud.android.offline.OfflineState.NOT_OFFLINE;
import static com.soundcloud.android.utils.RepoUtilsKt.enrichItemsWithProperties;
import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistAssociation;
import com.soundcloud.android.playlists.PlaylistRepository;
import com.soundcloud.android.playlists.PostsStorage;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Sets;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MyPlaylistsOperations {

    private static final int PLAYLIST_LIMIT = 1000; // Arbitrarily high, we don't want to worry about paging

    private static final Function<List<PlaylistAssociation>, List<PlaylistAssociation>> REMOVE_DUPLICATE_PLAYLISTS = playlistAssociations -> {
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

    private static final Function<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_CREATION = playlistAssociations -> {
        Collections.sort(playlistAssociations, (lhs, rhs) -> {
            // flipped as we want reverse chronological order
            return rhs.getAssociation().getCreatedAt().compareTo(lhs.getAssociation().getCreatedAt());
        });
        return playlistAssociations;
    };

    private static final Function<List<PlaylistAssociation>, List<PlaylistAssociation>> SORT_BY_TITLE = propertySets -> {
        Collections.sort(propertySets, (lhs, rhs) -> lhs.getPlaylist().title().compareTo(rhs.getPlaylist().title()));
        return propertySets;
    };


    private static io.reactivex.functions.BiFunction<List<PlaylistAssociation>, List<PlaylistAssociation>, List<PlaylistAssociation>> COMBINE_POSTED_AND_LIKED = (postedPlaylists, likedPlaylists) -> {
        List<PlaylistAssociation> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
        all.addAll(postedPlaylists);
        all.addAll(likedPlaylists);
        return all;
    };

    private static final Function<List<PlaylistAssociation>, List<Playlist>> EXTRACT_PLAYLIST_ITEMS = playlistAssociations -> Lists.transform(playlistAssociations, PlaylistAssociation::getPlaylist);

    private final SyncInitiatorBridge syncInitiatorBridge;
    private final PostsStorage postsStorage;
    private final LikesStorage likesStorage;
    private final PlaylistRepository playlistRepository;
    private final Scheduler scheduler;

    @Inject
    public MyPlaylistsOperations(SyncInitiatorBridge syncInitiatorBridge,
                                 PostsStorage postsStorage,
                                 LikesStorage likesStorage,
                                 PlaylistRepository playlistRepository,
                                 @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.syncInitiatorBridge = syncInitiatorBridge;
        this.postsStorage = postsStorage;
        this.likesStorage = likesStorage;
        this.playlistRepository = playlistRepository;
        this.scheduler = scheduler;
    }

    public Maybe<List<Playlist>> myPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .hasSyncedLikedAndPostedPlaylistsBefore()
                .flatMapMaybe(hasSynced -> {
                    if (hasSynced) {
                        return loadPlaylists(options);
                    } else {
                        return refreshAndLoadPlaylists(options);
                    }
                }).subscribeOn(scheduler);
    }

    public Maybe<List<Playlist>> refreshAndLoadPlaylists(final PlaylistsOptions options) {
        return syncInitiatorBridge
                .refreshMyPostedAndLikedPlaylists()
                .andThen(loadPlaylists(options));
    }

    private Maybe<List<Playlist>> loadPlaylists(PlaylistsOptions options) {
        return unsortedPlaylists(options)
                .map(albumsOnly(options.entities() == PlaylistsOptions.Entities.ALBUMS))
                .map(playlistsOnly(options.entities() == PlaylistsOptions.Entities.PLAYLISTS))
                .map(offlineOnly(options.showOfflineOnly()))
                .map(options.sortByTitle() ? SORT_BY_TITLE : SORT_BY_CREATION)
                .map(REMOVE_DUPLICATE_PLAYLISTS)
                .map(EXTRACT_PLAYLIST_ITEMS)
                .subscribeOn(scheduler)
                .toMaybe();
    }

    private Function<List<PlaylistAssociation>, List<PlaylistAssociation>> albumsOnly(final boolean albumsOnly) {
        return propertySets -> {
            if (albumsOnly) {
                for (Iterator<PlaylistAssociation> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                    Playlist playlist = iterator.next().getPlaylist();

                    if (!playlist.isAlbum()) {
                        iterator.remove();
                    }
                }
            }
            return propertySets;
        };
    }

    private Function<List<PlaylistAssociation>, List<PlaylistAssociation>> playlistsOnly(final boolean playlistsOnly) {
        return propertySets -> {
            if (playlistsOnly) {
                for (Iterator<PlaylistAssociation> iterator = propertySets.iterator(); iterator.hasNext(); ) {
                    Playlist playlist = iterator.next().getPlaylist();

                    if (playlist.isAlbum()) {
                        iterator.remove();
                    }
                }
            }
            return propertySets;
        };
    }

    private Function<List<PlaylistAssociation>, List<PlaylistAssociation>> offlineOnly(final boolean offlineOnly) {
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

    private Single<List<PlaylistAssociation>> unsortedPlaylists(PlaylistsOptions options) {
        final Single<List<PlaylistAssociation>> loadLikedPlaylists = likesStorage.loadPlaylistLikes(Long.MAX_VALUE, PLAYLIST_LIMIT)
                                                                                 .flatMap(source -> enrichItemsWithProperties(source,
                                                                                                                              playlistRepository.withUrns(transform(source, UrnHolder::urn)),
                                                                                                                              PlaylistAssociation::create));

        final Single<List<PlaylistAssociation>> loadPostedPlaylists = postsStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE)
                                                                                  .flatMap(source -> {
                                                                                      List<Urn> urns = transform(source, UrnHolder::urn);
                                                                                      return enrichItemsWithProperties(source,
                                                                                                                       playlistRepository.withUrns(urns),
                                                                                                                       PlaylistAssociation::create);
                                                                                  });

        if (options.showLikes() && !options.showPosts()) {
            return loadLikedPlaylists;
        } else if (options.showPosts() && !options.showLikes()) {
            return loadPostedPlaylists;
        } else {
            return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED);
        }
    }
}

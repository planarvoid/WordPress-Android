package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.MoreCollections;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class PlaylistExploder {

    static final int PLAYLIST_LOOKAHEAD_COUNT = 8;
    static final int PLAYLIST_LOOKBEHIND_COUNT = 4;

    private final PlaylistOperations playlistOperations;
    private final PlayQueueManager playQueueManager;

    private final Set<Urn> explodedPlaylists = new HashSet<>();
    private final CompositeSubscription loadPlaylistsSubscription = new CompositeSubscription();

    @Inject
    public PlaylistExploder(PlaylistOperations playlistOperations,
                            PlayQueueManager playQueueManager) {
        this.playlistOperations = playlistOperations;
        this.playQueueManager = playQueueManager;
    }

    void onPlayQueue(PlayQueueEvent event) {
        if (event.isNewQueue()) {
            loadPlaylistsSubscription.clear();
            explodedPlaylists.clear();
            explodePlaylists(playQueueManager.getCurrentPosition(), PLAYLIST_LOOKAHEAD_COUNT);
        }
    }

    void onCurrentPlayQueueItem(CurrentPlayQueueItemEvent event) {
        explodePlaylists(playQueueManager.getCurrentPosition(), PLAYLIST_LOOKAHEAD_COUNT);
    }

    public void explodePlaylists(int position, int count) {
        final Collection<Urn> playlists = getSurroundingPlaylists(position, count);
        for (final Urn playlist : playlists) {
            if (!explodedPlaylists.contains(playlist)) {
                loadPlaylistTracks(playlist);
            }
        }
    }

    private void loadPlaylistTracks(final Urn playlist) {
        explodedPlaylists.add(playlist);
        loadPlaylistsSubscription.add(playlistOperations.trackUrnsForPlayback(playlist)
                                                        .doOnTerminate(removePlaylistLoad(playlist))
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(new DefaultSubscriber<List<Urn>>() {
                                                            @Override
                                                            public void onNext(List<Urn> args) {
                                                                playQueueManager.insertPlaylistTracks(playlist, args);
                                                            }
                                                        }));
    }

    @NonNull
    private Action0 removePlaylistLoad(final Urn playlist) {
        return () -> explodedPlaylists.remove(playlist);
    }

    private Collection<Urn> getSurroundingPlaylists(int position, int count) {
        final List<Urn> surrounding = new ArrayList<>(playQueueManager.getPlayQueueItems(position, count));
        surrounding.addAll(playQueueManager.getPreviousPlayQueueItems(PLAYLIST_LOOKBEHIND_COUNT));
        return MoreCollections.filter(surrounding, input -> input.isPlaylist());
    }
}

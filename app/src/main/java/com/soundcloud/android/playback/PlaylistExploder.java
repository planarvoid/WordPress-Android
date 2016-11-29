package com.soundcloud.android.playback;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistExploder {

    static final int PLAYLIST_LOOKAHEAD_COUNT = 8;
    static final int PLAYLIST_LOOKBEHIND_COUNT = 4;

    private final EventBus eventBus;
    private final PlaylistOperations playlistOperations;
    private final PlayQueueManager playQueueManager;

    private Set<Urn> playlistLoads = new HashSet<>();
    private CompositeSubscription loadPlaylistsSubscription = new CompositeSubscription();

    @Inject
    public PlaylistExploder(EventBus eventBus,
                            PlaylistOperations playlistOperations,
                            PlayQueueManager playQueueManager) {
        this.eventBus = eventBus;
        this.playlistOperations = playlistOperations;
        this.playQueueManager = playQueueManager;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new PlayQueueTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.isNewQueue()) {
                loadPlaylistsSubscription.unsubscribe();
                loadPlaylistsSubscription = new CompositeSubscription();
                playlistLoads.clear();
                loadSurroundingPlaylists();
            }
        }
    }


    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            loadSurroundingPlaylists();
        }
    }

    private void loadSurroundingPlaylists() {
        final Collection<Urn> playlists = getSurroundingPlaylists();
        for (final Urn playlist : playlists) {
            if (!playlistLoads.contains(playlist)) {
                loadPlaylistTracks(playlist);
            }
        }
    }

    private void loadPlaylistTracks(final Urn playlist) {
        playlistLoads.add(playlist);
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
        return new Action0() {
            @Override
            public void call() {
                playlistLoads.remove(playlist);
            }
        };
    }

    private Collection<Urn> getSurroundingPlaylists() {
        final List<Urn> surrounding = new ArrayList<>(playQueueManager.getUpcomingPlayQueueItems(
                PLAYLIST_LOOKAHEAD_COUNT));
        surrounding.addAll(playQueueManager.getPreviousPlayQueueItems(PLAYLIST_LOOKBEHIND_COUNT));
        return MoreCollections.filter(surrounding, new Predicate<Urn>() {
            @Override
            public boolean apply(Urn input) {
                return input.isPlaylist();
            }
        });
    }
}

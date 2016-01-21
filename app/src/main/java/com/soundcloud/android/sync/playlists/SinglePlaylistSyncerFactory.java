package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.RemovePlaylistCommand;

import javax.inject.Inject;
import javax.inject.Provider;

public class SinglePlaylistSyncerFactory {

    private final Provider<LoadPlaylistTracksWithChangesCommand> loadUnpushedTracksForPlaylist;
    private final Provider<PushPlaylistAdditionsCommand> pushPlaylistAdditions;
    private final Provider<PushPlaylistRemovalsCommand> pushPlaylistRemovals;
    private final Provider<FetchPlaylistWithTracksCommand> fetchPlaylistWithTracks;
    private final StorePlaylistCommand storePlaylist;
    private final RemovePlaylistCommand removePlaylist;
    private final StoreTracksCommand storeTracks;
    private final Provider<ReplacePlaylistTracksCommand> replacePlaylistTracks;

    @Inject
    public SinglePlaylistSyncerFactory(Provider<LoadPlaylistTracksWithChangesCommand> loadUnpushedTracksForPlaylist,
                                       Provider<PushPlaylistAdditionsCommand> pushPlaylistAdditions,
                                       Provider<PushPlaylistRemovalsCommand> pushPlaylistRemovals,
                                       Provider<FetchPlaylistWithTracksCommand> fetchPlaylistWithTracks,
                                       StorePlaylistCommand storePlaylist,
                                       RemovePlaylistCommand removePlaylist,
                                       StoreTracksCommand storeTracks,
                                       Provider<ReplacePlaylistTracksCommand> replacePlaylistTracks) {
        this.loadUnpushedTracksForPlaylist = loadUnpushedTracksForPlaylist;
        this.pushPlaylistAdditions = pushPlaylistAdditions;
        this.pushPlaylistRemovals = pushPlaylistRemovals;
        this.fetchPlaylistWithTracks = fetchPlaylistWithTracks;
        this.storePlaylist = storePlaylist;
        this.removePlaylist = removePlaylist;
        this.storeTracks = storeTracks;
        this.replacePlaylistTracks = replacePlaylistTracks;
    }

    public SinglePlaylistSyncer create(Urn playlistUrn){
        return new SinglePlaylistSyncer(
                fetchPlaylistWithTracks.get().with(playlistUrn),
                removePlaylist,
                loadUnpushedTracksForPlaylist.get().with(playlistUrn),
                pushPlaylistAdditions.get().with(playlistUrn),
                pushPlaylistRemovals.get().with(playlistUrn),
                storeTracks,
                storePlaylist,
                replacePlaylistTracks.get().with(playlistUrn));
    }
}

package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.RemovePlaylistCommand;

import javax.inject.Inject;

public class SinglePlaylistSyncerFactory {
    private final LoadPlaylistTracksWithChangesCommand loadUnpushedTracksForPlaylist;
    private final PushPlaylistAdditionsCommand pushPlaylistAdditions;
    private final PushPlaylistRemovalsCommand pushPlaylistRemovals;
    private final FetchPlaylistWithTracksCommand fetchPlaylistWithTracks;
    private final StorePlaylistCommand storePlaylist;
    private final RemovePlaylistCommand removePlaylist;
    private final StoreTracksCommand storeTracks;
    private final ReplacePlaylistTracksCommand replacePlaylistTracks;

    @Inject
    public SinglePlaylistSyncerFactory(LoadPlaylistTracksWithChangesCommand loadUnpushedTracksForPlaylist,
                                       PushPlaylistAdditionsCommand pushPlaylistAdditions,
                                       PushPlaylistRemovalsCommand pushPlaylistRemovals,
                                       FetchPlaylistWithTracksCommand fetchPlaylistWithTracks,
                                       StorePlaylistCommand storePlaylist,
                                       RemovePlaylistCommand removePlaylist,
                                       StoreTracksCommand storeTracks,
                                       ReplacePlaylistTracksCommand replacePlaylistTracks) {
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
                fetchPlaylistWithTracks.with(playlistUrn),
                removePlaylist,
                loadUnpushedTracksForPlaylist.with(playlistUrn),
                pushPlaylistAdditions.with(playlistUrn),
                pushPlaylistRemovals.with(playlistUrn),
                storeTracks,
                storePlaylist,
                replacePlaylistTracks.with(playlistUrn));
    }
}

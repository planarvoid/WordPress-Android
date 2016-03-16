package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.playlists.RemovePlaylistCommand;

import javax.inject.Inject;
import javax.inject.Provider;

public class SinglePlaylistSyncerFactory {

    private final Provider<LoadPlaylistTracksWithChangesCommand> loadUnpushedTracksForPlaylist;
    private final ApiClient apiClient;
    private final Provider<FetchPlaylistWithTracksCommand> fetchPlaylistWithTracks;
    private final StorePlaylistsCommand storePlaylist;
    private final RemovePlaylistCommand removePlaylist;
    private final StoreTracksCommand storeTracks;
    private final PlaylistStorage playlistStorage;
    private final Provider<ReplacePlaylistTracksCommand> replacePlaylistTracks;

    @Inject
    public SinglePlaylistSyncerFactory(Provider<LoadPlaylistTracksWithChangesCommand> loadUnpushedTracksForPlaylist,
                                       ApiClient apiClient,
                                       Provider<FetchPlaylistWithTracksCommand> fetchPlaylistWithTracks,
                                       StorePlaylistsCommand storePlaylist,
                                       RemovePlaylistCommand removePlaylist,
                                       StoreTracksCommand storeTracks,
                                       PlaylistStorage playlistStorage,
                                       Provider<ReplacePlaylistTracksCommand> replacePlaylistTracks) {
        this.loadUnpushedTracksForPlaylist = loadUnpushedTracksForPlaylist;
        this.apiClient = apiClient;
        this.playlistStorage = playlistStorage;
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
                apiClient, storeTracks,
                storePlaylist,
                replacePlaylistTracks.get().with(playlistUrn),
                playlistStorage);
    }
}

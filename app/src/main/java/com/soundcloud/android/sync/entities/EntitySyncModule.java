package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(complete = false, library = true)
public class EntitySyncModule {

    public static final String TRACKS_SYNC = "TracksSync";
    public static final String PLAYLISTS_SYNC = "PlaylistsSync";

    @Provides
    @Named(TRACKS_SYNC)
    EntitySyncJob provideTrackSyncJob(FetchTracksCommand fetchTracks, StoreTracksCommand storeTracks) {
        return new EntitySyncJob(fetchTracks, storeTracks);
    }

    @Provides
    @Named(PLAYLISTS_SYNC)
    EntitySyncJob providePlaylistSyncJob(FetchPlaylistsCommand fetchPlaylists, StorePlaylistsCommand storePlaylists) {
        return new EntitySyncJob(fetchPlaylists, storePlaylists);
    }
}
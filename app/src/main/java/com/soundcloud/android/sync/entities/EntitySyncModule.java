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

    @Provides
    @Named("TracksSyncer")
    EntitySyncJob provideTrackSyncer(FetchTracksCommand fetchTracks, StoreTracksCommand storeTracks) {
        return new EntitySyncJob(fetchTracks, storeTracks);
    }

    @Provides
    @Named("PlaylistsSyncer")
    EntitySyncJob providePlaylistSyncer(FetchPlaylistsCommand fetchPlaylists, StorePlaylistsCommand storePlaylists) {
        return new EntitySyncJob(fetchPlaylists, storePlaylists);
    }
}
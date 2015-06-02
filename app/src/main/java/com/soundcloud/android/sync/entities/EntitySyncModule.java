package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.android.sync.commands.FetchUsersCommand;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(complete = false, library = true)
public class EntitySyncModule {

    public static final String TRACKS_SYNC = "TracksSync";
    public static final String PLAYLISTS_SYNC = "PlaylistsSync";
    public static final String USERS_SYNC = "UsersSync";

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

    @Provides
    @Named(USERS_SYNC)
    EntitySyncJob provideUsersSyncJob(FetchUsersCommand fetchUsers, StoreUsersCommand storeUsers) {
        return new EntitySyncJob(fetchUsers, storeUsers);
    }
}
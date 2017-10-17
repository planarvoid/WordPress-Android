package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.android.sync.commands.FetchUsersCommand;
import com.soundcloud.android.sync.commands.PublishPlaylistUpdateEventCommand;
import com.soundcloud.android.sync.commands.PublishTrackUpdateEventCommand;
import com.soundcloud.android.sync.commands.PublishUserUpdateEventCommand;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class EntitySyncModule {

    static final String TRACKS_SYNC = "TracksSync";
    static final String PLAYLISTS_SYNC = "PlaylistsSync";
    static final String USERS_SYNC = "UsersSync";

    @Provides
    @Named(TRACKS_SYNC)
    static EntitySyncJob provideTrackSyncJob(FetchTracksCommand fetchTracks, StoreTracksCommand storeTracks, PublishTrackUpdateEventCommand publishTracksUpdateEvent) {
        return new EntitySyncJob(fetchTracks, storeTracks, publishTracksUpdateEvent);
    }

    @Provides
    @Named(PLAYLISTS_SYNC)
    static EntitySyncJob providePlaylistSyncJob(FetchPlaylistsCommand fetchPlaylists, StorePlaylistsCommand storePlaylists, PublishPlaylistUpdateEventCommand publishPlaylistUpdateEvent) {
        return new EntitySyncJob(fetchPlaylists, storePlaylists, publishPlaylistUpdateEvent);
    }

    @Provides
    @Named(USERS_SYNC)
    static EntitySyncJob provideUsersSyncJob(FetchUsersCommand fetchUsers, StoreUsersCommand storeUsers, PublishUserUpdateEventCommand publishUserUpdateEvent) {
        return new EntitySyncJob(fetchUsers, storeUsers, publishUserUpdateEvent);
    }
}

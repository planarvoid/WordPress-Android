package com.soundcloud.android.sync.entities;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.android.sync.commands.FetchUsersCommand;
import com.soundcloud.android.sync.playlists.PublishPlaylistUpdateEvent;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module
public class EntitySyncModule {

    public static final String TRACKS_SYNC = "TracksSync";
    public static final String PLAYLISTS_SYNC = "PlaylistsSync";
    public static final String USERS_SYNC = "UsersSync";

    @Provides
    @Named(TRACKS_SYNC)
    EntitySyncJob provideTrackSyncJob(FetchTracksCommand fetchTracks, StoreTracksCommand storeTracks, LegacyPublishEntityUpdateEvent legacyPublishEntityUpdateEvent) {
        return new EntitySyncJob(fetchTracks, storeTracks, legacyPublishEntityUpdateEvent);
    }

    @Provides
    @Named(PLAYLISTS_SYNC)
    EntitySyncJob providePlaylistSyncJob(FetchPlaylistsCommand fetchPlaylists, StorePlaylistsCommand storePlaylists, PublishPlaylistUpdateEvent publishPlaylistUpdateEvent) {
        return new EntitySyncJob(fetchPlaylists, storePlaylists, publishPlaylistUpdateEvent);
    }

    @Provides
    @Named(USERS_SYNC)
    EntitySyncJob provideUsersSyncJob(FetchUsersCommand fetchUsers, StoreUsersCommand storeUsers, LegacyPublishEntityUpdateEvent legacyPublishEntityUpdateEvent) {
        return new EntitySyncJob(fetchUsers, storeUsers, legacyPublishEntityUpdateEvent);
    }
}

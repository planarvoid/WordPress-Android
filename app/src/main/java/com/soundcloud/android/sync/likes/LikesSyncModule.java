package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(complete = false, library = true)
public class LikesSyncModule {

    @Provides
    @Named("TrackLikesSyncer")
    LikesSyncer<ApiTrack> provideTrackLikesSyncer(
            FetchLikesCommand fetchLikesCommand, FetchTracksCommand fetchTracks, LoadLikesCommand loadLikes,
            @Named("TrackLikeAdditions") PushLikeAdditionsCommand pushLikeAdditions,
            @Named("TrackLikeDeletions") PushLikeDeletionsCommand pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StoreTracksCommand storeTracks, StoreLikesCommand storeLikes,
            @Named("RemoveTrackLikes") RemoveLikesCommand removeLikes) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_TRACKS), fetchTracks, pushLikeAdditions, pushLikeDeletions, loadLikes.with(Sounds.TYPE_TRACK),
                loadLikesPendingAddition.with(Sounds.TYPE_TRACK), loadLikesPendingRemoval.with(Sounds.TYPE_TRACK), storeTracks, storeLikes,
                removeLikes);
    }

    @Provides
    @Named("PlaylistLikesSyncer")
    LikesSyncer<ApiPlaylist> providePlaylistLikesSyncer(
            FetchLikesCommand fetchLikesCommand, FetchPlaylistsCommand fetchPlaylists, LoadLikesCommand loadLikes,
            @Named("PlaylistLikeAdditions") PushLikeAdditionsCommand pushLikeAdditions,
            @Named("PlaylistLikeDeletions") PushLikeDeletionsCommand pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StorePlaylistsCommand storePlaylists, StoreLikesCommand storeLikes,
            @Named("RemovePlaylistLikes") RemoveLikesCommand removeLikes) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_PLAYLISTS), fetchPlaylists, pushLikeAdditions, pushLikeDeletions, loadLikes.with(Sounds.TYPE_PLAYLIST),
                loadLikesPendingAddition.with(Sounds.TYPE_PLAYLIST), loadLikesPendingRemoval.with(Sounds.TYPE_PLAYLIST), storePlaylists, storeLikes,
                removeLikes);
    }

    @Provides
    @Named("TrackLikeAdditions")
    PushLikeAdditionsCommand provideTrackLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikeAdditionsCommand(apiClient, ApiEndpoints.CREATE_TRACK_LIKES);
    }

    @Provides
    @Named("TrackLikeDeletions")
    PushLikeDeletionsCommand provideTrackLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikeDeletionsCommand(apiClient, ApiEndpoints.DELETE_TRACK_LIKES);
    }

    @Provides
    @Named("PlaylistLikeAdditions")
    PushLikeAdditionsCommand providePlaylistLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikeAdditionsCommand(apiClient, ApiEndpoints.CREATE_PLAYLIST_LIKES);
    }

    @Provides
    @Named("PlaylistLikeDeletions")
    PushLikeDeletionsCommand providePlaylistLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikeDeletionsCommand(apiClient, ApiEndpoints.DELETE_PLAYLIST_LIKES);
    }

    @Provides
    @Named("RemoveTrackLikes")
    RemoveLikesCommand provideRemoveTrackLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Sounds.TYPE_TRACK);
    }

    @Provides
    @Named("RemovePlaylistLikes")
    RemoveLikesCommand provideRemovePlaylistLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Sounds.TYPE_PLAYLIST);
    }
}

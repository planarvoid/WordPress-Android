package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
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

    public static final String TRACK_LIKES_SYNCER = "TrackLikesSyncer";
    public static final String PLAYLIST_LIKES_SYNCER = "PlaylistLikesSyncer";
    public static final String TRACK_LIKE_ADDITIONS = "TrackLikeAdditions";
    public static final String TRACK_LIKE_DELETIONS = "TrackLikeDeletions";
    public static final String PLAYLIST_LIKE_ADDITIONS = "PlaylistLikeAdditions";
    public static final String PLAYLIST_LIKE_DELETIONS = "PlaylistLikeDeletions";
    public static final String REMOVE_TRACK_LIKES = "RemoveTrackLikes";
    public static final String REMOVE_PLAYLIST_LIKES = "RemovePlaylistLikes";

    @Provides
    @Named(TRACK_LIKES_SYNCER)
    LikesSyncer<ApiTrack> provideTrackLikesSyncer(
            FetchLikesCommand fetchLikesCommand, FetchTracksCommand fetchTracks, LoadLikesCommand loadLikes,
            @Named(TRACK_LIKE_ADDITIONS) PushLikesCommand<ApiLike> pushLikeAdditions,
            @Named(TRACK_LIKE_DELETIONS) PushLikesCommand<ApiDeletedLike> pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StoreTracksCommand storeTracks, StoreLikesCommand storeLikes,
            @Named(REMOVE_TRACK_LIKES) RemoveLikesCommand removeLikes) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_TRACKS), fetchTracks, pushLikeAdditions, pushLikeDeletions, loadLikes.with(Sounds.TYPE_TRACK),
                loadLikesPendingAddition.with(Sounds.TYPE_TRACK), loadLikesPendingRemoval.with(Sounds.TYPE_TRACK), storeTracks, storeLikes,
                removeLikes);
    }

    @Provides
    @Named(PLAYLIST_LIKES_SYNCER)
    LikesSyncer<ApiPlaylist> providePlaylistLikesSyncer(
            FetchLikesCommand fetchLikesCommand, FetchPlaylistsCommand fetchPlaylists, LoadLikesCommand loadLikes,
            @Named(PLAYLIST_LIKE_ADDITIONS) PushLikesCommand<ApiLike> pushLikeAdditions,
            @Named(PLAYLIST_LIKE_DELETIONS) PushLikesCommand<ApiDeletedLike> pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StorePlaylistsCommand storePlaylists, StoreLikesCommand storeLikes,
            @Named(REMOVE_PLAYLIST_LIKES) RemoveLikesCommand removeLikes) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_PLAYLISTS), fetchPlaylists, pushLikeAdditions, pushLikeDeletions, loadLikes.with(Sounds.TYPE_PLAYLIST),
                loadLikesPendingAddition.with(Sounds.TYPE_PLAYLIST), loadLikesPendingRemoval.with(Sounds.TYPE_PLAYLIST), storePlaylists, storeLikes,
                removeLikes);
    }

    @Provides
    @Named(TRACK_LIKE_ADDITIONS)
    PushLikesCommand<ApiLike> provideTrackLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient, ApiEndpoints.CREATE_TRACK_LIKES, new TypeToken<ModelCollection<ApiLike>>() {});
    }

    @Provides
    @Named(TRACK_LIKE_DELETIONS)
    PushLikesCommand<ApiDeletedLike> provideTrackLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient, ApiEndpoints.DELETE_TRACK_LIKES, new TypeToken<ModelCollection<ApiDeletedLike>>() {});
    }

    @Provides
    @Named(PLAYLIST_LIKE_ADDITIONS)
    PushLikesCommand<ApiLike> providePlaylistLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient, ApiEndpoints.CREATE_PLAYLIST_LIKES, new TypeToken<ModelCollection<ApiLike>>() {});
    }

    @Provides
    @Named(PLAYLIST_LIKE_DELETIONS)
    PushLikesCommand<ApiDeletedLike> providePlaylistLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient, ApiEndpoints.DELETE_PLAYLIST_LIKES, new TypeToken<ModelCollection<ApiDeletedLike>>() {});
    }

    @Provides
    @Named(REMOVE_TRACK_LIKES)
    RemoveLikesCommand provideRemoveTrackLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Sounds.TYPE_TRACK);
    }

    @Provides
    @Named(REMOVE_PLAYLIST_LIKES)
    RemoveLikesCommand provideRemovePlaylistLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Sounds.TYPE_PLAYLIST);
    }
}

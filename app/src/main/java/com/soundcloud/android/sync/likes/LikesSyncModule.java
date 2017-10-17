package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class LikesSyncModule {

    static final String TRACK_LIKES_SYNCER = "TrackLikesSyncer";
    static final String PLAYLIST_LIKES_SYNCER = "PlaylistLikesSyncer";
    private static final String TRACK_LIKE_ADDITIONS = "TrackLikeAdditions";
    private static final String TRACK_LIKE_DELETIONS = "TrackLikeDeletions";
    private static final String PLAYLIST_LIKE_ADDITIONS = "PlaylistLikeAdditions";
    private static final String PLAYLIST_LIKE_DELETIONS = "PlaylistLikeDeletions";
    private static final String REMOVE_TRACK_LIKES = "RemoveTrackLikes";
    private static final String REMOVE_PLAYLIST_LIKES = "RemovePlaylistLikes";

    @Provides
    @Named(TRACK_LIKES_SYNCER)
    static LikesSyncer<ApiTrack> provideTrackLikesSyncer(
            FetchLikesCommand fetchLikesCommand,
            FetchTracksCommand fetchTracks,
            LoadLikesCommand loadLikes,
            @Named(TRACK_LIKE_ADDITIONS) PushLikesCommand<ApiLike> pushLikeAdditions,
            @Named(TRACK_LIKE_DELETIONS) PushLikesCommand<ApiDeletedLike> pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition,
            LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StoreTracksCommand storeTracks,
            StoreLikesCommand storeLikes,
            @Named(REMOVE_TRACK_LIKES) RemoveLikesCommand removeLikes,
            EventBus eventBus) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_TRACKS),
                                 fetchTracks,
                                 pushLikeAdditions,
                                 pushLikeDeletions,
                                 loadLikes,
                                 loadLikesPendingAddition,
                                 loadLikesPendingRemoval,
                                 storeTracks,
                                 storeLikes,
                                 removeLikes,
                                 eventBus,
                                 Tables.Sounds.TYPE_TRACK);
    }

    @Provides
    @Named(PLAYLIST_LIKES_SYNCER)
    static LikesSyncer<ApiPlaylist> providePlaylistLikesSyncer(
            FetchLikesCommand fetchLikesCommand,
            FetchPlaylistsCommand fetchPlaylists,
            LoadLikesCommand loadLikes,
            @Named(PLAYLIST_LIKE_ADDITIONS) PushLikesCommand<ApiLike> pushLikeAdditions,
            @Named(PLAYLIST_LIKE_DELETIONS) PushLikesCommand<ApiDeletedLike> pushLikeDeletions,
            LoadLikesPendingAdditionCommand loadLikesPendingAddition,
            LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
            StorePlaylistsCommand storePlaylists,
            StoreLikesCommand storeLikes,
            @Named(REMOVE_PLAYLIST_LIKES) RemoveLikesCommand removeLikes,
            EventBus eventBus) {
        return new LikesSyncer<>(fetchLikesCommand.with(ApiEndpoints.LIKED_PLAYLISTS),
                                 fetchPlaylists,
                                 pushLikeAdditions,
                                 pushLikeDeletions,
                                 loadLikes,
                                 loadLikesPendingAddition,
                                 loadLikesPendingRemoval,
                                 storePlaylists,
                                 storeLikes,
                                 removeLikes,
                                 eventBus,
                                 Tables.Sounds.TYPE_PLAYLIST);
    }

    @Provides
    @Named(TRACK_LIKE_ADDITIONS)
    static PushLikesCommand<ApiLike> provideTrackLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient,
                                      ApiEndpoints.CREATE_TRACK_LIKES,
                                      new TypeToken<ModelCollection<ApiLike>>() {
                                      });
    }

    @Provides
    @Named(TRACK_LIKE_DELETIONS)
    static PushLikesCommand<ApiDeletedLike> provideTrackLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient,
                                      ApiEndpoints.DELETE_TRACK_LIKES,
                                      new TypeToken<ModelCollection<ApiDeletedLike>>() {
                                      });
    }

    @Provides
    @Named(PLAYLIST_LIKE_ADDITIONS)
    static PushLikesCommand<ApiLike> providePlaylistLikeAdditionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient,
                                      ApiEndpoints.CREATE_PLAYLIST_LIKES,
                                      new TypeToken<ModelCollection<ApiLike>>() {
                                      });
    }

    @Provides
    @Named(PLAYLIST_LIKE_DELETIONS)
    static PushLikesCommand<ApiDeletedLike> providePlaylistLikeDeletionsPushCommand(ApiClient apiClient) {
        return new PushLikesCommand<>(apiClient,
                                      ApiEndpoints.DELETE_PLAYLIST_LIKES,
                                      new TypeToken<ModelCollection<ApiDeletedLike>>() {
                                      });
    }

    @Provides
    @Named(REMOVE_TRACK_LIKES)
    static RemoveLikesCommand provideRemoveTrackLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Tables.Sounds.TYPE_TRACK);
    }

    @Provides
    @Named(REMOVE_PLAYLIST_LIKES)
    static RemoveLikesCommand provideRemovePlaylistLikesCommand(PropellerDatabase database) {
        return new RemoveLikesCommand(database, Tables.Sounds.TYPE_PLAYLIST);
    }
}

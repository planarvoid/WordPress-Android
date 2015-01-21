package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(complete = false, library = true)
public class LikesSyncModule {

    @Provides
    @Named("TrackLikesSyncer")
    LikesSyncer provideTrackLikesSyncer(ApiClient apiClient, FetchTracksCommand fetchTracks, LoadLikesCommand loadLikes,
                                        LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
                                        StoreTracksCommand storeTracks, StoreLikesCommand storeLikes, RemoveLikesCommand removeLikes,
                                        AccountOperations accountOperations) {
        return new LikesSyncer(apiClient, fetchTracks, loadLikes.with(Sounds.TYPE_TRACK),
                loadLikesPendingAddition.with(Sounds.TYPE_TRACK), loadLikesPendingRemoval.with(Sounds.TYPE_TRACK), storeTracks, storeLikes,
                removeLikes, accountOperations, ApiEndpoints.LIKED_TRACKS, ApiEndpoints.MY_TRACK_LIKES);
    }

    @Provides
    @Named("PlaylistLikesSyncer")
    LikesSyncer providePlaylistLikesSyncer(ApiClient apiClient, FetchPlaylistsCommand fetchPlaylists, LoadLikesCommand loadLikes,
                                           LoadLikesPendingAdditionCommand loadLikesPendingAddition, LoadLikesPendingRemovalCommand loadLikesPendingRemoval,
                                           StorePlaylistsCommand storePlaylists, StoreLikesCommand storeLikes, RemoveLikesCommand removeLikes,
                                           AccountOperations accountOperations) {
        return new LikesSyncer(apiClient, fetchPlaylists, loadLikes.with(Sounds.TYPE_PLAYLIST),
                loadLikesPendingAddition.with(Sounds.TYPE_PLAYLIST), loadLikesPendingRemoval.with(Sounds.TYPE_PLAYLIST), storePlaylists, storeLikes,
                removeLikes, accountOperations, ApiEndpoints.LIKED_PLAYLISTS, ApiEndpoints.MY_PLAYLIST_LIKES);
    }
}

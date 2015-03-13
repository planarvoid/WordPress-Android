package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module(complete = false, library = true)
public class PostsSyncModule {

    @Provides
    @Named("LoadPlaylistPosts")
    LoadLocalPostsCommand provideLoadLocalPlaylistPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Sounds.TYPE_PLAYLIST);
    }

    @Provides
    @Named("FetchPlaylistPosts")
    FetchPostsCommand provideFetchPlaylistPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_PLAYLIST_POSTS);
    }

    @Provides
    @Named("MyPlaylistPostsSyncer")
    PostsSyncer provideMyPlaylistPostsSyncer(@Named("LoadPlaylistPosts") LoadLocalPostsCommand loadLocalPosts,
                                             @Named("FetchPlaylistPosts") FetchPostsCommand fetchRemotePosts,
                                             StorePostsCommand storePostsCommand,
                                             RemovePostsCommand removePostsCommand,
                                             FetchPlaylistsCommand fetchPlaylistsCommand,
                                             StorePlaylistsCommand storePlaylistsCommand) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                fetchPlaylistsCommand, storePlaylistsCommand);
    }

    @Provides
    @Named("LoadTrackPosts")
    LoadLocalPostsCommand provideLoadLocalTrackPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Sounds.TYPE_TRACK);
    }

    @Provides
    @Named("FetchTrackPosts")
    FetchPostsCommand provideFetchTrackPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_TRACK_POSTS);
    }

    @Provides
    @Named("MyTrackPostsSyncer")
    PostsSyncer provideMyPostsSyncer(@Named("LoadTrackPosts") LoadLocalPostsCommand loadLocalPosts,
                                             @Named("FetchTrackPosts") FetchPostsCommand fetchRemotePosts,
                                             StorePostsCommand storePostsCommand,
                                             RemovePostsCommand removePostsCommand,
                                             FetchTracksCommand fetchTracksCommand,
                                             StoreTracksCommand storeTracksCommand) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                fetchTracksCommand, storeTracksCommand);
    }
}

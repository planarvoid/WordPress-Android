package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.storage.TableColumns.Sounds;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
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
                                             RemovePostsCommand removePostsCommand) {
        return new PostsSyncer(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand);
    }
}

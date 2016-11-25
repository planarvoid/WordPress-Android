package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@Module
public class PostsSyncModule {

    public static final String MY_PLAYLIST_POSTS_SYNCER = "MyPlaylistPostsSyncer";
    public static final String MY_TRACK_POSTS_SYNCER = "MyTrackPostsSyncer";
    public static final String LOAD_PLAYLIST_POSTS = "LoadPlaylistPosts";
    public static final String FETCH_PLAYLIST_POSTS = "FetchPlaylistPosts";
    public static final String LOAD_TRACK_POSTS = "LoadTrackPosts";
    public static final String FETCH_TRACK_POSTS = "FetchTrackPosts";

    @Provides
    @Named(LOAD_PLAYLIST_POSTS)
    LoadLocalPostsCommand provideLoadLocalPlaylistPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Tables.Sounds.TYPE_PLAYLIST);
    }

    @Provides
    @Named(FETCH_PLAYLIST_POSTS)
    FetchPostsCommand provideFetchPlaylistPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_PLAYLIST_POSTS);
    }

    @Provides
    @Named(MY_PLAYLIST_POSTS_SYNCER)
    PostsSyncer provideMyPlaylistPostsSyncer(@Named(LOAD_PLAYLIST_POSTS) LoadLocalPostsCommand loadLocalPosts,
                                             @Named(FETCH_PLAYLIST_POSTS) FetchPostsCommand fetchRemotePosts,
                                             StorePostsCommand storePostsCommand,
                                             RemovePostsCommand removePostsCommand,
                                             FetchPlaylistsCommand fetchPlaylistsCommand,
                                             StorePlaylistsCommand storePlaylistsCommand,
                                             EventBus eventBus) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                                 fetchPlaylistsCommand, storePlaylistsCommand, eventBus);
    }

    @Provides
    @Named(LOAD_TRACK_POSTS)
    LoadLocalPostsCommand provideLoadLocalTrackPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Tables.Sounds.TYPE_TRACK);
    }

    @Provides
    @Named(FETCH_TRACK_POSTS)
    FetchPostsCommand provideFetchTrackPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_TRACK_POSTS);
    }

    @Provides
    @Named(MY_TRACK_POSTS_SYNCER)
    PostsSyncer provideMyPostsSyncer(@Named(LOAD_TRACK_POSTS) LoadLocalPostsCommand loadLocalPosts,
                                     @Named(FETCH_TRACK_POSTS) FetchPostsCommand fetchRemotePosts,
                                     StorePostsCommand storePostsCommand,
                                     RemovePostsCommand removePostsCommand,
                                     FetchTracksCommand fetchTracksCommand,
                                     StoreTracksCommand storeTracksCommand,
                                     EventBus eventBus) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                                 fetchTracksCommand, storeTracksCommand, eventBus);
    }
}

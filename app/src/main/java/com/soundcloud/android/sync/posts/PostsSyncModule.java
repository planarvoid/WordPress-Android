package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.sync.commands.FetchPlaylistsCommand;
import com.soundcloud.android.sync.commands.FetchTracksCommand;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class PostsSyncModule {

    public static final String MY_PLAYLIST_POSTS_SYNCER = "MyPlaylistPostsSyncer";
    static final String MY_TRACK_POSTS_SYNCER = "MyTrackPostsSyncer";
    private static final String LOAD_PLAYLIST_POSTS = "LoadPlaylistPosts";
    private static final String FETCH_PLAYLIST_POSTS = "FetchPlaylistPosts";
    private static final String LOAD_TRACK_POSTS = "LoadTrackPosts";
    private static final String FETCH_TRACK_POSTS = "FetchTrackPosts";

    @Provides
    @Named(LOAD_PLAYLIST_POSTS)
    static LoadLocalPostsCommand provideLoadLocalPlaylistPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Tables.Sounds.TYPE_PLAYLIST);
    }

    @Provides
    @Named(FETCH_PLAYLIST_POSTS)
    static FetchPostsCommand provideFetchPlaylistPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_PLAYLIST_POSTS);
    }

    @Provides
    @Named(MY_PLAYLIST_POSTS_SYNCER)
    static PostsSyncer provideMyPlaylistPostsSyncer(@Named(LOAD_PLAYLIST_POSTS) LoadLocalPostsCommand loadLocalPosts,
                                             @Named(FETCH_PLAYLIST_POSTS) FetchPostsCommand fetchRemotePosts,
                                             StorePostsCommand storePostsCommand,
                                             RemovePostsCommand removePostsCommand,
                                             FetchPlaylistsCommand fetchPlaylistsCommand,
                                             StorePlaylistsCommand storePlaylistsCommand,
                                             EventBus eventBus) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                                 fetchPlaylistsCommand, storePlaylistsCommand::call, eventBus);
    }

    @Provides
    @Named(LOAD_TRACK_POSTS)
    static LoadLocalPostsCommand provideLoadLocalTrackPostsCommand(PropellerDatabase database) {
        return new LoadLocalPostsCommand(database, Tables.Sounds.TYPE_TRACK);
    }

    @Provides
    @Named(FETCH_TRACK_POSTS)
    static FetchPostsCommand provideFetchTrackPostsCommand(ApiClient apiClient) {
        return new FetchPostsCommand(apiClient).with(ApiEndpoints.MY_TRACK_POSTS);
    }

    @Provides
    @Named(MY_TRACK_POSTS_SYNCER)
    static PostsSyncer provideMyPostsSyncer(@Named(LOAD_TRACK_POSTS) LoadLocalPostsCommand loadLocalPosts,
                                     @Named(FETCH_TRACK_POSTS) FetchPostsCommand fetchRemotePosts,
                                     StorePostsCommand storePostsCommand,
                                     RemovePostsCommand removePostsCommand,
                                     FetchTracksCommand fetchTracksCommand,
                                     TrackRepository trackRepository,
                                     EventBus eventBus) {
        return new PostsSyncer<>(loadLocalPosts, fetchRemotePosts, storePostsCommand, removePostsCommand,
                                 fetchTracksCommand, trackRepository::storeTracks, eventBus);
    }
}

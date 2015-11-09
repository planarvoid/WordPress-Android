package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStrategy;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyPlaylistsSyncer implements SyncStrategy {

    private static final String TAG = "MyPlaylistsSyncer";

    private final PostsSyncer postsSyncer;
    private final LoadLocalPlaylistsCommand loadLocalPlaylists;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;
    private final ReplacePlaylistPostCommand replacePlaylist;
    private final ApiClient apiClient;
    private final EventBus eventBus;

    @Inject
    public MyPlaylistsSyncer(@Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER) PostsSyncer postsSyncer,
                             LoadLocalPlaylistsCommand loadLocalPlaylists,
                             LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
                             ReplacePlaylistPostCommand replacePlaylist, ApiClient apiClient,
                             EventBus eventBus) {
        this.postsSyncer = postsSyncer;
        this.loadLocalPlaylists = loadLocalPlaylists;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
        this.replacePlaylist = replacePlaylist;
        this.apiClient = apiClient;
        this.eventBus = eventBus;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        return postsSyncer.call(pushLocalPlaylists())
                ? ApiSyncResult.fromSuccessfulChange(uri)
                : ApiSyncResult.fromSuccessWithoutChange(uri);
    }

    private List<Urn> pushLocalPlaylists() throws Exception {
        final List<PropertySet> localPlaylists = loadLocalPlaylists.call();
        final List<Urn> postedPlaylistUrns = new ArrayList<>(localPlaylists.size());

        Log.d(TAG, "Local Playlist count : " + localPlaylists.size());
        for (PropertySet localPlaylist : localPlaylists) {
            final Urn playlistUrn = localPlaylist.get(PlaylistProperty.URN);
            final List<Urn> trackUrns = loadPlaylistTrackUrnsCommand.with(playlistUrn).call();

            final ApiRequest request = ApiRequest.post(ApiEndpoints.PLAYLISTS_CREATE.path())
                    .forPrivateApi(1)
                    .withContent(createPlaylistBody(localPlaylist, trackUrns))
                    .build();

            final ApiPlaylist newPlaylist = apiClient.fetchMappedResponse(request, ApiPlaylistWrapper.class).getApiPlaylist();
            publishPlaylistCreated(newPlaylist);
            replacePlaylist.with(Pair.create(playlistUrn, newPlaylist)).call();
            postedPlaylistUrns.add(newPlaylist.getUrn());
        }

        return postedPlaylistUrns;
    }

    private void publishPlaylistCreated(ApiPlaylist newPlaylist) {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromCreatePlaylist(EntityMetadata.from(newPlaylist)));
    }

    private Map<String, Object> createPlaylistBody(PropertySet localPlaylist, List<Urn> trackUrns) {
        final Map<String, Object> playlistBody = new ArrayMap<>(2);
        playlistBody.put("title", localPlaylist.get(PlaylistProperty.TITLE));
        playlistBody.put("public", !localPlaylist.get(PlaylistProperty.IS_PRIVATE));

        final Map<String, Object> requestBody = new ArrayMap<>(2);
        requestBody.put("playlist", playlistBody);
        requestBody.put("track_urns", Urns.toString(trackUrns));
        return requestBody;
    }
}

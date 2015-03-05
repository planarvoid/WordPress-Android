package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.util.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;

public class MyPlaylistsSyncer implements SyncStrategy {

    private final PostsSyncer postsSyncer;
    private final LoadLocalPlaylistsCommand loadLocalPlaylists;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;
    private final ReplacePlaylistCommand replacePlaylist;
    private final ApiClient apiClient;

    @Inject
    public MyPlaylistsSyncer(@Named("MyPlaylistPostsSyncer") PostsSyncer postsSyncer,
                             LoadLocalPlaylistsCommand loadLocalPlaylists,
                             LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
                             ReplacePlaylistCommand replacePlaylist, ApiClient apiClient) {
        this.postsSyncer = postsSyncer;
        this.loadLocalPlaylists = loadLocalPlaylists;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
        this.replacePlaylist = replacePlaylist;
        this.apiClient = apiClient;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        pushLocalPlaylists();
        return postsSyncer.call()
                ? ApiSyncResult.fromSuccessfulChange(uri)
                : ApiSyncResult.fromSuccessWithoutChange(uri);
    }

    private void pushLocalPlaylists() throws Exception {
        for (PropertySet localPlaylist : loadLocalPlaylists.call()) {
            final Urn playlistUrn = localPlaylist.get(PlaylistProperty.URN);
            final List<Urn> trackUrns = loadPlaylistTrackUrnsCommand.with(playlistUrn).call();

            final ApiRequest<ApiPlaylist> request = ApiRequest.Builder.<ApiPlaylist>post(ApiEndpoints.PLAYLISTS_CREATE.path())
                    .forPrivateApi(1)
                    .withContent(createPlaylistBody(localPlaylist, trackUrns))
                    .forResource(ApiPlaylist.class)
                    .build();

            final ApiPlaylist newPlaylist = apiClient.fetchMappedResponse(request);
            replacePlaylist.with(Pair.create(playlistUrn, newPlaylist)).call();
        }
    }

    private Map<String, Object> createPlaylistBody(PropertySet localPlaylist, List<Urn> trackUrns) {
        final Map<String, Object> playlistBody = new ArrayMap<>(2);
        playlistBody.put("title", localPlaylist.get(PlaylistProperty.TITLE));
        playlistBody.put("public", !localPlaylist.get(PlaylistProperty.IS_PRIVATE));

        final Map<String, Object> requestBody = new ArrayMap<>(2);
        requestBody.put("playlist", playlistBody);
        requestBody.put("track_urns", trackUrns);
        return requestBody;
    }
}

package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.events.EntityStateChangedEvent.fromPlaylistPushedToServer;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.LoadOfflinePlaylistsCommand;
import com.soundcloud.android.playlists.LoadPlaylistPendingRemovalCommand;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStrategy;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
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
    private final LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand;
    private final RemovePlaylistCommand removePlaylistCommand;
    private final ApiClient apiClient;
    private final LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    private final EventBus eventBus;

    @Inject
    public MyPlaylistsSyncer(@Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER) PostsSyncer postsSyncer,
                             LoadLocalPlaylistsCommand loadLocalPlaylists,
                             LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
                             ReplacePlaylistPostCommand replacePlaylist,
                             LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand,
                             RemovePlaylistCommand removePlaylistCommand,
                             ApiClient apiClient,
                             LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand,
                             SinglePlaylistSyncerFactory singlePlaylistSyncerFactory, EventBus eventBus) {
        this.postsSyncer = postsSyncer;
        this.loadLocalPlaylists = loadLocalPlaylists;
        this.loadPlaylistTrackUrnsCommand = loadPlaylistTrackUrnsCommand;
        this.replacePlaylist = replacePlaylist;
        this.loadPlaylistPendingRemovalCommand = loadPlaylistPendingRemovalCommand;
        this.removePlaylistCommand = removePlaylistCommand;
        this.apiClient = apiClient;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
        this.eventBus = eventBus;
        this.loadOfflinePlaylistsCommand = loadOfflinePlaylistsCommand;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        syncPendingRemovals();
        final List<Urn> pushedPlaylists = pushLocalPlaylists();
        final boolean postedPlaylistsChanged = postsSyncer.call(pushedPlaylists);
        final boolean offlinePlaylistsChanged = syncOfflinePlaylists();
        final boolean changed = postedPlaylistsChanged || offlinePlaylistsChanged;

        return changed
                ? ApiSyncResult.fromSuccessfulChange(uri)
                : ApiSyncResult.fromSuccessWithoutChange(uri);
    }

    private boolean syncOfflinePlaylists() {
        final List<Urn> offlinePlaylists = loadOfflinePlaylistsCommand.call(null);
        final List<Urn> updatedOfflinePlaylists = new ArrayList<>();
        for (Urn playlist : offlinePlaylists) {
            try {
                if (singlePlaylistSyncerFactory.create(playlist).call()) {
                    updatedOfflinePlaylists.add(playlist);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync offline playlist " + playlist);
            }
        }

        final boolean hasUpdatedPlaylists = !updatedOfflinePlaylists.isEmpty();
        if (hasUpdatedPlaylists) {
            eventBus.publish(EventQueue.SYNC_RESULT, SyncResult.success(SyncActions.SYNC_PLAYLIST, true, updatedOfflinePlaylists));
        }
        return hasUpdatedPlaylists;
    }

    private void syncPendingRemovals() {
        final List<Urn> removeUrns = loadPlaylistPendingRemovalCommand.call(null);
        for (Urn urn : removeUrns) {
            final ApiResponse response = apiClient.fetchResponse(ApiRequest.delete(ApiEndpoints.PLAYLISTS_DELETE.path(urn))
                    .forPrivateApi(1)
                    .build());
            if (response.isSuccess() || isErrorIgnored(response)) {
                removePlaylistCommand.call(urn);
            }
        }
    }

    private boolean isErrorIgnored(ApiResponse response) {
        return response.getStatusCode() >= HttpStatus.BAD_REQUEST
                && response.getStatusCode() < HttpStatus.INTERNAL_SERVER_ERROR;
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
            replacePlaylist.with(Pair.create(playlistUrn, newPlaylist)).call();
            publishPlaylistCreated(playlistUrn, newPlaylist);
            postedPlaylistUrns.add(newPlaylist.getUrn());
        }

        return postedPlaylistUrns;
    }

    private void publishPlaylistCreated(Urn localPlaylistUrn, ApiPlaylist newPlaylist) {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromCreatePlaylist(EntityMetadata.from(newPlaylist)));

        final EntityStateChangedEvent event = fromPlaylistPushedToServer(localPlaylistUrn, newPlaylist.toPropertySet());
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);
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

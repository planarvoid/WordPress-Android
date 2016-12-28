package com.soundcloud.android.sync.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncer;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.v4.util.ArrayMap;
import android.util.Pair;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@AutoFactory(allowSubclasses = true)
class MyPlaylistsSyncer implements Callable<Boolean> {

    private static final String TAG = "MyPlaylistsSyncer";

    private final PostsSyncer postsSyncer;
    private final LoadLocalPlaylistsCommand loadLocalPlaylists;
    private final LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand;
    private final ReplacePlaylistPostCommand replacePlaylist;
    private final LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand;
    private final RemovePlaylistCommand removePlaylistCommand;
    private final ApiClient apiClient;
    private final LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    private final boolean syncOfflinePlaylists;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    private final EventBus eventBus;

    public MyPlaylistsSyncer(@Provided @Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER) PostsSyncer postsSyncer,
                             @Provided LoadLocalPlaylistsCommand loadLocalPlaylists,
                             @Provided LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrnsCommand,
                             @Provided ReplacePlaylistPostCommand replacePlaylist,
                             @Provided LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand,
                             @Provided RemovePlaylistCommand removePlaylistCommand,
                             @Provided ApiClient apiClient,
                             @Provided LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand,
                             @Provided SinglePlaylistSyncerFactory singlePlaylistSyncerFactory,
                             @Provided EventBus eventBus,
                             boolean isUiRequest) {
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
        this.syncOfflinePlaylists = !isUiRequest;
    }

    @Override
    public Boolean call() throws Exception {
        syncPendingRemovals();
        final List<Urn> pushedPlaylists = pushLocalPlaylists();
        final boolean postedPlaylistsChanged = postsSyncer.call(pushedPlaylists);
        final boolean offlinePlaylistsChanged = syncOfflinePlaylists && syncOfflinePlaylists();
        return postedPlaylistsChanged || offlinePlaylistsChanged;
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
            eventBus.publish(EventQueue.SYNC_RESULT,
                             SyncJobResult.success(Syncable.PLAYLIST.name(), true, updatedOfflinePlaylists));
        }
        return hasUpdatedPlaylists;
    }

    private void syncPendingRemovals() {
        final List<Urn> removeUrns = loadPlaylistPendingRemovalCommand.call(null);
        for (Urn urn : removeUrns) {
            final ApiResponse response = apiClient.fetchResponse(ApiRequest.delete(ApiEndpoints.PLAYLISTS_DELETE.path(urn))
                    .forPrivateApi()
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
        final List<PlaylistItem> localPlaylists = loadLocalPlaylists.call();
        final List<Urn> postedPlaylistUrns = new ArrayList<>(localPlaylists.size());

        Log.d(TAG, "Local Playlist count : " + localPlaylists.size());
        for (PlaylistItem localPlaylist : localPlaylists) {
            final Urn playlistUrn = localPlaylist.getUrn();
            final List<Urn> trackUrns = loadPlaylistTrackUrnsCommand.with(playlistUrn).call();

            final ApiRequest request = ApiRequest.post(ApiEndpoints.PLAYLISTS_CREATE.path())
                                                 .forPrivateApi()
                                                 .withContent(createPlaylistBody(localPlaylist, trackUrns))
                                                 .build();

            final ApiPlaylist newPlaylist = apiClient.fetchMappedResponse(request, ApiPlaylistWrapper.class)
                                                     .getApiPlaylist();
            replacePlaylist.with(Pair.create(playlistUrn, newPlaylist)).call();
            publishPlaylistCreated(playlistUrn, newPlaylist);
            postedPlaylistUrns.add(newPlaylist.getUrn());
        }

        return postedPlaylistUrns;
    }

    private void publishPlaylistCreated(Urn localPlaylistUrn, ApiPlaylist newPlaylist) {
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromCreatePlaylist(EntityMetadata.from(newPlaylist)));

        final EntityStateChangedEvent event = newPlaylist.toPushedEvent(localPlaylistUrn);
        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, event);
    }

    private Map<String, Object> createPlaylistBody(PlaylistItem localPlaylist, List<Urn> trackUrns) {
        final Map<String, Object> playlistBody = new ArrayMap<>(2);
        playlistBody.put("title", localPlaylist.getTitle());
        playlistBody.put("public", !localPlaylist.isPrivate());

        final Map<String, Object> requestBody = new ArrayMap<>(2);
        requestBody.put("playlist", playlistBody);
        requestBody.put("track_urns", Urns.toString(trackUrns));
        return requestBody;
    }
}

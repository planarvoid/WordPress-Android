package com.soundcloud.android.sync.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.LoadOfflinePlaylistsCommand;
import com.soundcloud.android.playlists.LoadPlaylistPendingRemovalCommand;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistStorage;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PlaylistStorage playlistStorage;
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
                             @Provided PlaylistStorage playlistStorage,
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
        this.playlistStorage = playlistStorage;
    }

    @Override
    public Boolean call() throws Exception {
        syncPendingRemovals();
        final boolean postedPlaylistsChanged = postsSyncer.call(pushLocalPlaylists());
        final boolean playlistContentChanged = syncPlaylistsContent(getPlaylistsToSync());
        return postedPlaylistsChanged || playlistContentChanged;
    }

    private Set<Urn> getPlaylistsToSync() {
        final Set<Urn> playlists = playlistStorage.playlistWithTrackChanges();
        if (syncOfflinePlaylists) {
            playlists.addAll(loadOfflinePlaylistsCommand.call(null));
        }
        return playlists;
    }

    private boolean syncPlaylistsContent(Collection<Urn> playlists) {
        final List<Urn> updatedPlaylists = new ArrayList<>();
        for (Urn playlist : playlists) {
            if (syncSinglePlaylistContent(playlist)) {
                updatedPlaylists.add(playlist);
            }
        }

        notifySyncPlaylistSuccesses(updatedPlaylists);
        return !updatedPlaylists.isEmpty();
    }

    private void notifySyncPlaylistSuccesses(List<Urn> updatedPlaylists) {
        if (!updatedPlaylists.isEmpty()) {
            final SyncJobResult event = SyncJobResult.success(Syncable.PLAYLIST.name(), true, updatedPlaylists);
            eventBus.publish(EventQueue.SYNC_RESULT, event);
        }
    }

    private boolean syncSinglePlaylistContent(Urn playlist) {
        try {
            return singlePlaylistSyncerFactory.create(playlist).call();
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync my playlist " + playlist);
            return false;
        }
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

        final PlaylistEntityChangedEvent event = PlaylistEntityChangedEvent.fromPlaylistPushedToServer(localPlaylistUrn, PlaylistItem.from(newPlaylist));
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, event);
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

package com.soundcloud.android.sync.playlists;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.TempEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistApiCreateObject;
import com.soundcloud.android.playlists.PlaylistApiUpdateObject;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class PlaylistSyncHelper {

    private final PlaylistStorage playlistStorage;
    private final SoundAssociationStorage soundAssociationStorage;
    private final ScModelManager modelManager;

    public PlaylistSyncHelper() {
        this(new PlaylistStorage(), new SoundAssociationStorage(), SoundCloudApplication.sModelManager);
    }

    @VisibleForTesting
    PlaylistSyncHelper(PlaylistStorage playlistStorage, SoundAssociationStorage soundAssociationStorage, ScModelManager modelManager) {
        this.playlistStorage = playlistStorage;
        this.soundAssociationStorage = soundAssociationStorage;
        this.modelManager = modelManager;
    }

    /* package */
    public int pushLocalPlaylists(Context context, PublicCloudAPI api, SyncStateManager syncStateManager) throws IOException {
        // check for local playlists that need to be pushed
        List<PublicApiPlaylist> playlistsToUpload = playlistStorage.getLocalPlaylists();

        for (PublicApiPlaylist p : playlistsToUpload) {
            Uri toDelete = p.toUri();

            final String content = getApiCreateJson(p);
            Log.d(ApiSyncService.LOG_TAG, "Pushing new playlist to api: " + content);

            Request r = Request.to(TempEndpoints.PLAYLISTS).withContent(content, "application/json");
            PublicApiPlaylist playlist;
            final PublicApiResource result = api.create(r);
            if (result instanceof PublicApiPlaylist) {
                playlist = (PublicApiPlaylist) result;
            } else {
                // Debugging. Return objects sometimes are not playlists. Need to log this to get more info about the cause of this
                ErrorUtils.handleSilentException("Error adding new playlist " + p, new PlaylistUpdateException(content));
                continue; // this will get retried next sync
            }

            // update local state
            p.updateFrom(playlist, PublicApiResource.CacheUpdateMode.FULL);
            modelManager.removeFromCache(toDelete);

            playlistStorage.store(playlist);
            soundAssociationStorage.addCreation(playlist);

            syncStateManager.updateLastSyncSuccessTime(p.toUri(), System.currentTimeMillis());

            // remove all traces of the old temporary playlist
            playlistStorage.removePlaylist(toDelete);
        }
        return playlistsToUpload.size();
    }

    public ApiSyncResult pullRemotePlaylists(PublicCloudAPI api) throws IOException {

        final Request request = Request.to(Content.ME_PLAYLISTS.remoteUri)
                .add("representation", "compact").with("limit", 200);

        List<PublicApiResource> resources = api.readFullCollection(request, PublicApiResource.ResourceHolder.class);

        // manually build the sound association holder
        List<SoundAssociation> associations = new ArrayList<SoundAssociation>();
        for (PublicApiResource resource : resources) {
            PublicApiPlaylist playlist = (PublicApiPlaylist) resource;

            // update metadata in MM with mini mode to avoid overwriting local tracks
            modelManager.cache(playlist, PublicApiResource.CacheUpdateMode.MINI);
            associations.add(new SoundAssociation(playlist));

            final List<Long> trackIds = playlistStorage.getPlaylistTrackIds(playlist.getId());
            final int localTrackCount = trackIds == null ? 0 : trackIds.size();
            final boolean shouldSyncTracks = localTrackCount != playlist.getTrackCount();

            if (shouldSyncTracks && playlist.getTrackCount() < PlaylistSyncer.MAX_MY_PLAYLIST_TRACK_COUNT_SYNC) {
                try {
                    playlist.tracks = api.readList(Request.to(TempEndpoints.PLAYLIST_TRACKS, playlist.getId()));
                } catch (IOException e) {
                    // don't let the track fetch fail the sync, it is just an optimization
                    Log.e(PlaylistSyncer.TAG, "Failed to fetch playlist tracks for playlist " + playlist, e);
                }
            }
        }

        boolean changed = soundAssociationStorage.syncToLocal(associations, Content.ME_PLAYLISTS.uri);
        ApiSyncResult result = new ApiSyncResult(Content.ME_PLAYLISTS.uri);
        result.change = changed ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        result.setSyncData(System.currentTimeMillis(), associations.size());
        result.success = true;
        return result;
    }

    public String getApiCreateJson(PublicApiPlaylist playlist) throws JsonProcessingException {
        PlaylistApiCreateObject createObject = new PlaylistApiCreateObject(playlist);

        if (createObject.tracks.isEmpty()) {
            // add the tracks
            createObject.tracks = new ArrayList<ScModel>();
            List<Long> trackIds = playlistStorage.getPlaylistTrackIds(playlist.getId());

            for (Long id : trackIds){
                createObject.tracks.add(new ScModel(id));
            }
        }
        return createObject.toJson();
    }

    public void removePlaylist(Uri playlistUri){
        playlistStorage.removePlaylist(playlistUri);
        modelManager.removeFromCache(playlistUri);
    }


    /**
     * Fetch a remote playlist, push any changes, store and return the result
     * @param playlistUri
     * @param apiWrapper
     * @return
     * @throws IOException
     */
    public PublicApiPlaylist syncPlaylist(Uri playlistUri, /** inject this **/PublicCloudAPI apiWrapper) throws IOException {
        final PublicApiPlaylist playlist = resolvePlaylistWithAdditions(playlistUri, apiWrapper);
        modelManager.cache(playlist, PublicApiResource.CacheUpdateMode.FULL);

        logInvalidPlaylistTracks(playlist);

        return playlistStorage.store(playlist);
    }

    private void logInvalidPlaylistTracks(PublicApiPlaylist playlist) {
        Collection<Urn> tracksWithoutTitles = playlist.getTracksWithoutTitles();
        if (!tracksWithoutTitles.isEmpty()){
            final String message = String.format("Invalid playlist tracks fround. playlist : %s , tracks : %s",
                    playlist.getUrn(), TextUtils.join(", ", tracksWithoutTitles));
            ErrorUtils.handleSilentException(new IllegalStateException(message));
        }
    }

    private PublicApiPlaylist resolvePlaylistWithAdditions(Uri playlistUri, /** inject this **/PublicCloudAPI apiWrapper) throws IOException {
        PublicApiPlaylist playlist = safeFetchPlaylist(apiWrapper, playlistUri);

        List<Long> unpushedTracks = playlistStorage.getUnpushedTracksForPlaylist(playlist.getId());
        if (!unpushedTracks.isEmpty()) {
            Set<Long> toAdd = new LinkedHashSet<Long>(unpushedTracks.size());
            for (PublicApiTrack t : playlist.getTracks()) {
                toAdd.add(t.getId());
            }
            toAdd.addAll(unpushedTracks);

            final String content = new PlaylistApiUpdateObject(toAdd).toJson();
            Log.d(ApiSyncService.LOG_TAG, "Pushing new playlist content to api: " + content);

            Request r = Content.PLAYLIST.request(playlistUri).withContent(content, "application/json");
            final PublicApiResource scResource = apiWrapper.update(r);
            if (scResource instanceof PublicApiPlaylist){
                return (PublicApiPlaylist) scResource;
            } else {
                // Debugging. Return objects sometimes are not playlists. In this case the user will lose addition.
                // This is an edge case so I think its acceptable until we can figure out the root of the problem [JS]
                ErrorUtils.handleSilentException("Error updating playlist " + playlist, new PlaylistUpdateException(content));
                return playlist;
            }
        }
        return playlist;
    }

    private PublicApiPlaylist safeFetchPlaylist(PublicCloudAPI apiWrapper, Uri playlistUri) throws IOException {
        PublicApiResource scResource = apiWrapper.read(Content.match(playlistUri).request(playlistUri));
        if (scResource instanceof PublicApiPlaylist){
            return (PublicApiPlaylist) scResource;
        } else {
            // log for debugging and throw
            final UnknownResourceException exception = new UnknownResourceException(playlistUri);
            ErrorUtils.handleSilentException("Error retrieving playlist " + playlistUri, exception);
            throw exception;

        }
    }

}

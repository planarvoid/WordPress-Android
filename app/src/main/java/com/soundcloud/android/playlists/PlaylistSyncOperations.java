package com.soundcloud.android.playlists;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.api.TempEndpoints;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.exception.PlaylistUpdateException;
import com.soundcloud.android.sync.exception.UnknownResourceException;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlaylistSyncOperations {

    private final PlaylistStorage mPlaylistStorage;
    private final SoundAssociationStorage mSoundAssociationStorage;
    private final ScModelManager mModelManager;

    public PlaylistSyncOperations() {
        this(new PlaylistStorage(), new SoundAssociationStorage(), SoundCloudApplication.sModelManager);
    }

    @VisibleForTesting
    PlaylistSyncOperations(PlaylistStorage playlistStorage, SoundAssociationStorage soundAssociationStorage, ScModelManager modelManager) {
        mPlaylistStorage = playlistStorage;
        mSoundAssociationStorage = soundAssociationStorage;
        mModelManager = modelManager;
    }

    /* package */
    public int pushLocalPlaylists(Context context, PublicCloudAPI api, SyncStateManager syncStateManager) throws IOException {
        // check for local playlists that need to be pushed
        List<Playlist> playlistsToUpload = mPlaylistStorage.getLocalPlaylists();

        for (Playlist p : playlistsToUpload) {
            Uri toDelete = p.toUri();

            final String content = getApiCreateJson(p);
            Log.d(ApiSyncService.LOG_TAG, "Pushing new playlist to api: " + content);

            Request r = Request.to(TempEndpoints.PLAYLISTS).withContent(content, "application/json");
            Playlist playlist;
            final ScResource result = api.create(r);
            if (result instanceof Playlist) {
                playlist = (Playlist) result;
            } else {
                // Debugging. Return objects sometimes are not playlists. Need to log this to get more info about the cause of this
                SoundCloudApplication.handleSilentException("Error adding new playlist " + p, new PlaylistUpdateException(content));
                continue; // this will get retried next sync
            }

            // update local state
            p.localToGlobal(context, playlist);
            mModelManager.removeFromCache(toDelete);

            mPlaylistStorage.store(playlist);
            mSoundAssociationStorage.addCreation(playlist);

            syncStateManager.updateLastSyncSuccessTime(p.toUri(), System.currentTimeMillis());

            // remove all traces of the old temporary playlist
            mPlaylistStorage.removePlaylist(toDelete);
        }
        return playlistsToUpload.size();
    }

    public String getApiCreateJson(Playlist playlist) throws JsonProcessingException {
        PlaylistApiCreateObject createObject = new PlaylistApiCreateObject(playlist);

        if (createObject.tracks.isEmpty()) {
            // add the tracks
            createObject.tracks = new ArrayList<ScModel>();
            List<Long> trackIds = mPlaylistStorage.getPlaylistTrackIds(playlist.getId());

            for (Long id : trackIds){
                createObject.tracks.add(new ScModel(id));
            }
        }
        return createObject.toJson();
    }

    public boolean syncMyNewPlaylists(List<SoundAssociation> associations){
        return mSoundAssociationStorage.syncToLocal(associations, Content.ME_PLAYLISTS.uri);
    }

    public void removePlaylist(Uri playlistUri){
        mPlaylistStorage.removePlaylist(playlistUri);
        mModelManager.removeFromCache(playlistUri);
    }


    /**
     * Fetch a remote playlist, push any changes, store and return the result
     * @param playlistUri
     * @param apiWrapper
     * @return
     * @throws IOException
     */
    public Playlist syncPlaylist(Uri playlistUri, /** inject this **/PublicCloudAPI apiWrapper) throws IOException {
        final Playlist playlist = resolvePlaylistWithAdditions(playlistUri, apiWrapper);
        final Playlist store = mPlaylistStorage.store(playlist);
        return store;
    }

    private Playlist resolvePlaylistWithAdditions(Uri playlistUri, /** inject this **/PublicCloudAPI apiWrapper) throws IOException {
        Playlist playlist = safeFetchPlaylist(apiWrapper, playlistUri);

        List<Long> unpushedTracks = mPlaylistStorage.getUnpushedTracksForPlaylist(playlist.getId());
        if (!unpushedTracks.isEmpty()) {
            Set<Long> toAdd = new LinkedHashSet<Long>(unpushedTracks.size());
            for (Track t : playlist.getTracks()) {
                toAdd.add(t.getId());
            }
            toAdd.addAll(unpushedTracks);

            final String content = new PlaylistApiUpdateObject(toAdd).toJson();
            Log.d(ApiSyncService.LOG_TAG, "Pushing new playlist content to api: " + content);

            Request r = Content.PLAYLIST.request(playlistUri).withContent(content, "application/json");
            final ScResource scResource = apiWrapper.update(r);
            if (scResource instanceof Playlist){
                return (Playlist) scResource;
            } else {
                // Debugging. Return objects sometimes are not playlists. In this case the user will lose addition.
                // This is an edge case so I think its acceptable until we can figure out the root of the problem [JS]
                SoundCloudApplication.handleSilentException("Error updating playlist " + playlist, new PlaylistUpdateException(content));
                return playlist;
            }
        }
        return playlist;
    }

    private Playlist safeFetchPlaylist(PublicCloudAPI apiWrapper, Uri playlistUri) throws IOException {
        ScResource scResource = apiWrapper.read(Content.match(playlistUri).request(playlistUri));
        if (scResource instanceof Playlist){
            return (Playlist) scResource;
        } else {
            // log for debugging and throw
            final UnknownResourceException exception = new UnknownResourceException(playlistUri);
            SoundCloudApplication.handleSilentException("Error retrieving playlist " + playlistUri, exception);
            throw exception;

        }
    }

}

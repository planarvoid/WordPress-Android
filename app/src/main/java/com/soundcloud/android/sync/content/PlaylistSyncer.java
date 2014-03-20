package com.soundcloud.android.sync.content;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.api.TempEndpoints;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistSyncer extends SyncStrategy {

    @VisibleForTesting
    public static final int MAX_MY_PLAYLIST_TRACK_COUNT_SYNC = 30;

    private final PlaylistSyncHelper mPlaylistSyncHelper;

    public PlaylistSyncer(Context context, ContentResolver resolver) {
        this(context, resolver, new PublicApi(context),  new SyncStateManager(resolver, new LocalCollectionDAO(resolver)),
                new AccountOperations(context), new PlaylistSyncHelper());
    }

    // TODO : Inject when we add injection to the syncer
    public PlaylistSyncer(Context context, ContentResolver resolver, PublicCloudAPI publicCloudAPI, SyncStateManager syncStateManager,
                          AccountOperations accountOperations, PlaylistSyncHelper playlistOperations) {

        super(context, resolver, publicCloudAPI, syncStateManager, accountOperations);
        mPlaylistSyncHelper = playlistOperations;
    }

    public ApiSyncResult syncContent(@NotNull Uri uri) throws IOException {
        return syncContent(uri, null);
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException {
        if (!isLoggedIn()) {
            Log.w(TAG, "Invalid user id, skipping sync ");
            return new ApiSyncResult(uri);
        }

        switch (Content.match(uri)){
            case ME_PLAYLISTS:
                return syncMyPlaylists();

            case PLAYLIST:
                return syncPlaylist(uri);

            default:
                // default to unsuccessful sync result
                return new ApiSyncResult(uri);
        }
    }

    /**
     * Pushes any locally created playlists to the server, fetches the user's playlists from the server,
     * and fetches tracks for these playlists that are missing locally.
     * <p/>
     * This is specific because the Api does not return these as sound associations, otherwise
     * we could use that path
     */
    private ApiSyncResult syncMyPlaylists() throws IOException {
        mPlaylistSyncHelper.pushLocalPlaylists(mContext, mApi, mSyncStateManager);

        final Request request = Request.to(Content.ME_PLAYLISTS.remoteUri)
                .add("representation", "compact").with("limit", 200);

        List<ScResource> resources = mApi.readFullCollection(request, ScResource.ScResourceHolder.class);

        // manually build the sound association holder
        List<SoundAssociation> associations = new ArrayList<SoundAssociation>();

        for (ScResource resource : resources) {
            Playlist playlist = (Playlist) resource;
            associations.add(new SoundAssociation(playlist));
            boolean onWifi = IOUtils.isWifiConnected(mContext);

            // if we have never synced the playlist or are on wifi and past the stale time, fetch the tracks
            final LocalCollection localCollection = mSyncStateManager.fromContent(playlist.toUri());
            final boolean playlistStale = (localCollection.shouldAutoRefresh() && onWifi) || !localCollection.hasSyncedBefore();

            if (playlistStale && playlist.getTrackCount() < MAX_MY_PLAYLIST_TRACK_COUNT_SYNC) {
                try {
                    playlist.tracks = mApi.readList(Request.to(TempEndpoints.PLAYLIST_TRACKS, playlist.getId()));
                } catch (IOException e) {
                    // don't let the track fetch fail the sync, it is just an optimization
                    Log.e(TAG, "Failed to fetch playlist tracks for playlist " + playlist, e);
                }
            }
        }

        boolean changed = mPlaylistSyncHelper.syncMyNewPlaylists(associations);
        ApiSyncResult result = new ApiSyncResult(Content.ME_PLAYLISTS.uri);
        result.change = changed ? ApiSyncResult.CHANGED : ApiSyncResult.UNCHANGED;
        result.setSyncData(System.currentTimeMillis(), associations.size());
        result.success = true;
        return result;

    }

    private ApiSyncResult syncPlaylist(Uri contentUri) throws IOException {
        log("Syncing playlist " + contentUri);
        ApiSyncResult result = new ApiSyncResult(contentUri);
        try {
            Playlist playlist = mPlaylistSyncHelper.syncPlaylist(contentUri, mApi);
            if (playlist != null) {
                log("inserted " + playlist.toString());
                result.setSyncData(true, System.currentTimeMillis(), 1, ApiSyncResult.CHANGED);
            } else {
                log("failed to create to " + contentUri);
                result.success = false;
            }
            return result;

        } catch (PublicCloudAPI.NotFoundException e) {
            log("Received a 404 on playlist, deleting " + contentUri.toString());
            mPlaylistSyncHelper.removePlaylist(contentUri);
            result.setSyncData(true, System.currentTimeMillis(), 0, ApiSyncResult.CHANGED);
            return result;
        }
    }
}

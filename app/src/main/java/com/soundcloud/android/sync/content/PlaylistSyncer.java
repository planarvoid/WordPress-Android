package com.soundcloud.android.sync.content;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;

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
     * Pushes any locally created playlists to the server, fetches the user's playlists from the server.
     */
    private ApiSyncResult syncMyPlaylists() throws IOException {
        mPlaylistSyncHelper.pushLocalPlaylists(mContext, mApi, mSyncStateManager);
        return mPlaylistSyncHelper.pullRemotePlaylists(mApi);

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

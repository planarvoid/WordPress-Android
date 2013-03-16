package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.UriUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PlaylistDAO extends BaseDAO<Playlist> {
    public PlaylistDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    /**
     * delete any caching, and mark any local instances as removed
     * {@link com.soundcloud.android.activity.track.PlaylistActivity#onPlaylistChanged()}
     * @param playlistUri the playlistUri
     */
    public void removePlaylist(Uri playlistUri) {
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(playlistUri);
        if (p != null) {
            p.removed = true;
            SoundCloudApplication.MODEL_MANAGER.removeFromCache(p.toUri());
        }
        removePlaylistFromDb(mResolver, UriUtils.getLastSegmentAsLong(playlistUri));
    }

    public Uri insertAsMyPlaylist(Playlist playlist) {
        playlist.insert(mResolver);
        // association so it appears in ME_SOUNDS, ME_PLAYLISTS, etc.
        return new SoundAssociation(playlist, new Date(System.currentTimeMillis()), SoundAssociation.Type.PLAYLIST)
                .insert(mResolver, Content.ME_PLAYLISTS.uri);
    }

    // Local i.e. unpushed playlists are currently identified by having a negative timestamp
    public boolean hasLocalPlaylists() {
        Cursor itemsCursor = mResolver.query(Content.PLAYLISTS.uri,
                new String[]{DBHelper.SoundView._ID}, DBHelper.SoundView._ID + " < 0",
                null, null);

        boolean hasPlaylists = false;
        if (itemsCursor != null) {
            hasPlaylists = itemsCursor.getCount() > 0;
            itemsCursor.close();
        }
        return hasPlaylists;
    }

    // Local i.e. unpushed playlists are currently identified by having a negative timestamp
    public List<Playlist> getLocalPlaylists() {
        Cursor itemsCursor = mResolver.query(Content.PLAYLISTS.uri,
                null, DBHelper.SoundView._ID + " < 0",
                null, DBHelper.SoundView._ID + " DESC");

        List<Playlist> playlists = new ArrayList<Playlist>();
        if (itemsCursor != null) {
            while (itemsCursor.moveToNext()) {
                playlists.add(SoundCloudApplication.MODEL_MANAGER.getCachedPlaylistFromCursor(itemsCursor));
            }

        }
        if (itemsCursor != null) itemsCursor.close();
        return playlists;
    }

    public Uri addTrackToPlaylist(Playlist playlist, long trackId){
        return addTrackToPlaylist(playlist, trackId,System.currentTimeMillis());
    }

    public Uri addTrackToPlaylist(Playlist playlist, long trackId, long time){
        playlist.setTrackCount(playlist.getTrackCount() + 1);

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlist.id);
        cv.put(DBHelper.PlaylistTracks.TRACK_ID, trackId);
        cv.put(DBHelper.PlaylistTracks.ADDED_AT, time);
        cv.put(DBHelper.PlaylistTracks.POSITION, playlist.getTrackCount());
        return mResolver.insert(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.id)), cv);
    }

    public int removePlaylistFromDb(ContentResolver resolver, long playlistId){

        final String playlistIdString = String.valueOf(playlistId);
        int deleted = resolver.delete(Content.PLAYLIST.forQuery(playlistIdString), null, null);
        deleted += resolver.delete(Content.PLAYLIST_TRACKS.forQuery(playlistIdString), null, null);

        // delete from collections
        String where = DBHelper.CollectionItems.ITEM_ID + " = " + playlistId + " AND "
                + DBHelper.CollectionItems.RESOURCE_TYPE + " = " + Playable.DB_TYPE_PLAYLIST;

        deleted += resolver.delete(Content.ME_PLAYLISTS.uri, where, null);
        deleted += resolver.delete(Content.ME_SOUNDS.uri, where, null);
        deleted += resolver.delete(Content.ME_LIKES.uri, where, null);

        // delete from activities
        where = DBHelper.Activities.SOUND_ID + " = " + playlistId + " AND " +
                DBHelper.ActivityView.TYPE + " IN ( " + Activity.getDbPlaylistTypesForQuery() + " ) ";
        deleted += resolver.delete(Content.ME_ALL_ACTIVITIES.uri, where, null);

        return deleted;
    }

    @Override
    public Content getContent() {
        return Content.PLAYLISTS;
    }
}

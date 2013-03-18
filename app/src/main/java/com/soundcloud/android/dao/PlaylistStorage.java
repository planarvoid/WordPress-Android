package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistStorage {
    private final ContentResolver mResolver;
    private final PlaylistDAO mPlaylistDAO;
    private final TrackDAO mTrackDAO;


    public long create(Playlist playlist) {
        long id = mPlaylistDAO.create(playlist);
        if (playlist.tracks != null) {
            for (Track t : playlist.tracks) {
                mTrackDAO.create(t);
            }
        }
        return id;
    }

    public boolean update(Playlist playlist) {
        return mPlaylistDAO.update(playlist);
    }

    public PlaylistStorage(ContentResolver resolver) {
        mResolver = resolver;
        mPlaylistDAO = new PlaylistDAO(resolver);
        mTrackDAO = new TrackDAO(resolver);
    }

    public @Nullable Playlist getPlaylistWithTracks(long playlistId) {
        Playlist playlist = mPlaylistDAO.queryForId(playlistId);
        if (playlist != null) {
            playlist.tracks = loadPlaylistTracks(playlistId);
        }
        return playlist;
    }

    public @Nullable Playlist getPlaylistWithTracks(Uri uri) {
        return getPlaylistWithTracks(Long.parseLong(uri.getLastPathSegment()));
    }

    public List<Track> loadPlaylistTracks(long playlistId){
        List<Track> tracks = new ArrayList<Track>();
        Cursor cursor = mResolver.query(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)), null, null, null, null);
        if (cursor == null) return tracks;
        while (cursor.moveToNext()) {
            tracks.add(new Track(cursor));
        }
        cursor.close();
        return tracks;
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

    public Playlist createNewPlaylist(User user, boolean isPrivate, List<Track> tracks) {
        final String title = "new playlist " + System.currentTimeMillis();
        final long[] trackIds = new long[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            trackIds[i] = mTrackDAO.create(tracks.get(i));
        }
        return createPlaylist(user, title, isPrivate, trackIds);
    }


    public Playlist createPlaylist(User user, String title, boolean isPrivate, long... trackIds) {
        Playlist p = new Playlist(-System.currentTimeMillis());
        p.user = user;
        p.title = title;
        p.sharing = isPrivate ? Sharing.PRIVATE : Sharing.PUBLIC;
        p.created_at = new Date(System.currentTimeMillis());
        p.setTrackCount(trackIds.length);
        p.tracks = new ArrayList<Track>();

        for (long trackId : trackIds){
            Track track = SoundCloudApplication.MODEL_MANAGER.getCachedTrack(trackId);
            p.tracks.add(track == null ? new Track(trackId) : track);
        }
        p.insert(mResolver);
        return p;
    }


    public @Nullable Set<Uri> getPlaylistsDueForSync() {
        Cursor c = mResolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{DBHelper.PlaylistTracks.PLAYLIST_ID},
                DBHelper.PlaylistTracks.ADDED_AT + " IS NOT NULL AND " + DBHelper.PlaylistTracks.PLAYLIST_ID + " > 0", null, null);

        if (c != null) {
            Set<Uri> uris = new HashSet<Uri>();
            while (c.moveToNext()) {
                uris.add(Content.PLAYLIST.forId(c.getLong(0)));
            }
            c.close();
            return uris;
        }
        return null;
    }

    public Playlist getPlaylist(long id) {
        return mPlaylistDAO.queryForId(id);

    }
}

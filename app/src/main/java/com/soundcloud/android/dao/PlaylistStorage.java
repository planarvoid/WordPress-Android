package com.soundcloud.android.dao;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistStorage extends ScheduledOperations implements Storage<Playlist> {
    private final ContentResolver mResolver;
    private final PlaylistDAO mPlaylistDAO;

    public PlaylistStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mPlaylistDAO = new PlaylistDAO(mResolver);
        subscribeOn(ScSchedulers.STORAGE_SCHEDULER);
    }

    /**
     * Takes a playlist and stores it in the database. This will not only create a playlist record in the Sounds table
     * but also create records in Sounds for every track in the playlist as well as a record for every track in the
     * PlaylistTracks join table.
     *
     * @param playlist the playlist to store
     */
    @Override
    public Observable<Playlist> create(final Playlist playlist) {
        return schedule(Observable.create(new Func1<Observer<Playlist>, Subscription>() {
            @Override
            public Subscription call(Observer<Playlist> observer) {
                mPlaylistDAO.create(playlist);
                observer.onNext(playlist);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    /**
     * Convenience method to store a new playlist a user has just created on their device.
     *
     * @see #create(com.soundcloud.android.model.Playlist)
     */
    public Observable<Playlist> createNewUserPlaylist(User user, String title, boolean isPrivate, long... trackIds) {
        ArrayList<Track> tracks = new ArrayList<Track>(trackIds.length);
        for (long trackId : trackIds){
            Track track = SoundCloudApplication.MODEL_MANAGER.getCachedTrack(trackId);
            tracks.add(track == null ? new Track(trackId) : track);
        }

        Playlist playlist = Playlist.newUserPlaylist(user, title, isPrivate, tracks);
        return create(playlist);
    }

    public Observable<Playlist> loadPlaylistWithTracks(final long playlistId) {
        return schedule(Observable.create(new Func1<Observer<Playlist>, Subscription>() {
            @Override
            public Subscription call(final Observer<Playlist> observer) {
                final Playlist playlist = mPlaylistDAO.queryById(playlistId);
                if (playlist != null) {
                    loadPlaylistTracks(playlistId).toList().subscribe(new Action1<List<Track>>() {
                        @Override
                        public void call(List<Track> tracks) {
                            playlist.tracks = tracks;
                            observer.onNext(playlist);
                            observer.onCompleted();
                        }
                    });
                } else {
                    observer.onCompleted();
                }
                return Subscriptions.empty();
            }
        }));
    }

    //TODO: use DAO, not ContentResolver
    public Observable<Track> loadPlaylistTracks(final long playlistId) {
        return schedule(Observable.create(new Func1<Observer<Track>, Subscription>() {
            @Override
            public Subscription call(Observer<Track> observer) {
                Cursor cursor = mResolver.query(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)), null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        observer.onNext(new Track(cursor));
                    }
                    cursor.close();
                }
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Uri addTrackToPlaylist(Playlist playlist, long trackId){
        return addTrackToPlaylist(playlist, trackId,System.currentTimeMillis());
    }

    public Uri addTrackToPlaylist(Playlist playlist, long trackId, long time){
        playlist.setTrackCount(playlist.getTrackCount() + 1);

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(DBHelper.PlaylistTracks.TRACK_ID, trackId);
        cv.put(DBHelper.PlaylistTracks.ADDED_AT, time);
        cv.put(DBHelper.PlaylistTracks.POSITION, playlist.getTrackCount());
        return mResolver.insert(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId())), cv);
    }


    /**
     * Remove a playlist and all associations such as likes, reposts or sharings from the database.
     */
    public void removePlaylist(Uri playlistUri) {
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(playlistUri);
        if (p != null) {
            p.removed = true;
            SoundCloudApplication.MODEL_MANAGER.removeFromCache(p.toUri());
        }
        long playlistId = UriUtils.getLastSegmentAsLong(playlistUri);

        final String playlistIdString = String.valueOf(playlistId);
        mResolver.delete(Content.PLAYLIST.forQuery(playlistIdString), null, null);
        mResolver.delete(Content.PLAYLIST_TRACKS.forQuery(playlistIdString), null, null);

        // delete from collections
        String where = DBHelper.CollectionItems.ITEM_ID + " = " + playlistId + " AND "
                + DBHelper.CollectionItems.RESOURCE_TYPE + " = " + Playable.DB_TYPE_PLAYLIST;

        mResolver.delete(Content.ME_PLAYLISTS.uri, where, null);
        mResolver.delete(Content.ME_SOUNDS.uri, where, null);
        mResolver.delete(Content.ME_LIKES.uri, where, null);

        // delete from activities
        where = DBHelper.Activities.SOUND_ID + " = " + playlistId + " AND " +
                DBHelper.ActivityView.TYPE + " IN ( " + Activity.getDbPlaylistTypesForQuery() + " ) ";
        mResolver.delete(Content.ME_ALL_ACTIVITIES.uri, where, null);
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
        return mPlaylistDAO.queryById(id);

    }
}

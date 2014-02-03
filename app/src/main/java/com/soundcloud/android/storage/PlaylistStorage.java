package com.soundcloud.android.storage;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistStorage extends ScheduledOperations implements Storage<Playlist> {
    private final ContentResolver mResolver;
    private final PlaylistDAO mPlaylistDAO;
    private final TrackDAO mTrackDAO;
    private final ScModelManager mModelManager;

    @Deprecated
    public PlaylistStorage() {
        this(SoundCloudApplication.instance.getContentResolver(),
                new PlaylistDAO(SoundCloudApplication.instance.getContentResolver()),
                new TrackDAO(SoundCloudApplication.instance.getContentResolver()),
                SoundCloudApplication.sModelManager);
    }

    @Inject
    public PlaylistStorage(ContentResolver resolver, PlaylistDAO playlistDAO, TrackDAO trackDAO, ScModelManager modelManager) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        mResolver = resolver;
        mPlaylistDAO = playlistDAO;
        mTrackDAO = trackDAO;
        mModelManager = modelManager;
    }

    public Playlist loadPlaylist(long playlistId) throws NotFoundException {
        final Playlist playlist = mPlaylistDAO.queryById(playlistId);
        if (playlist == null) {
            throw new NotFoundException(playlistId);
        } else {
            return mModelManager.cache(playlist);
        }
    }

    public Observable<Playlist> loadPlaylistAsync(final long playlistId) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Playlist>() {
            @Override
            public Subscription onSubscribe(Observer<? super Playlist> observer) {
                try {
                    observer.onNext(loadPlaylist(playlistId));
                    observer.onCompleted();
                } catch (NotFoundException e) {
                    observer.onError(e);
                }
                return Subscriptions.empty();
            }
        }));
    }

    public Playlist loadPlaylistWithTracks(long playlistId) throws NotFoundException {
        final Playlist playlist = loadPlaylist(playlistId);
        playlist.tracks = loadTracksForPlaylist(playlist);
        return playlist;
    }

    public Observable<Playlist> loadPlaylistWithTracksAsync(final long playlistId) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Playlist>() {
            @Override
            public Subscription onSubscribe(Observer<? super Playlist> observer) {
                try {
                    observer.onNext(loadPlaylistWithTracks(playlistId));
                    observer.onCompleted();
                } catch (NotFoundException e) {
                    observer.onError(e);
                }
                return Subscriptions.empty();
            }
        }));
    }

    @Override
    public Playlist store(Playlist playlist) {
        mPlaylistDAO.create(playlist);
        return playlist;
    }

    /**
     * Takes a playlist and stores it in the database. This will not only create a playlist record in the Sounds table
     * but also create records in Sounds for every track in the playlist as well as a record for every track in the
     * PlaylistTracks join table.
     *
     * @param playlist the playlist to store
     */
    @Override
    public Observable<Playlist> storeAsync(final Playlist playlist) {
        return schedule(Observable.create(new Observable.OnSubscribeFunc<Playlist>() {
            @Override
            public Subscription onSubscribe(Observer<? super Playlist> observer) {
                observer.onNext(store(playlist));
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    /**
     * Convenience method to store a new playlist a user has just created on their device.
     *
     * @see #storeAsync(com.soundcloud.android.model.Playlist)
     */
    public Observable<Playlist> createNewUserPlaylistAsync(User user, String title, boolean isPrivate, long... trackIds) {
        ArrayList<Track> tracks = new ArrayList<Track>(trackIds.length);
        for (long trackId : trackIds){
            Track track = mModelManager.getCachedTrack(trackId);
            tracks.add(track == null ? new Track(trackId) : track);
        }

        Playlist playlist = Playlist.newUserPlaylist(user, title, isPrivate, tracks);
        return storeAsync(playlist);
    }

    public Playlist addTrackToPlaylist(Playlist playlist, long trackId) {
        return addTrackToPlaylist(playlist, trackId, System.currentTimeMillis());
    }

    public Playlist addTrackToPlaylist(Playlist playlist, long trackId, long timeAdded) {
        playlist.setTrackCount(playlist.getTrackCount() + 1);

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(DBHelper.PlaylistTracks.TRACK_ID, trackId);
        cv.put(DBHelper.PlaylistTracks.ADDED_AT, timeAdded);
        cv.put(DBHelper.PlaylistTracks.POSITION, playlist.getTrackCount());
        mResolver.insert(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId())), cv);

        return playlist;
    }

    /**
     * Remove a playlist and all associations such as likes, reposts or sharings from the database.
     */
    public void removePlaylist(Uri playlistUri) {
        Playlist p = mModelManager.getPlaylist(playlistUri);
        if (p != null) {
            p.removed = true;
            mModelManager.removeFromCache(p.toUri());
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

        if (itemsCursor != null) {
            List<Playlist> playlists = new ArrayList<Playlist>(itemsCursor.getCount());
            while (itemsCursor.moveToNext()) {
                playlists.add(mModelManager.getCachedPlaylistFromCursor(itemsCursor));
            }
            itemsCursor.close();

            for (Playlist p : playlists) {
                p.tracks = loadTracksForPlaylist(p);
            }
            return playlists;

        } else {
            return Collections.emptyList();
        }
    }

    private List<Track> loadTracksForPlaylist(Playlist playlist) {
        final List<Track> tracks = mTrackDAO.queryAllByUri(Content.PLAYLIST_TRACKS.forId(playlist.getId()));
        // make sure we loops database records through the cache
        return Lists.newArrayList(Lists.transform(tracks, new Function<Track, Track>() {
            @Override
            public Track apply(Track input) {
                return mModelManager.cache(input);
            }
        }));
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

    public @Nullable List<Long> getPlaylistTrackIds(long playlistId) {
        return mTrackDAO.queryIdsByUri(Content.PLAYLIST_TRACKS.forId(playlistId));
    }

    public List<Long> getUnpushedTracksForPlaylist(long playlistId) {
        Cursor cursor = mResolver.query(
                Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)),
                new String[]{DBHelper.PlaylistTracksView._ID},
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " IS NOT NULL", null,
                DBHelper.PlaylistTracksView.PLAYLIST_ADDED_AT + " ASC");

        if (cursor != null) {
            List<Long> ids = Lists.newArrayList();
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
            cursor.close();
            return ids;
        }
        return null;
    }
}

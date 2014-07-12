package com.soundcloud.android.storage;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscriber;

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

public class PlaylistStorage extends ScheduledOperations implements Storage<PublicApiPlaylist> {
    private final ContentResolver resolver;
    private final PlaylistDAO playlistDAO;
    private final TrackDAO trackDAO;
    private final ScModelManager modelManager;

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
        this.resolver = resolver;
        this.playlistDAO = playlistDAO;
        this.trackDAO = trackDAO;
        this.modelManager = modelManager;
    }

    public PublicApiPlaylist loadPlaylist(long playlistId) throws NotFoundException {
        final PublicApiPlaylist playlist = playlistDAO.queryById(playlistId);
        if (playlist == null) {
            throw new NotFoundException(playlistId);
        } else {
            return modelManager.cache(playlist);
        }
    }

    public Observable<PublicApiPlaylist> loadPlaylistAsync(final long playlistId) {
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiPlaylist>() {
            @Override
            public void call(Subscriber<? super PublicApiPlaylist> observer) {
                try {
                    observer.onNext(loadPlaylist(playlistId));
                    observer.onCompleted();
                } catch (NotFoundException e) {
                    observer.onError(e);
                }
            }
        }));
    }

    public PublicApiPlaylist loadPlaylistWithTracks(long playlistId) throws NotFoundException {
        final PublicApiPlaylist playlist = loadPlaylist(playlistId);
        playlist.tracks = loadTracksForPlaylist(playlist);

        return modelManager.cache(playlist, PublicApiResource.CacheUpdateMode.FULL);
    }

    public Observable<PublicApiPlaylist> loadPlaylistWithTracksAsync(final long playlistId) {
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiPlaylist>() {
            @Override
            public void call(Subscriber<? super PublicApiPlaylist> observer) {
                try {
                    observer.onNext(loadPlaylistWithTracks(playlistId));
                    observer.onCompleted();
                } catch (NotFoundException e) {
                    observer.onError(e);
                }
            }
        }));
    }

    @Override
    public PublicApiPlaylist store(PublicApiPlaylist playlist) {
        playlistDAO.create(playlist);
        if (playlist.getTrackCount() == 0){
            // this needs to be run on an empty playlist, if not empty, they would have been removed by a bulkInsert of the new tracks
            resolver.delete(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId())), null, null);
        }

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
    public Observable<PublicApiPlaylist> storeAsync(final PublicApiPlaylist playlist) {
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiPlaylist>() {
            @Override
            public void call(Subscriber<? super PublicApiPlaylist> observer) {
                observer.onNext(store(playlist));
                observer.onCompleted();
            }
        }));
    }

    /**
     * Convenience method to store a new playlist a user has just created on their device.
     *
     * @see #storeAsync(com.soundcloud.android.api.legacy.model.PublicApiPlaylist)
     */
    public Observable<PublicApiPlaylist> createNewUserPlaylistAsync(PublicApiUser user, String title, boolean isPrivate, long... trackIds) {
        ArrayList<PublicApiTrack> tracks = new ArrayList<PublicApiTrack>(trackIds.length);
        for (long trackId : trackIds){
            PublicApiTrack track = modelManager.getCachedTrack(trackId);
            tracks.add(track == null ? new PublicApiTrack(trackId) : track);
        }

        PublicApiPlaylist playlist = PublicApiPlaylist.newUserPlaylist(user, title, isPrivate, tracks);
        return storeAsync(playlist);
    }

    public PublicApiPlaylist addTrackToPlaylist(PublicApiPlaylist playlist, long trackId) {
        return addTrackToPlaylist(playlist, trackId, System.currentTimeMillis());
    }

    public PublicApiPlaylist addTrackToPlaylist(PublicApiPlaylist playlist, long trackId, long timeAdded) {
        playlist.setTrackCount(playlist.getTrackCount() + 1);
        modelManager.cache(playlist, PublicApiResource.CacheUpdateMode.MINI);

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlist.getId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, trackId);
        cv.put(TableColumns.PlaylistTracks.ADDED_AT, timeAdded);
        cv.put(TableColumns.PlaylistTracks.POSITION, playlist.getTrackCount());
        resolver.insert(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlist.getId())), cv);

        return playlist;
    }

    /**
     * Remove a playlist and all associations such as likes, reposts or sharings from the database.
     */
    public void removePlaylist(Uri playlistUri) {
        PublicApiPlaylist p = modelManager.getPlaylist(playlistUri);
        if (p != null) {
            p.removed = true;
            modelManager.removeFromCache(p.toUri());
        }
        long playlistId = UriUtils.getLastSegmentAsLong(playlistUri);

        final String playlistIdString = String.valueOf(playlistId);
        resolver.delete(Content.PLAYLIST.forQuery(playlistIdString), null, null);
        resolver.delete(Content.PLAYLIST_TRACKS.forQuery(playlistIdString), null, null);

        // delete from collections
        String where = TableColumns.CollectionItems.ITEM_ID + " = " + playlistId + " AND "
                + TableColumns.CollectionItems.RESOURCE_TYPE + " = " + Playable.DB_TYPE_PLAYLIST;

        resolver.delete(Content.ME_PLAYLISTS.uri, where, null);
        resolver.delete(Content.ME_SOUNDS.uri, where, null);
        resolver.delete(Content.ME_LIKES.uri, where, null);

        // delete from activities
        where = TableColumns.Activities.SOUND_ID + " = " + playlistId + " AND " +
                TableColumns.ActivityView.TYPE + " IN ( " + Activity.getDbPlaylistTypesForQuery() + " ) ";
        resolver.delete(Content.ME_ALL_ACTIVITIES.uri, where, null);
    }

    // Local i.e. unpushed playlists are currently identified by having a negative timestamp
    public boolean hasLocalPlaylists() {
        Cursor itemsCursor = resolver.query(Content.PLAYLISTS.uri,
                new String[]{TableColumns.SoundView._ID}, TableColumns.SoundView._ID + " < 0",
                null, null);

        boolean hasPlaylists = false;
        if (itemsCursor != null) {
            hasPlaylists = itemsCursor.getCount() > 0;
            itemsCursor.close();
        }
        return hasPlaylists;
    }

    // Local i.e. unpushed playlists are currently identified by having a negative timestamp
    public List<PublicApiPlaylist> getLocalPlaylists() {
        Cursor itemsCursor = resolver.query(Content.PLAYLISTS.uri,
                null, TableColumns.SoundView._ID + " < 0",
                null, TableColumns.SoundView._ID + " DESC");

        if (itemsCursor != null) {
            List<PublicApiPlaylist> playlists = new ArrayList<PublicApiPlaylist>(itemsCursor.getCount());
            while (itemsCursor.moveToNext()) {
                playlists.add(modelManager.getCachedPlaylistFromCursor(itemsCursor));
            }
            itemsCursor.close();

            for (PublicApiPlaylist p : playlists) {
                p.tracks = loadTracksForPlaylist(p);
            }
            return playlists;

        } else {
            return Collections.emptyList();
        }
    }

    private List<PublicApiTrack> loadTracksForPlaylist(PublicApiPlaylist playlist) {
        final List<PublicApiTrack> tracks = trackDAO.queryAllByUri(Content.PLAYLIST_TRACKS.forId(playlist.getId()));
        // make sure we loops database records through the cache
        return Lists.newArrayList(Lists.transform(tracks, new Function<PublicApiTrack, PublicApiTrack>() {
            @Override
            public PublicApiTrack apply(PublicApiTrack input) {
                return modelManager.cache(input);
            }
        }));
    }

    public @Nullable Set<Uri> getPlaylistsDueForSync() {
        Cursor c = resolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{TableColumns.PlaylistTracks.PLAYLIST_ID},
                TableColumns.PlaylistTracks.ADDED_AT + " IS NOT NULL AND " + TableColumns.PlaylistTracks.PLAYLIST_ID + " > 0", null, null);

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
        return trackDAO.queryIdsByUri(Content.PLAYLIST_TRACKS.forId(playlistId));
    }

    public List<Long> getUnpushedTracksForPlaylist(long playlistId) {
        Cursor cursor = resolver.query(
                Content.PLAYLIST_ALL_TRACKS.uri,
                new String[]{ TableColumns.PlaylistTracks.TRACK_ID },
                TableColumns.PlaylistTracks.ADDED_AT + " IS NOT NULL AND " + TableColumns.PlaylistTracks.PLAYLIST_ID + " = ?",
                new String[]{ String.valueOf(playlistId) },
                TableColumns.PlaylistTracks.ADDED_AT + " ASC");

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

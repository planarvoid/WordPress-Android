package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscriber;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class PlaylistStorage extends ScheduledOperations {
    private final ContentResolver resolver;
    private final PlaylistDAO playlistDAO;
    private final ScModelManager modelManager;

    @Deprecated
    public PlaylistStorage() {
        this(SoundCloudApplication.instance.getContentResolver(),
                new PlaylistDAO(SoundCloudApplication.instance.getContentResolver()),
                SoundCloudApplication.sModelManager);
    }

    @Inject
    public PlaylistStorage(ContentResolver resolver, PlaylistDAO playlistDAO, ScModelManager modelManager) {
        super(ScSchedulers.STORAGE_SCHEDULER);
        this.resolver = resolver;
        this.playlistDAO = playlistDAO;
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

    @Nullable
    public Set<Uri> getPlaylistsDueForSync() {
        Cursor c = resolver.query(Content.PLAYLIST_ALL_TRACKS.uri, new String[]{TableColumns.PlaylistTracks.PLAYLIST_ID},
                TableColumns.PlaylistTracks.ADDED_AT + " IS NOT NULL AND " + TableColumns.PlaylistTracks.PLAYLIST_ID + " > 0", null, null);

        if (c != null) {
            Set<Uri> uris = new HashSet<>();
            while (c.moveToNext()) {
                uris.add(Content.PLAYLIST.forId(c.getLong(0)));
            }
            c.close();
            return uris;
        }
        return null;
    }

}

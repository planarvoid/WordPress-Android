package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.activity.track.PlaylistActivity;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.api.Params;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Playlist extends Playable {

    public static final String EXTRA = "com.soundcloud.android.playlist";
    public static final String EXTRA_ID = "com.soundcloud.android.playlist_id";
    public static final String EXTRA_TRACKS_COUNT = "com.soundcloud.android.playlist_tracks";

    public static final String ACTION_CONTENT_CHANGED = "com.soundcloud.android.playlist.content_changed";

    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) @Nullable public List<Track> tracks;
    @JsonView(Views.Full.class) public int track_count;


    public static Playlist fromIntent(Intent intent) {
        Playlist playlist = intent.getParcelableExtra(EXTRA);
        if (playlist == null) {
            playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(intent.getLongExtra(EXTRA_ID, 0));
            if (playlist == null) {
                throw new IllegalArgumentException("Could not obtain playlist from intent " + intent);
            }
        }
        return playlist;
    }

    public Playlist() {
        super();
    }

    public Playlist(long id) {
        super(id);
    }

    public Playlist(Parcel in) {
        Bundle b = in.readBundle(getClass().getClassLoader());
        super.readFromBundle(b);

        playlist_type = b.getString("playlist_type");
        tracks_uri = b.getString("tracks_uri");
        track_count = b.getInt("track_count");
        tracks = b.getParcelableArrayList("tracks");
    }

    public Playlist(Cursor cursor) {
        super(cursor);
        tracks_uri = cursor.getString(cursor.getColumnIndex(DBHelper.Sounds.TRACKS_URI));
        track_count = cursor.getInt(cursor.getColumnIndex(DBHelper.Sounds.TRACK_COUNT));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = super.getBundle();
        b.putString("playlist_type", playlist_type);
        b.putString("tracks_uri", tracks_uri);
        b.putInt("track_count", track_count);
        b.putParcelableArrayList("tracks", (ArrayList<? extends Parcelable>) tracks);
        dest.writeBundle(b);
    }

    public Playlist updateFrom(Playlist updatedItem, CacheUpdateMode cacheUpdateMode) {
        super.updateFrom(updatedItem, cacheUpdateMode);
        tracks_uri = updatedItem.tracks_uri;
        track_count = updatedItem.track_count;
        playlist_type = updatedItem.playlist_type;
        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            tracks = updatedItem.tracks;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", user=" + user +
                ", track_count=" + (track_count == -1 ? (tracks != null ? tracks.size() : "-1") : track_count) +
                ", tracks_uri='" + tracks_uri + '\'' +
                '}';
    }

    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Sounds.TRACKS_URI, tracks_uri);
        cv.put(DBHelper.Sounds.TRACK_COUNT, track_count);
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAYLISTS.uri;
    }

    @Override
    public ScResource getRefreshableResource() {
        return this;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destMap) {
        super.putDependencyValues(destMap);
        if (tracks != null) {
            int i = 0;
            for (Track t : tracks) {
                t.putFullContentValues(destMap);

                // add to relationship table
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.PlaylistTracks.TRACK_ID,t.id);
                cv.put(DBHelper.PlaylistTracks.POSITION,i);
                destMap.add(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(id)), cv);
                i++;
            }
        }
    }

    @Override
    public Uri toUri() {
        return Content.PLAYLISTS.forQuery(String.valueOf(id));
    }


    @Override
    public int getTypeId() {
        return DB_TYPE_PLAYLIST;
    }

    public Uri insertAsMyPlaylist(ContentResolver resolver){
        insert(resolver);

        // association so it appears in ME_SOUNDS, ME_PLAYLISTS, etc.
        return new SoundAssociation(this, new Date(System.currentTimeMillis()), SoundAssociation.Type.PLAYLIST)
                .insert(resolver, Content.ME_PLAYLISTS.uri);
    }

    @Override
    public Intent getViewIntent() {
        return PlaylistActivity.getIntent(this);
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    public static boolean hasLocalPlaylists(ContentResolver resolver) {
        Cursor itemsCursor = resolver.query(Content.PLAYLISTS.uri,
                new String[]{DBHelper.SoundView._ID}, DBHelper.SoundView._ID + " < 0",
                null, null);

        boolean hasPlaylists = false;
        if (itemsCursor != null) {
            hasPlaylists = itemsCursor.getCount() > 0;
            itemsCursor.close();
        }
        return hasPlaylists;
    }

    public static List<Playlist> getLocalPlaylists(ContentResolver resolver) {
        Cursor itemsCursor = resolver.query(Content.PLAYLISTS.uri,
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

    public boolean isLocal() {
        return id < 0;
    }

    @JsonRootName("playlist")
    public static class ApiCreateObject{

        @JsonView(Views.Full.class) String title;
        @JsonView(Views.Full.class) String sharing;
        @JsonView(Views.Full.class) public List<ScModel> tracks;
        public ApiCreateObject(Playlist p) {

            this.title = p.title;
            this.sharing =  p.sharing == Sharing.PRIVATE ? Params.Track.PRIVATE : Params.Track.PUBLIC;

            if (p.tracks != null){
                // convert to ScModel as we only want to serialize the id
                this.tracks = new ArrayList<ScModel>();
                for (Track t : p.tracks){
                    tracks.add(new ScModel(t.id));
                }
            }
        }

        public String toJson(ObjectMapper mapper) throws JsonProcessingException {
            return mapper.writeValueAsString(this);
        }
    }

    @JsonRootName("playlist")
    public static class ApiUpdateObject {
        @JsonView(Views.Full.class) List<ScModel> tracks;

        public ApiUpdateObject(List<Long> toAdd) {
            this.tracks = new ArrayList<ScModel>();
            for (Long id : toAdd){
                this.tracks.add(new ScModel(id));
            }
        }

        public String toJson(ObjectMapper mapper) throws IOException {
            return mapper.writeValueAsString(this);
        }
    }

    public static Uri addTrackToPlaylist(ContentResolver resolver, long playlistId, long trackId){
        return addTrackToPlaylist(resolver, playlistId, trackId,System.currentTimeMillis());
    }

    public static Uri addTrackToPlaylist(ContentResolver resolver, long playlistId, long trackId, long time){
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.PlaylistTracks.PLAYLIST_ID, playlistId);
        cv.put(DBHelper.PlaylistTracks.TRACK_ID, trackId);
        cv.put(DBHelper.PlaylistTracks.ADDED_AT, time);
        return resolver.insert(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)), cv);
    }

    public static int removePlaylist(ContentResolver resolver, long playlistId){
        final String playlistIdString = String.valueOf(playlistId);
        int deleted = resolver.delete(Content.PLAYLIST.forQuery(playlistIdString), null, null);
        deleted += resolver.delete(Content.PLAYLIST_TRACKS.forQuery(playlistIdString), null, null);
        deleted += resolver.delete(Content.ME_PLAYLISTS.forQuery(playlistIdString),null,null);
        return deleted;
    }

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.playlist;
    }

    public boolean isStreamable() {
        return true;
    }

    /**
     * Change listening. Playlist IDs are mutable, so we listen on the actual instance instead of content uri's
     */
    private OnChangeListener mChangeListener;
    private Set<WeakReference<OnChangeListener>> mListenerWeakReferences;

    public interface OnChangeListener {
        void onPlaylistChanged();
        Handler getHandler();
    }
    private ContentObserver mPlaylistObserver;
    public synchronized void startObservingChanges(@NotNull ContentResolver contentResolver, @NotNull OnChangeListener listener) {
        if (mPlaylistObserver == null) {
            mPlaylistObserver = new ContentObserver(listener.getHandler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    notifyChangeListeners();
                }
            };
        }
        if (mListenerWeakReferences == null) {
            // start observing
            contentResolver.registerContentObserver(toUri(), false, mPlaylistObserver);
            mListenerWeakReferences = new HashSet<WeakReference<OnChangeListener>>();
        } else {

            // see if this listener already exists
            for (WeakReference<OnChangeListener> listenerRef : mListenerWeakReferences) {
                final OnChangeListener candidate = listenerRef.get();
                if (candidate != null && candidate == listener) {
                    return; // already listening
                }
            }
        }
        mListenerWeakReferences.add(new WeakReference<OnChangeListener>(listener));
    }

    public synchronized void stopObservingChanges(ContentResolver contentResolver, OnChangeListener listener) {

        if (mListenerWeakReferences != null) {
            List<WeakReference<OnChangeListener>> toRemove = new ArrayList<WeakReference<OnChangeListener>>();
            for (WeakReference<OnChangeListener> listenerRef : mListenerWeakReferences) {
                final OnChangeListener candidate = listenerRef.get();
                if (candidate == null || candidate == listener) {
                    toRemove.add(listenerRef);
                }
            }
            mListenerWeakReferences.removeAll(toRemove);

            if (mListenerWeakReferences.isEmpty()) {
                // stop observing
                contentResolver.unregisterContentObserver(mPlaylistObserver);
                mListenerWeakReferences = null;
            }
        }

    }

    /**
     * When a playlist goes from local to global, the id changes. We have to re-register the content observer
     * @param context
     * @param updated
     */
    public void localToGlobal(Context context, Playlist updated){
        Uri oldUri = toUri();
        final ContentResolver resolver = context.getContentResolver();
        updateFrom(updated, ScResource.CacheUpdateMode.FULL);
        if (mListenerWeakReferences == null && mPlaylistObserver != null) {
            resolver.unregisterContentObserver(mPlaylistObserver);
            resolver.registerContentObserver(toUri(), false, mPlaylistObserver);
        }

        // do not call notifyChangeListeners directly as we may be on a thread
        resolver.notifyChange(toUri(),null);
        PlayQueueManager.onPlaylistUriChanged(context,oldUri,toUri());
    }



    private void notifyChangeListeners() {
        if (mListenerWeakReferences != null) {
            for (WeakReference<OnChangeListener> listenerRef : mListenerWeakReferences) {
                final OnChangeListener listener = listenerRef.get();
                if (listener != null) {
                    listener.onPlaylistChanged();
                }
            }
        }
    }

}

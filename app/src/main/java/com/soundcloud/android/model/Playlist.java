package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.http.json.Views;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.utils.ScTextUtils;
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
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Playlist extends Playable {

    public static final String EXTRA = "com.soundcloud.android.playlist";
    public static final String EXTRA_ID = "com.soundcloud.android.playlist_id";
    public static final String EXTRA_URI = "com.soundcloud.android.playlist_uri";
    public static final String EXTRA_TRACKS_COUNT = "com.soundcloud.android.playlist_tracks";

    public static final String ACTION_CONTENT_CHANGED = "com.soundcloud.android.playlist.content_changed";

    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) public List<Track> tracks = new ArrayList<Track>(0);
    @JsonView(Views.Full.class) private int track_count;
    public boolean removed;

    public Playlist(PlaylistSummary playlist) {
        setUrn(playlist.getUrn());
        setUser(new User(playlist.getUser()));
        setTitle(playlist.getTitle());
        artwork_url = playlist.getArtworkUrl();
        tag_list = playlist.getTags() == null ? ScTextUtils.EMPTY_STRING : TextUtils.join(" ", playlist.getTags());
        created_at = playlist.getCreatedAt();
        duration = playlist.getDuration();
        track_count = playlist.getTrackCount();
        sharing = Sharing.from(playlist.isPublic());
        PlayableStats stats = playlist.getStats();
        if (stats != null) {
            likes_count = stats.getLikesCount();
            reposts_count = stats.getRepostsCount();
        }
    }

    public interface OnChangeListener { void onPlaylistChanged(); }

    private Set<WeakReference<OnChangeListener>> mListenerWeakReferences;
    private ContentObserver mPlaylistObserver;

    public static @Nullable Playlist fromBundle(Bundle bundle) {
        Playlist playlist;
        if (bundle.containsKey(EXTRA)) {
            playlist = bundle.getParcelable(EXTRA);
        } else if (bundle.containsKey(EXTRA_ID)) {
            playlist = SoundCloudApplication.sModelManager.getPlaylist(bundle.getLong(EXTRA_ID, 0));
        } else if (bundle.containsKey(EXTRA_URI)) {
            Uri uri = (Uri) bundle.getParcelable(EXTRA_URI);
            playlist = SoundCloudApplication.sModelManager.getPlaylist(uri);
        } else {
            throw new IllegalArgumentException("Could not obtain playlist from bundle");
        }
        return playlist;
    }

    public static @Nullable Playlist fromIntent(Intent intent) {
        return fromBundle(intent.getExtras());
    }

    /**
     * Helper to instantiate a playlist the given user created locally. This playlist will have a negative timestamp
     * to indicate that it hasn't been synced to the API yet.
     */
    public static Playlist newUserPlaylist(User user, String title, boolean isPrivate, @NotNull List<Track> tracks) {
        Playlist playlist = new Playlist(-System.currentTimeMillis());
        playlist.user = user;
        playlist.title = title;
        playlist.sharing = isPrivate ? Sharing.PRIVATE : Sharing.PUBLIC;
        playlist.created_at = new Date();
        playlist.tracks = tracks;
        playlist.track_count = tracks.size();
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
        if (tracks == null) {
            tracks = new LinkedList<Track>();
        }
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
                "id=" + getId() +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", user=" + user +
                ", track_count=" + (track_count == -1 ? tracks.size() : track_count) +
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
    public Refreshable getRefreshableResource() {
        return this;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destMap) {
        super.putDependencyValues(destMap);
        int i = 0;
        for (Track t : tracks) {
            t.putFullContentValues(destMap);

            // add to relationship table
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.PlaylistTracks.TRACK_ID, t.getId());
            cv.put(DBHelper.PlaylistTracks.POSITION, i);
            destMap.add(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(getId())), cv);
            i++;
        }
    }

    @Override
    public Uri toUri() {
        return Content.PLAYLISTS.forQuery(String.valueOf(getId()));
    }

    public List<Track> getTracks() {
        return tracks;
    }

    @Override
    public int getTypeId() {
        return DB_TYPE_PLAYLIST;
    }

    @Override
    public Intent getViewIntent() {
        return PlaylistDetailActivity.getIntent(this, Screen.DEEPLINK);
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

    public boolean isLocal() {
        return getId() < 0;
    }

    @Override
    public boolean isStale() {
        return System.currentTimeMillis() - last_updated > Consts.ResourceStaleTimes.playlist;
    }

    public boolean isStreamable() {
        return true;
    }


    public int getTrackCount() {
        return Math.max(tracks.size(), track_count);
    }

    @JsonProperty("track_count")
    public void setTrackCount(int count) {
        track_count = count;
    }

    /**
     * Change listening. Playlist IDs are mutable, so we listen on the actual instance instead of content uri's
     */

    public synchronized void startObservingChanges(@NotNull ContentResolver contentResolver, @NotNull OnChangeListener listener) {
        if (mPlaylistObserver == null) {
            mPlaylistObserver = new ContentObserver(new Handler()) {
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

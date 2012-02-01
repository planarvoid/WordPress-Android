package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

public class PlaylistManager {
    public static final String PREF_PLAYLIST_URI = "sc_playlist_uri";

    // these will be stored as uri parameters
    public static final String PARAM_PLAYLIST_POS = "playlistPos";
    public static final String PARAM_SEEK_POS = "seekPos";
    public static final String PARAM_TRACK_ID = "trackId";

    private Track[] mPlaylist = new Track[0];
    private Cursor mTrackCursor;
    private Uri mPlaylistUri;

    private int mPlayPos;
    private Context mContext;
    private TrackCache mCache;
    private static final int DEFAULT_PLAYLIST = 0;
    static final Uri DEFAULT_PLAYLIST_URI = Content.PLAYLIST.forId(DEFAULT_PLAYLIST);
    private long mUserId;

    public PlaylistManager(Context context, TrackCache cache, long userId) {
        mCache = cache;
        mContext = context;
        mUserId = userId;
    }

    public int length() {
        return mPlaylist.length;
    }

    public boolean isEmpty() {
        return mPlaylist.length == 0;
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mPlaylist.length) {
            mPlayPos = playPos;
            return true;
        } else {
            return false;
        }
    }

    public Track getTrack() {
        return getTrackAt(mPlayPos);
    }

    public Track getTrackAt(int pos) {
        if (pos >= 0 && pos < mPlaylist.length) {
            if (mPlaylist[pos] == null){
                if (mTrackCursor != null && !mTrackCursor.isClosed() && mTrackCursor.moveToPosition(pos)){
                    mPlaylist[pos] = new Track(mTrackCursor);
                }
            }
            mCache.put(mPlaylist[pos]);
            return mPlaylist[pos];

        } else {
            return null;
        }
    }

    public long getTrackIdAt(int pos) {
        Track t = getTrackAt(pos);
        return t== null ? -1 : t.id;
    }

    public boolean prev() {
        if (mPlayPos == 0)
            return false;
        mPlayPos--;
        return true;
    }

    public boolean next() {
        if (mPlayPos >= length()-1) return false;
        mPlayPos++;
        return true;
    }

    public Track getPrev() {
        if (mPlayPos > 0) {
            return getTrackAt(mPlayPos - 1);
        } else {
            return null;
        }
    }

    public Track getNext() {
        if (mPlayPos < length() - 1) {
            return getTrackAt(mPlayPos + 1);
        } else {
            return null;
        }
    }

    public void setTrack(Track toBePlayed) {
        mCache.put(toBePlayed);
        mPlaylist = new Track[]{toBePlayed};
        mPlaylistUri = null;
        mPlayPos = 0;
    }

    public void setUri(Uri uri, int position) {
        mPlaylistUri = uri;
        if (mTrackCursor != null){
            if (!mTrackCursor.isClosed()) mTrackCursor.close();
        }
        mTrackCursor = mContext.getContentResolver().query(uri, null, null, null, null);
        mPlaylist = new Track[mTrackCursor.getCount()];
        if (position >= 0 && position < mTrackCursor.getCount()){
            mPlayPos = position;
            mTrackCursor.moveToPosition(position);
        }
    }

    public void setPlaylist(final List<? extends Parcelable> playlist, int playPos) {
        // cache a new tracklist
        mPlaylist = new Track[playlist.size()];

        int i = 0;
        for (Parcelable p : playlist){
            if (p instanceof Track) {
                mPlaylist[i] = (Track) p;
            } else if (p instanceof Activity) {
                mPlaylist[i] = ((Activity) p).getTrack();
            } else {
                // not playable, must be a recording.
                // ignore it and decrease play index to account for it
                playPos--;
                continue;
            }
            i++;
        }

        mPlaylistUri = DEFAULT_PLAYLIST_URI;
        mPlayPos = Math.max(0,playPos);

        // TODO, only do this on exit???
        //noinspection unchecked
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mContext.getContentResolver().delete(DEFAULT_PLAYLIST_URI,null,null);
                SoundCloudDB.bulkInsertParcelables(mContext.getContentResolver(), playlist, DEFAULT_PLAYLIST_URI, mUserId);
                return null;
            }
        }.execute();
    }

    public void clear() {
        mPlaylist = new Track[0];
    }

    public void saveQueue(long seekPos) {
        if (SoundCloudApplication.getUserIdFromContext(mContext) >= 0) {
            PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString(PREF_PLAYLIST_URI, getPlaylistState(seekPos).toString())
                    .commit();
        }
    }

    /* package */ Uri getPlaylistState(long seekPos) {
        Uri playlistState = mPlaylistUri == null ?
                DEFAULT_PLAYLIST_URI : mPlaylistUri;
        Uri.Builder builder = playlistState.buildUpon();
        final Track currentTrack = getTrack();
        if (currentTrack != null) {
            builder.appendQueryParameter(PARAM_TRACK_ID, String.valueOf(currentTrack.id));
        }
        builder.appendQueryParameter(PARAM_PLAYLIST_POS, String.valueOf(mPlayPos));
        builder.appendQueryParameter(PARAM_SEEK_POS, String.valueOf(seekPos));
        return builder.build();
    }

    public long reloadQueue() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String lastUri = preferences.getString(PREF_PLAYLIST_URI, null);
        if (!TextUtils.isEmpty(lastUri)){
            final Uri uri = Uri.parse(lastUri);
            setUri(uri, extractValue(uri, PARAM_PLAYLIST_POS, 0));
            final long trackId = extractValue(uri, PARAM_TRACK_ID, 0);
            long seekPos = extractValue(uri, PARAM_SEEK_POS, 0);

            if (trackId != 0 && getTrack().id != trackId && Content.match(uri).isCollectionItem()) {
                final int newPos = getPlaylistPositionFromUri(
                        mContext.getContentResolver(),
                        uri,
                        trackId);

                if (newPos == -1) seekPos = 0;
                setPosition(Math.max(newPos, 0));
            }
            return seekPos;
        } else {
            return 0; // seekpos
        }
    }

    public static int getPlaylistPositionFromUri(ContentResolver resolver, Uri collectionUri, long itemId) {
        Cursor cursor = resolver.query(collectionUri,
                new String[]{ DBHelper.CollectionItems.POSITION },
                DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[] {String.valueOf(itemId)},
                null);

        int position = -1;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            position = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        return position;
    }

    private static int extractValue(Uri uri, String parameter, final int defaultValue) {
        final String pos = uri.getQueryParameter(parameter);
        if (!TextUtils.isEmpty(pos)) {
            try {
                return Integer.parseInt(pos);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }
}

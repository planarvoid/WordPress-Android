package com.soundcloud.android.service.playback;


import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

/* package */
class PlaylistManager {
    public static final String PREF_PLAYLIST_URI = "sc_playlist_uri";
    public static final String PREF_PLAYLIST_LAST_POS = "sc_playlist_last_pos";
    public static final String PREF_PLAYLIST_LAST_ID = "sc_playlist_last_id";
    public static final String PREF_PLAYLIST_LAST_TIME = "sc_playlist_time";
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
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        final Track currentTrack = getTrackAt(mPlayPos);
        editor.putString(PREF_PLAYLIST_URI, mPlaylistUri == null ? "" : mPlaylistUri.toString());
        editor.putInt(PREF_PLAYLIST_LAST_POS, mPlayPos);
        editor.putLong(PREF_PLAYLIST_LAST_ID, currentTrack == null ? 0 : currentTrack.id);
        editor.putLong(PREF_PLAYLIST_LAST_TIME, seekPos);
        editor.commit();
    }

    public long reloadQueue() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String lastUriString = preferences.getString(PREF_PLAYLIST_URI, "");
        if (!TextUtils.isEmpty(lastUriString)){

            final int playlistPos = preferences.getInt(PREF_PLAYLIST_LAST_POS, 0);
            final long playlistTrackId = preferences.getLong(PREF_PLAYLIST_LAST_ID, 0);
            final Uri playlistUri = Uri.parse(lastUriString);

            long playlistLastTime = preferences.getLong(PREF_PLAYLIST_LAST_TIME, 0);
            setUri(playlistUri, playlistPos);

            if (playlistTrackId != 0 && getTrack().id != playlistTrackId){
                final int newPos = SoundCloudDB.getCollectionPositionFromItemId(
                        mContext.getContentResolver(),
                        playlistUri,
                        playlistTrackId);

                if (newPos == -1) playlistLastTime = 0;
                setPosition(Math.max(newPos, 0));
            }
            return playlistLastTime;
        }
        return 0; // seekpos
    }
}

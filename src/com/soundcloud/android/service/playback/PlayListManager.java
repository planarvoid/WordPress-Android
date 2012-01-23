package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

/* package */ class PlaylistManager {
    private static final String TAG = "PlaylistManager";

    private SoundCloudApplication mApp;
    private Track[] mPlaylist = new Track[0];
    private Cursor mTrackCursor;
    private Uri mPlaylistUri;

    private int mPlayPos;
    private Context mContext;
    private TrackCache mCache;
    private static final int DEFAULT_PLAYLIST = 0;
    private static final Uri DEFAULT_PLAYLIST_URI = Content.PLAYLIST_ITEMS.forId(DEFAULT_PLAYLIST);

    public PlaylistManager(Context context,
                           SoundCloudApplication app, TrackCache cache) {
        mContext = context;
        mApp = app;
        mCache = cache;
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
        List<Track> tracks = SoundCloudDB.getTracks(mContext.getContentResolver(), uri);
        if (mTrackCursor != null){
            if (!mTrackCursor.isClosed()) mTrackCursor.close();
        }
        mTrackCursor = mContext.getContentResolver().query(uri, null, null, null, null);
        mPlaylist = new Track[mTrackCursor.getCount()];
        if (position >=0 && position < mTrackCursor.getCount()){
            mPlayPos = position;
            mTrackCursor.moveToPosition(position);
        }
    }

    public void setPlaylist(final List<Parcelable> playlist, int playPos) {
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
        new Thread() {
            @Override
            public void run() {
                mApp.getContentResolver().delete(DEFAULT_PLAYLIST_URI,null,null);
                SoundCloudDB.bulkInsertParcelables(mApp.getContentResolver(), playlist, DEFAULT_PLAYLIST_URI, mApp.getCurrentUserId(), 0);
            }
        }.start();

    }

    public void clear() {
        mPlaylist = new Track[0];
    }

    public void saveQueue(long seekPos) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString("sc_playlist_uri", mPlaylistUri == null ? "" : mPlaylistUri.toString());
        editor.putInt("sc_playlist_pos",mPlayPos);
        editor.putLong("sc_playlist_time", seekPos);
        editor.commit();
    }

    public long reloadQueue() {
        // TODO, save the track id, check it, in case the uri has changed
        final String lastUriString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("sc_playlist_uri","");
        if (!TextUtils.isEmpty(lastUriString)){
            setUri(Uri.parse(lastUriString),PreferenceManager.getDefaultSharedPreferences(mContext).getInt("sc_playlist_pos", 0));
            return PreferenceManager.getDefaultSharedPreferences(mContext).getLong("sc_playlist_time", 0);
        }
        return 0; // seekpos
    }
}

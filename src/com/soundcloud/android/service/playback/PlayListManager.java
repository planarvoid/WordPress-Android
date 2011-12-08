package com.soundcloud.android.service.playback;


import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Track;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/* package */ class PlaylistManager  {
    private static final String TAG = "PlaylistManager";

    private final List<Long> mPlaylist = new ArrayList<Long>();
    private int mPlayPos;
    private Context mContext;
    private TrackCache mCache;

    public PlaylistManager(Context context,
                           TrackCache cache) {
        mContext = context;
        mCache = cache;
    }

    public int length() {
        return mPlaylist.size();
    }

    public boolean isEmpty() {
        return mPlaylist.isEmpty();
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mPlaylist.size()) {
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
        if (pos >= 0 && pos < mPlaylist.size()) {
            Track cached = mCache.get(mPlaylist.get(pos));
            if (cached != null) {
                return cached;
            } else {
                // cache miss
                Track t = SoundCloudDB.getTrackById(mContext.getContentResolver(), mPlaylist.get(pos));
                if (t != null) mCache.put(t);
                return t;
            }
        } else {
            return null;
        }
    }

    public long getTrackIdAt(int pos) {
        if (pos >= 0 && pos < length()) {
            return mPlaylist.get(pos);
        } else {
            return -1;
        }
    }

    public boolean prev() {
        if (mPlayPos == 0)
            return false;

        int newPos = mPlayPos - 1;
        Track newTrack = getTrackAt(newPos);
        while (newPos > 0 && (newTrack == null || !newTrack.isStreamable())) {
            newPos--;
            newTrack = getTrackAt(newPos);
        }

        if (newTrack != null && newTrack.isStreamable()) {
            mPlayPos = newPos;
            return true;
        } else {
            return false;
        }
    }

    public boolean next() {
        if (mPlayPos > length()) return false;

        int newPos = mPlayPos + 1;
        Track newTrack = getTrackAt(newPos);
        while (newPos < length() - 1 && (newTrack == null || !newTrack.isStreamable())) {
            newPos++;
            newTrack = getTrackAt(newPos);
        }

        if (newTrack != null && newTrack.isStreamable()) {
            mPlayPos = newPos;
            return true;
        } else {
            return false;
        }
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
        mPlaylist.clear();
        mPlaylist.add(toBePlayed.id);
        mPlayPos = 0;
    }

    public void setTracks(long[] toBePlayed, Track current) {
        mPlaylist.clear();
        for (long id : toBePlayed) mPlaylist.add(id);
        mPlayPos = mPlaylist.indexOf(current.id);
    }

    public void setTracks(List<Track> toBePlayed, Track current) {
        mPlaylist.clear();
        for (Track t : toBePlayed) {
            mCache.put(t);
            mPlaylist.add(t.id);
        }
        mPlayPos = mPlaylist.indexOf(current.id);
    }

    public void setTracks(Uri uri, Track current) {
        List<Track> tracks = SoundCloudDB.getTracks(mContext.getContentResolver(), uri);
        setTracks(tracks, current);
    }

    public void clear() {
        mPlaylist.clear();
    }

    public void saveQueue(boolean full, long seekPos) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    public long reloadQueue() {
        return 0; // seekpos
    }
}

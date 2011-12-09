package com.soundcloud.android.service.playback;


import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Track;

import android.content.Context;
import com.soundcloud.android.task.LoadTrackInfoTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.util.ArrayList;
import java.util.List;

/* package */ class PlaylistManager implements LoadTrackInfoTask.LoadTrackInfoListener {
    private static final String TAG = "PlaylistManager";

    private SoundCloudApplication mApp;
    private final List<Long> mPlaylist = new ArrayList<Long>();
    private int mPlayPos;
    private Context mContext;
    private TrackCache mCache;

    public PlaylistManager(Context context,
                           SoundCloudApplication app, TrackCache cache) {
        mContext = context;
        mApp = app;
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
                //Track t = SoundCloudDB.getTrackById(mContext.getContentResolver(), mPlaylist.get(pos));
                Track t = null;
                if (t == null) {
                    t = new Track();
                    t.id = mPlaylist.get(pos);
                    t.load_info_task = new LoadTrackInfoTask(mApp, mPlaylist.get(pos), true, true);
                    t.load_info_task.addListener(this);
                    t.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mPlaylist.get(pos)));
                }
                mCache.put(t);
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

    @Override
    public void onTrackInfoLoaded(Track track, String action) {
        mCache.put(track);
    }

    @Override
    public void onTrackInfoError(long trackId) {}
}

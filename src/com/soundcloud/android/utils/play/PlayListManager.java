
package com.soundcloud.android.utils.play;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.CommitTracksTask;

import android.content.ContentResolver;
import android.content.SharedPreferences.Editor;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

public class PlayListManager {

    private static final String TAG = "PlayListManager";

    private CloudPlaybackService mPlaybackService;


    private long[] mPlayList = new long[0];

    private int mPlayPos = 0;

    private int mPlayListLen = 0;

    // used when tracks get committed to db
    private Track[] mPlayListCache;

    public PlayListManager(CloudPlaybackService service) {
        mPlaybackService = service;
    }

    public int getCurrentLength() {
        return mPlayListLen;
    }

    public int getCurrentPosition() {
        return mPlayPos;
    }

    public Boolean setCurrentPosition(int playPos) {
        if (playPos < mPlayListLen) {
            mPlayPos = playPos;
            return true;
        }
        return false;
    }

    public Track getCurrentTrack() {
        return getTrackAt(mPlayPos);
    }

    public Track getTrackAt(int pos) {
        if (mPlayList != null && mPlayListLen > pos) {
            if (mPlayListCache != null) {
                return mPlayListCache[pos];
            } else {
                return SoundCloudDB.getInstance().getTrackById(
                        mPlaybackService.getContentResolver(),
                        mPlayList[pos],
                        ((SoundCloudApplication) mPlaybackService.getApplication())
                                .getCurrentUserId());
            }
        } else
            return null;

    }

    public Boolean prev() {
        if (mPlayPos == 0)
            return false;

        mPlayPos--;
        return true;

    }

    public Boolean next() {
        if (mPlayPos >= mPlayListLen - 1)
            return false;

        mPlayPos++;
        return true;

    }

    public void loadCachedPlaylist(List<Parcelable> playlistCache, int playPos) {
        // cache a new tracklist
        mPlayListCache = new Track[playlistCache.size()];

        mPlayList = new long[playlistCache.size()];

        int i = 0;
        for (Parcelable p : playlistCache){
            if (p instanceof Track) {
                mPlayListCache[i] = (Track) p;
            } else if (p instanceof Event) {
                mPlayListCache[i] = ((Event) p).getTrack();
            } else {
                // not playable, must be a recording.
                // ignore it and decrease play index to account for it
                playPos--;
                continue;
            }
            mPlayList[i] = mPlayListCache[i].id;
            i++;
        }

        mPlayPos = playPos;
        mPlayListLen = i;

        new CommitPlaylistTask(mPlaybackService.getContentResolver(),
                ((SoundCloudApplication) mPlaybackService.getApplication()).getCurrentUserId(),
                mPlayList).execute(mPlayListCache);
    }

    public class CommitPlaylistTask extends CommitTracksTask {
        private long[] currentPlaylist;

        public CommitPlaylistTask(ContentResolver contentResolver, Long long1, long[] currentPlaylist) {
            super(contentResolver, long1);
            this.currentPlaylist = currentPlaylist;
        }

        @Override
        protected void afterCommitInBg() {

        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            synchronized (PlayListManager.this) {
                mPlayListCache = null;
            }
        }
    }

    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            long[] newlist = new long[size * 2];
            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = mPlayList[i];
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }

    private final char hexdigits[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public void saveQueue(boolean full) {
        Editor ed = PreferenceManager.getDefaultSharedPreferences(mPlaybackService).edit();
        long start = System.currentTimeMillis();
        if (full) {
            StringBuilder q = new StringBuilder();

            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            // (saving the full state takes about 40 ms under no-load conditions
            // on the phone)
            int len = mPlayList.length;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        int digit = (int) (n & 0xf);
                        n >>= 4;
                        q.append(hexdigits[digit]);
                    }
                    q.append(";");
                }
            }
            Log.i("@@@@ service", "created queue string in " + (System.currentTimeMillis() - start)
                    + " ms");
            ed.putString("queue", q.toString());

        }
        ed.putInt("curpos", mPlayPos);
        ed.commit();

        Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis() - start) + " ms");
    }

    public void reloadQueue() {
        String q = PreferenceManager.getDefaultSharedPreferences(mPlaybackService).getString(
                "queue", "");

        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            Log.i("@@@@ service", "loaded queue: " + q + " " + qlen);
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    } else {
                        // bogus playlist data
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;

            int pos = PreferenceManager.getDefaultSharedPreferences(mPlaybackService).getInt(
                    "curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                // The saved playlist is bogus, discard it
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;
        }
    }
}

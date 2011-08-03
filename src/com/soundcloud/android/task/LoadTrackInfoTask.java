
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;

import java.lang.ref.WeakReference;

public class LoadTrackInfoTask extends LoadTask<Track> {
    private SoundCloudApplication mApp;
    private boolean mCacheResult;
    private boolean mWriteToDB;
    private long mTrackId;
    private WeakReference<LoadTrackInfoListener> mListenerWeakReference;

    public LoadTrackInfoTask(SoundCloudApplication app, long trackId, boolean cacheResult, boolean writeToDb) {
        super(app, Track.class);
        mApp = app;
        mTrackId = trackId;
        mCacheResult = cacheResult;
        mWriteToDB = writeToDb;
    }

    public void setListener(LoadTrackInfoListener listener){
        mListenerWeakReference = new WeakReference<LoadTrackInfoListener>(listener);
    }

    @Override
    protected void onPostExecute(Track result) {
        super.onPostExecute(result);

        LoadTrackInfoListener listener = mListenerWeakReference != null ? mListenerWeakReference.get() : null;
        if (result != null) {

            if (mApp.getTrackFromCache(result.id) != null) {
                result.setAppFields(mApp.getTrackFromCache(result.id));
            }

            if (mWriteToDB){
                SoundCloudDB.writeTrack(mApp.getContentResolver(), result,
                    WriteState.all, mApp.getCurrentUserId());
            }
            result.info_loaded = true;
            if (mCacheResult){
                mApp.cacheTrack(result);
            }

            if (listener != null){
                listener.onInfoLoaded(result);
            }

        } else if (listener != null){
            listener.onError(mTrackId);
        }
    }

    // Define our custom Listener interface
    public interface LoadTrackInfoListener {
        void onInfoLoaded(Track track);
        void onError(long trackId);
    }
}

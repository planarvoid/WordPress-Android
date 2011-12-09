
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.model.Track;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LoadTrackInfoTask extends LoadTask<Track> {
    private SoundCloudApplication mApp;
    private long mTrackId;
    private boolean mCacheResult;
    private boolean mWriteToDB;
    private ArrayList<WeakReference<LoadTrackInfoListener>> mListenerWeakReferences;

    public String action;

    public LoadTrackInfoTask(SoundCloudApplication app, long trackId, boolean cacheResult, boolean writeToDb) {
        super(app, Track.class);
        mApp = app;
        mTrackId = trackId;
        mCacheResult = cacheResult;
        mWriteToDB = writeToDb;
    }

    public void addListener(LoadTrackInfoListener listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new ArrayList<WeakReference<LoadTrackInfoListener>>();
        }
        mListenerWeakReferences.add(new WeakReference<LoadTrackInfoListener>(listener));
    }

    @Override
    protected void onPostExecute(Track result) {
        super.onPostExecute(result);

        if (result != null) {
            if (SoundCloudApplication.TRACK_CACHE.containsKey(result.id)) {
                result.setAppFields(SoundCloudApplication.TRACK_CACHE.get(result.id));
            }

            if (mWriteToDB){
                SoundCloudDB.writeTrack(mApp.getContentResolver(), result,
                    WriteState.all, mApp.getCurrentUserId());
            }
            result.info_loaded = true;
            if (mCacheResult){
                SoundCloudApplication.TRACK_CACHE.put(result);
            }

            if (mListenerWeakReferences != null){
                for (WeakReference<LoadTrackInfoListener> listenerRef : mListenerWeakReferences){
                    LoadTrackInfoListener listener = listenerRef.get();
                    if (listener != null){
                        listener.onTrackInfoLoaded(result, action);
                    }
                }
            }
        } else {
            if (mListenerWeakReferences != null){
                for (WeakReference<LoadTrackInfoListener> listenerRef : mListenerWeakReferences){
                    LoadTrackInfoListener listener = listenerRef.get();
                    if (listener != null){
                        listener.onTrackInfoError(mTrackId);
                    }
                }
            }
        }
    }

    // Define our custom Listener interface
    public interface LoadTrackInfoListener {
        void onTrackInfoLoaded(Track track, String action);
        void onTrackInfoError(long trackId);
    }
}

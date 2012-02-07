
package com.soundcloud.android.task;

import android.content.ContentResolver;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.model.Track;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LoadTrackInfoTask extends LoadTask<Track> {
    private SoundCloudApplication mApp;
    private long mTrackId;
    private ArrayList<WeakReference<LoadTrackInfoListener>> mListenerWeakReferences;

    public String action;

    public LoadTrackInfoTask(SoundCloudApplication app, long trackId) {
        super(app, Track.class);
        mApp = app;
        mTrackId = trackId;
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
            result.info_loaded = true;
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

    @Override
    protected void updateLocally(ContentResolver resolver, Track track) {
        track.info_loaded = true;
        track.last_updated = System.currentTimeMillis();
        SoundCloudApplication.TRACK_CACHE.putWithLocalFields(track);
        SoundCloudDB.upsertTrack(resolver, track);
    }

    // Define our custom Listener interface
    public interface LoadTrackInfoListener {
        void onTrackInfoLoaded(Track track, String action);
        void onTrackInfoError(long trackId);
    }
}

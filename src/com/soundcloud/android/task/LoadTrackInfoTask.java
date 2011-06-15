
package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Track;

public class LoadTrackInfoTask extends LoadTask<Track> {
    private SoundCloudApplication mApp;

    private long mTrackId;

    public LoadTrackInfoTask(SoundCloudApplication app, long trackId) {
        super(app, Track.class);
        mApp = app;
        mTrackId = trackId;
    }

    @Override
    protected void onPostExecute(Track result) {
        super.onPostExecute(result);

        if (result != null) {
            if (mApp.getTrackFromCache(result.id) != null) {
                result.setAppFields(mApp.getTrackFromCache(result.id));
            }

            SoundCloudDB.writeTrack(mApp.getContentResolver(), result,
                    WriteState.all, mApp.getCurrentUserId());

            result.info_loaded = true;
            mApp.cacheTrack(result);
        }

        if (mActivityReference != null && mActivityReference.get() != null)
            ((ScPlayer) mActivityReference.get()).onTrackInfoResult(mTrackId, result);
    }
}

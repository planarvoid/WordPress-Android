package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import java.util.List;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class RefreshTask extends LoadCollectionTask {
    public RefreshTask(SoundCloudApplication app) {
        super(app, null);
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {

        // TODO Go over exception handling

        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            adapter.onPreTaskExecute();
            loadModel = adapter.getLoadModel(true);
        }
    }

    /**
     * Add all new items that have been retrieved, now that we are back on a
     * UI thread
     */
    @Override
    protected void onPostExecute(Boolean keepGoing) {

    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {
        Log.i("asdf","UPDATED PROGRESSS " + keepGoing);
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
        }
    }
}

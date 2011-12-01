package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;

import android.os.Parcelable;

import java.util.List;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class AppendEventsTask extends LoadEventsTask {

    public AppendEventsTask(SoundCloudApplication app) {
        super(app, null);
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
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostTaskExecute(mNewItems, mNextHref, mResponseCode, keepGoing);
        }
    }
}

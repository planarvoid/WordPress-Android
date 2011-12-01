package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.SyncAdapterService;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.util.List;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class RefreshEventsTask extends LoadEventsTask {
    public RefreshEventsTask(SoundCloudApplication app) {
        super(app, null);
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            loadModel = adapter.getLoadModel(true);
        }

        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL,true);
        ContentResolver.requestSync(mApp.getAccount(),ScContentProvider.AUTHORITY,b);
    }

    /**
     * Add all new items that have been retrieved, now that we are back on a
     * UI thread
     */
    @Override
    protected void onPostExecute(Boolean keepGoing) {
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostRefresh(mNewItems, mNextHref, mResponseCode, keepGoing);
        }

    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (mNewItems != null && mNewItems.size() > 0) {
            for (Parcelable newitem : mNewItems) {
                adapter.getWrappedAdapter().addItem(newitem);
            }
            adapter.notifyDataSetChanged();
        }



    }
}

package com.soundcloud.android.task;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SyncedCollectionAdapter;

import java.lang.ref.WeakReference;
import java.util.List;

public class SyncedCollectionTask extends LoadCollectionTask {

    public SyncedCollectionTask(SoundCloudApplication app, LazyEndlessAdapter adapter) {
        super(app, adapter);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        SyncedCollectionAdapter adapter = (SyncedCollectionAdapter) mAdapterReference.get();
        if (adapter != null) {
            if (mParams.refresh){
                adapter.onPostRefresh(mNewItems);
            } else {
                adapter.onPostTaskExecute(mNewItems);
            }
        }
    }

    @Override
    protected Boolean doInBackground(Boolean... params) {
        if (mParams.contentUri != null) {
            mNewItems = (List<Parcelable>) loadLocalContent();
            return true;
        } else {
            // no local content, fail
            keepGoing = false;
            return false;
        }
    }

}

package com.soundcloud.android.task;

import android.os.Parcelable;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.SyncedCollectionAdapter;

import java.lang.ref.WeakReference;
import java.util.List;

public class SyncedCollectionTask extends LoadCollectionTask {

    public SyncedCollectionTask(SoundCloudApplication app, CollectionParams params) {
        super(app, params);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        SyncedCollectionAdapter adapter = (SyncedCollectionAdapter) mAdapterReference.get();
        if (adapter != null) {
            if (mParams.refresh){
                adapter.onPostRefresh(mNewItems, keepGoing);
            } else {
                adapter.onPostTaskExecute(mNewItems, keepGoing);
            }
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        if (mParams.contentUri != null) {
            mNewItems = (List<Parcelable>) loadLocalContent();
            keepGoing = mNewItems.size() == Consts.COLLECTION_PAGE_SIZE;
            return true;
        } else {
            // no local content, fail
            keepGoing = false;
            return false;
        }
    }

}

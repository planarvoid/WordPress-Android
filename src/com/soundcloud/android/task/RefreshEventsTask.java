package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.model.Activities;
import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;

public class RefreshEventsTask extends LoadCollectionTask {
    public File cacheFile;
    public RefreshEventsTask(SoundCloudApplication app) {
        super(app, null,null,0,true);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostRefresh(mNewItems, mNextHref, success);
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            if (cacheFile.exists()) {
                Activities a = Activities.fromJSON(cacheFile);
                mNewItems.addAll(a.collection);
                mNextHref = a.next_href;
                mResponseCode = HttpStatus.SC_OK;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

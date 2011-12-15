package com.soundcloud.android.task;

import android.app.DownloadManager;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;

public class RefreshEventsTask extends LoadCollectionTask {
    public File cacheFile;
    public String mNextHref;

    public RefreshEventsTask(SoundCloudApplication app, EventsAdapterWrapper lazyEndlessAdapter, Request request) {
        super(app, lazyEndlessAdapter);
        setAdapter(lazyEndlessAdapter);
        cacheFile = ActivitiesCache.getCacheFile(app,request);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostRefresh(mNewItems, mNextHref, success);
        }
    }

    @Override
    protected Boolean doInBackground(Boolean... params) {
        try {
            if (cacheFile.exists()) {
                Activities a = Activities.fromJSON(cacheFile);
                mNewItems.addAll(a.collection);
                mNextHref = a.next_href;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

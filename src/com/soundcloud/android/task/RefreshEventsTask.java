package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class RefreshEventsTask extends LoadEventsTask {
    public RefreshEventsTask(SoundCloudApplication app) {
        super(app, null);
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
        Log.i("asdf","REFRASHHHH");
        try {
            if (mCacheFile.exists()) {
                Activities a = Activities.fromJSON(mCacheFile);
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

package com.soundcloud.android.task;

import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.CloudUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LoadActivitiesTask extends RemoteCollectionTask {

    public LoadActivitiesTask(SoundCloudApplication app, LazyEndlessAdapter lazyEndlessAdapter) {
        super(app, lazyEndlessAdapter);
    }

    @Override
    protected void respond(){
        EventsAdapterWrapper adapter = (EventsAdapterWrapper) mAdapterReference.get();
        if (adapter != null) {
            adapter.onNewEvents(mNewItems, mNextHref, mResponseCode, keepGoing, mParams.isRefresh);
        }
    }

    @Override
    protected Boolean doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading activities with params: " + mParams);
        if (mParams.request != null) {
            return doRemoteLoad();
        } else if (mParams.contentUri != null) {
            mResponseCode = 0;
            mNewItems = new ArrayList<Parcelable>();
            Activities activities = Activities.get(CloudUtils.getPagedUri(mParams.contentUri,mParams.pageIndex), mApp.getContentResolver());
                for (Activity a : activities) {
                    a.resolve(mApp);
                    mNewItems.add(a);
                }

            publishProgress(mNewItems);
            return true;
        } else {
            throw new IllegalArgumentException("Incorrect paramaters");
        }
    }

    protected boolean doRemoteLoad() {
        try {
            HttpResponse resp = mApp.get(mParams.request);
            mResponseCode = resp.getStatusLine().getStatusCode();
            if (mResponseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            Activities activities = mApp.getMapper().readValue(resp.getEntity().getContent(), Activities.class);
            mNextHref = activities == null || TextUtils.isEmpty(activities.next_href) ? null : activities.next_href;
            keepGoing = !TextUtils.isEmpty(mNextHref);

            for (Parcelable p : activities) {
                ((ScModel) p).resolve(mApp);
                mNewItems.add(p);
            }

            // publish what we have, since we already have the items, we don't have to wait on a db commit
            publishProgress(mNewItems);

            activities.insert(Content.match(mParams.contentUri),mApp.getContentResolver());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }
        return false;
    }
}

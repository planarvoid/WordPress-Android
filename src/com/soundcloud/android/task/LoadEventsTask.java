package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.ModelBase;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.ActivitiesCache;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class LoadEventsTask extends LoadCollectionTask {
    protected SoundCloudApplication mApp;
    protected File mCacheFile;

    public LoadEventsTask(SoundCloudApplication app, Class<?> loadModel) {
        super(app,loadModel);
    }

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     */
    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        if (lazyEndlessAdapter != null) {
            loadModel = lazyEndlessAdapter.getLoadModel(false);
        }
    }

    @Override
    protected void onProgressUpdate(List<? super Parcelable>... values) {

    }

    @Override
    protected Boolean doInBackground(String... params) {

        if (refresh){
            try {
                if (mCacheFile.exists()) {
                    Activities a = Activities.fromJSON(mCacheFile);
                    mNewItems.addAll(a.collection);
                    mNextHref = a.next_href;
                    mResponseCode = HttpStatus.SC_OK;
                    publishProgress(mNewItems);

                    Activities updates = ActivitiesCache.getEvents(mApp, a.get(0), Request.to(a.future_href));
                    updates = updates.merge(a);
                    mNewItems.addAll(updates.collection);
                    return true;
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        return false;
    }
}

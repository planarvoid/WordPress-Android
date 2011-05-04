package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Activities;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.type.TypeFactory;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class AppendTask extends AsyncTask<Request, Parcelable, Boolean> {
    private SoundCloudApplication mApp;
    private WeakReference<LazyEndlessAdapter> mAdapterReference;
    /* package */ ArrayList<Parcelable> newItems = new ArrayList<Parcelable>();

    private Exception mException;

    public Class<?> loadModel;

    public int pageSize;

    public AppendTask(SoundCloudApplication app){
        mApp = app;
    }

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     */
    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            adapter.onPreTaskExecute();
            loadModel = adapter.getLoadModel();
        }
    }

    /**
     * Add all new items that have been retrieved, now that we are back on a
     * UI thread
     */
    @Override
    protected void onPostExecute(Boolean keepGoing) {
        LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null) {

            if (!Event.class.equals(loadModel)) {
                if (mException == null) {
                    adapter.incrementPage();
                } else {
                    adapter.setException(mException);
                }

                if (newItems != null && newItems.size() > 0) {
                    for (Parcelable newitem : newItems) {
                        adapter.getData().add(newitem);
                    }
                }

            }
            adapter.onPostTaskExecute(keepGoing);
        }
    }

    @Override
    protected Boolean doInBackground(Request... request) {
        final Request req = request[0];
        if (req == null) return false;

        try {
            HttpResponse resp = mApp.get(req);

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }
            InputStream is = resp.getEntity().getContent();

            if (Track.class.equals(loadModel) || User.class.equals(loadModel)) {
                newItems = mApp.getMapper().readValue(is, TypeFactory.collectionType(ArrayList.class, loadModel));
            } else if (Event.class.equals(loadModel)) {
                Activities activities = mApp.getMapper().readValue(is, Activities.class);
                activities.setCursorToLastEvent();
                SoundCloudDB.getInstance().insertActivities(mApp.getContentResolver(), activities, mApp.getCurrentUserId());
                return activities.size() >= pageSize;
            }

            // resolve data
            if (newItems != null) {
                for (Parcelable p : newItems) CloudUtils.resolveParcelable(mApp, p, mApp.getCurrentUserId());
                return newItems.size() >= pageSize;

            } else {
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            mException = e;
            return false;
        }
    }
}

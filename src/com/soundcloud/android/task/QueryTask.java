package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class QueryTask extends AsyncTask<String, Parcelable, Boolean> {
    private SoundCloudApplication mApp;
    protected WeakReference<LazyBaseAdapter> mAdapterReference;
    /* package */ ArrayList<Parcelable> newItems = new ArrayList<Parcelable>();

    public Class<?> loadModel;

    private Uri mQueryUri;
    private String[] mQueryProjection;
    private String mQuerySelection;
    private String[] mQuerySelectionArgs;
    private String mQuerySortOrder;

    protected AtomicBoolean keepMoving;

    public QueryTask(SoundCloudApplication app){
        mApp = app;
        keepMoving = new AtomicBoolean();
    }

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     */
    public void setAdapter(LazyBaseAdapter baseAdapter) {
        mAdapterReference = new WeakReference<LazyBaseAdapter>(baseAdapter);
    }

    public void setQuery(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        mQueryUri = uri;
        mQueryProjection = projection;
        mQuerySelection = selection;
        mQuerySelectionArgs = selectionArgs;
        mQuerySortOrder = sortOrder;
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {
        keepMoving.set(true);
        LazyBaseAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            loadModel = adapter.getLoadModel();
        }
    }

    /**
     * Add all new items that have been retrieved, now that we are back on a
     * UI thread
     */
    @Override
    protected void onPostExecute(Boolean keepGoing) {
        LazyBaseAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            adapter.onPostQueryExecute();
        }
    }

    @Override
    protected void onProgressUpdate(Parcelable...updates) {
        super.onProgressUpdate(updates);

        LazyBaseAdapter adapter = mAdapterReference.get();
        if (adapter != null) {
            if (updates != null && updates.length > 0) {
                for (Parcelable newitem : updates) {
                    adapter.getData().add(newitem);
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(String... param) {

        Cursor cursor = mApp.getContentResolver().query(
                mQueryUri,
                mQueryProjection,
                mQuerySelection, mQuerySelectionArgs, mQuerySortOrder);

        if (cursor != null && !cursor.isClosed()) {
            if (Event.class.equals(loadModel)) {
            Event e = null;
            while (cursor.moveToNext() && keepMoving.get()) {
                    e = new Event(cursor, true);
                    e.track = new Track(cursor, true);
                    e.track.user = new User(cursor,true);
                    publishProgress(e);
                }
            }
            cursor.close();
        }
        return true;
    }

}

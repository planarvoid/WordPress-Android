package com.soundcloud.android.task;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

public class CollectionLoader extends AsyncTaskLoader<List<Parcelable>> {

    protected SoundCloudApplication mApp;
    public boolean keepGoing;
    List<Parcelable> mItems;

    public Uri contentUri;
    public int pageIndex;
    public boolean refresh;

    public CollectionLoader(Context context, Uri uri) {
        this(context, uri, 0, false);
    }

    public CollectionLoader(Context context, Uri contentUri, int pageIndex, boolean refresh) {
        super(context);
        this.contentUri = contentUri;
        this.pageIndex = pageIndex;
        this.refresh = refresh;
    }

    @Override
    public List<Parcelable> loadInBackground() {

        final Class<? extends Parcelable> loadModel = Content.match(contentUri).resourceType;
        List<Parcelable> items = new ArrayList<Parcelable>();
        if (Activity.class.equals(loadModel)){
            for (Activity a : Activities.get(getContext().getContentResolver(), contentUri).collection){
                items.add(a);
            }
        } else {
            Cursor itemsCursor = getContext().getContentResolver().query(contentUri, null, null, null, null);
            if (itemsCursor != null && itemsCursor.moveToFirst()) {
                do {
                    if (Track.class.equals(loadModel)) {
                        items.add(new Track(itemsCursor));
                    } else if (User.class.equals(loadModel)) {
                        items.add(new User(itemsCursor));
                    }
                } while (itemsCursor.moveToNext());
            }
            if (itemsCursor != null) itemsCursor.close();
        }
        return items;
    }

    @Override public void deliverResult(List<Parcelable> items) {
        mItems = items;
        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(items);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override protected void onStartLoading() {
        if (mItems != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mItems);
        }

        if (takeContentChanged() || mItems == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
        mItems = null;
    }

}
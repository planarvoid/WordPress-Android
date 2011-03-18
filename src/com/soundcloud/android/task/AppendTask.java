package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.EventsAdapterWrapper;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.EventsWrapper;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
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
public class AppendTask extends AsyncTask<HttpUriRequest, Parcelable, Boolean> {
    private WeakReference<LazyEndlessAdapter> mAdapterReference;
    private WeakReference<ScActivity> mActivityReference;
    /* package */ ArrayList<Parcelable> newItems = new ArrayList<Parcelable>();

    public CloudUtils.Model loadModel;

    public int pageSize;

    /**
     * Set the activity and adapter that this task now belong to. This will
     * be set as new context is destroyed and created in response to
     * orientation changes
     *
     * @param lazyEndlessAdapter
     * @param activity
     */
    public void setContext(LazyEndlessAdapter lazyEndlessAdapter, ScActivity activity) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
        mActivityReference = new WeakReference<ScActivity>(activity);
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {
        if (mAdapterReference.get() != null)
            mAdapterReference.get().onPreTaskExecute();
    }

    /**
     * Add all new items that have been retrieved, now that we are back on a
     * UI thread
     */
    @Override
    protected void onPostExecute(Boolean keepGoing) {
        if (mAdapterReference.get() != null) {
            if (newItems != null && newItems.size() > 0) {
                for (Parcelable newitem : newItems) {
                    mAdapterReference.get().getData().add(newitem);
                }
            }
            mAdapterReference.get().onPostTaskExecute(keepGoing);
        }
        if (mActivityReference.get() != null) {
            mActivityReference.get().handleError();
            mActivityReference.get().handleException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Boolean doInBackground(HttpUriRequest... params) { /* XXX HttpUriRequest is not testable */

        // make sure we have a valid url
        HttpUriRequest req = params[0];
        if (req == null)
            return false;

        final LazyEndlessAdapter adapter = mAdapterReference.get();
        final ScActivity activity = mActivityReference.get();

        if (adapter == null || activity == null) return false;
        boolean keepAppending = true;

        try {
            Log.d(TAG, "Executing request " +req.getRequestLine().getUri());
            HttpResponse resp = activity
                    .getSoundCloudApplication()
                    .execute(req);

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            InputStream is = resp.getEntity().getContent();
            ObjectMapper mapper = activity.getSoundCloudApplication().getMapper();
            if (newItems != null) newItems.clear();
            switch (adapter.getLoadModel()) {
                case track:
                    newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                            Track.class));
                    break;
                case user:
                    newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                            User.class));
                    break;

                case event:
                    EventsWrapper evtWrapper = mapper.readValue(is, EventsWrapper.class);
                    newItems = new ArrayList<Parcelable>(evtWrapper.getCollection().size());
                    for (Event evt : evtWrapper.getCollection())
                        newItems.add(evt);
                    if (evtWrapper.getNext_href() != null)
                        ((EventsAdapterWrapper) adapter)
                                .onNextEventsParam(evtWrapper.getNext_href());
                    break;
            }

            // resolve data
            for (Parcelable p : newItems) CloudUtils.resolveParcelable(activity, p);

            // we have less than the requested number of items, so we are
            // done grabbing items for this list

            if (newItems == null || newItems.size() < pageSize) keepAppending = false;

            // we were successful, so increment the adapter
             adapter.incrementPage();
            return keepAppending;
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            adapter.setException(e);
            return false;
        }
    }
}

package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.soundcloud.android.SoundCloudApplication.TAG;

/**
 * A background task that will be run when there is a need to append more
 * data. Mostly, this code delegates to the subclass, to append the data in
 * the background thread and rebind the pending view once that is done.
 */
public class RefreshTask extends AppendTask {
    public RefreshTask(SoundCloudApplication app) {
        super(app);
    }

    /**
     * Do any task preparation we need to on the UI thread
     */
    @Override
    protected void onPreExecute() {

        // TODO Go over exception handling

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
            if (mException != null){
                 adapter.handleResponseCode(mResponseCode);
            } else if (newItems.size() > 0){
                // false for notify of change, we can only notify after resetting listview
                adapter.reset(true, false);
                super.onPostExecute(keepGoing);
            }

            adapter.onPostRefresh(mException == null);
        }
    }
}

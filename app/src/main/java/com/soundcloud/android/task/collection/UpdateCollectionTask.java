package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.BaseAdapter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

public class UpdateCollectionTask extends AsyncTask<Map<Long, ? extends ScResource>, String, Boolean> {
    protected AndroidCloudAPI mApp;
    protected Class<?> mLoadModel;
    protected WeakReference<BaseAdapter> mAdapterReference;

    public UpdateCollectionTask(SoundCloudApplication app, Class<?> loadModel) {
        mApp = app;
        mLoadModel = loadModel;
        if (!(Track.class.equals(mLoadModel) || User.class.equals(mLoadModel))) {
            throw new IllegalArgumentException("Collection updating only allowed for tracks, users and Friends");
        }
    }

    public void setAdapter(BaseAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<BaseAdapter>(lazyEndlessAdapter);
    }



    @Override
    protected void onProgressUpdate(String... values) {
        final BaseAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
    }

    @Override
    protected Boolean doInBackground(Map<Long, ? extends ScResource>... params) {
        Map<Long,? extends ScResource> itemsToUpdate = params[0];
        Log.i(TAG,"Updating " + itemsToUpdate.size() + " items");
        try {
            HttpResponse resp = mApp.get(Request.to(Track.class.equals(mLoadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                    .add("linked_partitioning", "1").add("ids", TextUtils.join(",", itemsToUpdate.keySet())));

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            SoundCloudApplication.MODEL_MANAGER.writeCollectionFromStream(resp.getEntity().getContent(), ScModel.CacheUpdateMode.FULL);
            publishProgress();
            return true;

        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

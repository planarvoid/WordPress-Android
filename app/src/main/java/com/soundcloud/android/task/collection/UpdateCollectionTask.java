package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.text.TextUtils;
import android.util.Log;
import android.widget.BaseAdapter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

public class UpdateCollectionTask extends ParallelAsyncTask<String, String, Boolean> {
    private AndroidCloudAPI mApi;
    private String mEndpoint;
    private WeakReference<BaseAdapter> mAdapterReference;
    private Set<Long> mResourceIds;

    public UpdateCollectionTask(AndroidCloudAPI api, String endpoint, Set<Long> resourceIds) {
        if (TextUtils.isEmpty(endpoint)) throw new IllegalArgumentException("endpoint is empty");

        mApi = api;
        mEndpoint = endpoint;
        mResourceIds = resourceIds;
    }

    public void setAdapter(BaseAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<BaseAdapter>(lazyEndlessAdapter);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        final BaseAdapter adapter = mAdapterReference.get();
        if (adapter != null && success) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Log.i(TAG,"Updating " + mResourceIds.size() + " items");
        try {
            Request request = Request.to(mEndpoint)
                    .add("linked_partitioning", "1")
                    .add("ids", TextUtils.join(",", mResourceIds));
            HttpUtils.addQueryParams(request, params);
            HttpResponse resp = mApi.get(request);

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            SoundCloudApplication.MODEL_MANAGER.writeCollectionFromStream(resp.getEntity().getContent(), ScResource.CacheUpdateMode.FULL);
            return true;
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.http.PublicApiWrapper;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.Request;

import android.text.TextUtils;
import android.util.Log;
import android.widget.BaseAdapter;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UpdateCollectionTask extends ParallelAsyncTask<String, String, Boolean> {
    private PublicCloudAPI mApi;
    private String mEndpoint;
    private WeakReference<BaseAdapter> mAdapterReference;
    private Set<Long> mResourceIds;

    public UpdateCollectionTask(PublicCloudAPI api, String endpoint, Set<Long> resourceIds) {
        if (TextUtils.isEmpty(endpoint)) throw new IllegalArgumentException("endpoint is empty");

        mApi = api;
        mEndpoint = endpoint;
        mResourceIds = resourceIds;
    }

    public void setAdapter(BaseAdapter adapter) {
        mAdapterReference = new WeakReference<BaseAdapter>(adapter);
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
                .add(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .add("ids", TextUtils.join(",", mResourceIds));

            /* in memory objects will only receive these lookups if you go through the cache,
            of course this can change eventually */
            List<ScResource> resources = new ArrayList<ScResource>();
            for (ScResource r :  mApi.readList(HttpUtils.addQueryParams(request, params))){
                resources.add(SoundCloudApplication.MODEL_MANAGER.cache(r, ScResource.CacheUpdateMode.FULL));
            }

            new BaseDAO<ScResource>(SoundCloudApplication.instance.getContentResolver()) {
                @Override public Content getContent() {
                    return Content.COLLECTIONS;
                }
            }.createCollection(resources);

            return true;
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

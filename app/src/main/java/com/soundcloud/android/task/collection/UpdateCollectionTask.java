package com.soundcloud.android.task.collection;

import android.text.TextUtils;
import android.util.Log;
import android.widget.BaseAdapter;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.dao.BaseDAO;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import static com.soundcloud.android.SoundCloudApplication.TAG;

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
                .add(Wrapper.LINKED_PARTITIONING, "1")
                .add("ids", TextUtils.join(",", mResourceIds));

            List<ScResource> resources = mApi.readList(HttpUtils.addQueryParams(request, params));

            new BaseDAO<ScResource>(mApi.getContext().getContentResolver()) {
                @Override public Content getContent() {
                    return Content.COLLECTIONS;
                }
            }.create(resources);

            return true;
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.BaseDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.HttpUtils;
import com.soundcloud.android.api.legacy.Request;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UpdateCollectionTask extends ParallelAsyncTask<String, String, Boolean> {
    private final PublicApiWrapper api;
    private final String endpoint;
    private final Set<Long> resourceIds;
    private final Map<Urn, PublicApiResource> updatedResources;
    private WeakReference<ScBaseAdapter> adapterReference;

    public UpdateCollectionTask(PublicApiWrapper api, String endpoint, Set<Long> resourceIds) {
        if (TextUtils.isEmpty(endpoint)) {
            throw new IllegalArgumentException("endpoint is empty");
        }

        this.api = api;
        this.endpoint = endpoint;
        this.resourceIds = resourceIds;
        updatedResources = new HashMap<>(resourceIds.size());
    }

    public void setAdapter(ScBaseAdapter adapter) {
        adapterReference = new WeakReference<>(adapter);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        final ScBaseAdapter adapter = adapterReference.get();
        if (adapter != null && success) {
            adapter.updateItems(updatedResources);
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Log.i(TAG, "Updating " + resourceIds.size() + " items");
        try {
            Request request = Request.to(endpoint)
                    .add(PublicApiWrapper.LINKED_PARTITIONING, "1")
                    .add("ids", TextUtils.join(",", resourceIds));


            /* in memory objects will only receive these lookups if you go through the cache,
            of course this can change eventually */
            final Request request1 = HttpUtils.addQueryParams(request, params);
            for (PublicApiResource r : api.readList(request1)) {
                r.setUpdated();
                updatedResources.put(r.getUrn(), SoundCloudApplication.sModelManager.cache(r, PublicApiResource.CacheUpdateMode.FULL));
            }

            new BaseDAO<PublicApiResource>(SoundCloudApplication.instance.getContentResolver()) {
                @Override
                public Content getContent() {
                    return Content.COLLECTIONS;
                }
            }.createCollection(updatedResources.values());

            return true;
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

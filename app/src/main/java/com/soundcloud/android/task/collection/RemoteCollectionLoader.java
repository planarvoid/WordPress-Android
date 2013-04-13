package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.UnknownResource;
import org.apache.http.HttpStatus;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionLoader<T extends ScResource> extends CollectionLoader<T> {

    @Override
    public ReturnData<T> load(AndroidCloudAPI app, CollectionParams<T> params) {
        try {
            CollectionHolder<T> holder = app.readCollection(params.request);

            // process new items and publish them
            final String nextHref = TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
            // suppress unknown resources
            List<ScResource> toRemove = new ArrayList<ScResource>();
            for (ScResource resource : holder){
                if (resource instanceof UnknownResource) toRemove.add(resource);
            }
            holder.collection.removeAll(toRemove);

            return new ReturnData<T>(holder.collection,
                    params,
                    nextHref,
                    HttpStatus.SC_OK,
                    !TextUtils.isEmpty(nextHref),
                    true);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        return new ReturnData.Error<T>();
    }
}

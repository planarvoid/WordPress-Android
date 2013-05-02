package com.soundcloud.android.task.collection;

import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScResource;
import org.apache.http.HttpStatus;

import java.io.IOException;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class RemoteCollectionLoader<T extends ScResource> implements CollectionLoader<T> {

    @Override
    public ReturnData<T> load(AndroidCloudAPI app, CollectionParams<T> params) {
        try {
            CollectionHolder<T> holder = app.readCollection(params.getRequest());

            // suppress unknown resources
            holder.removeUnknownResources();

            return new ReturnData<T>(holder.getCollection(),
                    params,
                    holder.getNextHref(),
                    HttpStatus.SC_OK,
                    holder.moreResourcesExist(),
                    true);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
        }

        return new ReturnData.Error<T>(params);
    }
}

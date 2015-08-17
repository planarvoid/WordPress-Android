package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import org.apache.http.HttpStatus;

import android.util.Log;

import java.io.IOException;

@Deprecated // only used for WhoToFollow these days
public class RemoteCollectionLoader<T extends PublicApiResource> implements CollectionLoader<T> {

    @Override
    public ReturnData<T> load(PublicApi app, CollectionParams<T> params) {
        try {
            CollectionHolder<T> holder = app.readCollection(params.getRequest());

            // suppress unknown resources
            holder.removeUnknownResources();

            return new ReturnData<>(holder.getCollection(),
                    params,
                    holder.getNextHref(),
                    HttpStatus.SC_OK,
                    holder.moreResourcesExist(),
                    true);

        } catch (UnexpectedResponseException e){
            Log.e(TAG, "error", e);
            return new ReturnData.Error<>(params, e.getStatusCode());
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return new ReturnData.Error<>(params);
        }
    }
}

package com.soundcloud.android.task.collection;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.task.ParallelAsyncTask;

import android.util.Log;

import java.lang.ref.WeakReference;

public class CollectionTask extends ParallelAsyncTask<CollectionParams, ReturnData, ReturnData> {
    private AndroidCloudAPI mApi;
    private WeakReference<Callback> mCallback;

    public interface Callback {
        void onPostTaskExecute(ReturnData data);
    }

    public CollectionTask(AndroidCloudAPI api, Callback callback) {
        mApi = api;
        mCallback = new WeakReference<Callback>(callback);
    }

    @Override
    protected void onPostExecute(ReturnData returnData) {
        Callback callback = mCallback == null ? null : mCallback.get();
        if (callback != null) {
            callback.onPostTaskExecute(returnData);
        }
    }

    @Override
    protected ReturnData doInBackground(CollectionParams... xparams) {
        CollectionParams params = xparams[0];
        Log.d(TAG, getClass().getSimpleName() + "Loading collection with params: " + params);

        final Class<? extends ScModel> resourceType = params.getContent().modelType;
        if (resourceType != null && Activity.class.isAssignableFrom(resourceType)) {
            return new ActivitiesLoader().load(mApi, params);
        } else if (params.getContent().isSyncable()) {
            return new MyCollectionLoader().load(mApi, params);

        } else if (params.request != null) {
            return new RemoteCollectionLoader().load(mApi, params);

        } else return new ReturnData(params);
    }
}

package com.soundcloud.android.task.collection;

import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.task.ParallelAsyncTask;

import java.lang.ref.WeakReference;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class CollectionTask extends ParallelAsyncTask<CollectionParams, ReturnData, ReturnData> {
    private AndroidCloudAPI mApi;
    private WeakReference<Callback> mCallback;
    private CollectionLoader mSyncableCollectionLoader;
    private CollectionLoader mRemoteCollectionLoader;
    private CollectionLoader mSoundcloudActivityLoder;

    public interface Callback {
        void onPostTaskExecute(ReturnData data);
    }

    public CollectionTask(AndroidCloudAPI api, Callback callback){
        this(api, callback, new MyCollectionLoader(), new RemoteCollectionLoader(), new ActivitiesLoader());
    }

    protected CollectionTask(AndroidCloudAPI api, Callback callback, CollectionLoader syncableCollectionLoader,
                          CollectionLoader remoteCollectionLoader, CollectionLoader soundcloudActivityLoder) {
        mApi = api;
        mCallback = new WeakReference<Callback>(callback);
        mSyncableCollectionLoader = syncableCollectionLoader;
        mRemoteCollectionLoader = remoteCollectionLoader;
        mSoundcloudActivityLoder = soundcloudActivityLoder;
    }

    @Override
    protected void onPostExecute(ReturnData returnData) {
        Callback callback = mCallback.get();
        if (callback != null) {
            callback.onPostTaskExecute(returnData);
        }
    }

    @Override
    protected ReturnData doInBackground(CollectionParams... xparams) {
        CollectionParams params = xparams[0];
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, getClass().getSimpleName() + "Loading collection with params: " + params);
        }


        if (resourceIsASoundcloudActivity(params)) {
            return mSoundcloudActivityLoder.load(mApi, params);
        } else if (contentIsSyncable(params)) {
            return mSyncableCollectionLoader.load(mApi, params);
        } else if (collectionIsLocatedRemotely(params)) {
            return mRemoteCollectionLoader.load(mApi, params);
        } else {
            return new ReturnData(params);
        }
    }

    private boolean resourceIsASoundcloudActivity(CollectionParams params) {
        Class<? extends ScModel> resourceType = params.getContent().getModelType();
        return resourceType != null && Activity.class.isAssignableFrom(resourceType);
    }

    private boolean collectionIsLocatedRemotely(CollectionParams params) {
        return params.getRequest() != null;
    }

    private boolean contentIsSyncable(CollectionParams params) {
        return params.getContent().isSyncable();
    }
}

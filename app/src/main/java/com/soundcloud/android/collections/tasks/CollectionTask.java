package com.soundcloud.android.collections.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.Log;

import java.lang.ref.WeakReference;

@Deprecated
public class CollectionTask extends ParallelAsyncTask<CollectionParams, ReturnData, ReturnData> {
    private final PublicCloudAPI mApi;
    private final WeakReference<Callback> mCallback;
    private final CollectionLoaderFactory mCollectionLoaderFactory;

    public interface Callback {
        void onPostTaskExecute(ReturnData data);
    }

    public CollectionTask(PublicCloudAPI api, Callback callback){
        this(api, callback, new CollectionLoaderFactory());
    }

    protected CollectionTask(PublicCloudAPI api, Callback callback, CollectionLoaderFactory collectionLoaderFactory) {
        mApi = api;
        mCallback = new WeakReference<Callback>(callback);
        mCollectionLoaderFactory = collectionLoaderFactory;
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
        Log.d(TAG, getClass().getSimpleName() + "Loading collection with params: " + params);
        CollectionLoader loader = mCollectionLoaderFactory.createCollectionLoader(params);
        return loader == null ? new ReturnData(params) : loader.load(mApi,params);
    }

}

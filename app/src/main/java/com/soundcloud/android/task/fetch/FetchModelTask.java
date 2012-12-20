package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public abstract class FetchModelTask<Model extends ScResource> extends ParallelAsyncTask<Request, Void, Model> {
    private AndroidCloudAPI mApi;
    private Set<WeakReference<FetchModelListener<Model>>> mListenerWeakReferences;
    private long mModelId;
    private Class<? extends Model> mModel;

    public String action;
    private boolean mError;

    public FetchModelTask(AndroidCloudAPI api, Class<? extends Model> model, long modelId) {
        mApi = api;
        mModel = model;
        mModelId = modelId;
    }

    public void addListener(FetchModelListener<Model> listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new HashSet<WeakReference<FetchModelListener<Model>>>();
        }

        mListenerWeakReferences.add(new WeakReference<FetchModelListener<Model>>(listener));
    }

    @Override
    protected void onPostExecute(Model result) {
        mError = result == null;
        if (mListenerWeakReferences != null) {
            for (WeakReference<FetchModelListener<Model>> listenerRef : mListenerWeakReferences) {
                final FetchModelListener<Model> listener = listenerRef.get();
                if (listener != null) {
                    if (!mError) {
                        listener.onSuccess(result, action);
                    } else {
                        listener.onError(mModelId);
                    }
                }
            }
        }
    }

    @Nullable
    public Model resolve(Request request) {
        try {
            HttpResponse resp = mApi.get(request);
            if (isCancelled()) return null;

            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK: {
                    return mApi.getMapper().readValue(resp.getEntity().getContent(), mModel);
                }

                case HttpStatus.SC_NOT_FOUND: // gone
                    return null;

                default:
                    Log.w(TAG, "unexpected response " + resp.getStatusLine());
                    return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    public boolean wasError(){
        return mError;
    }

    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need path to executeAppendTask");
        return resolve(request[0]);
    }

    public interface FetchModelListener<Model extends Parcelable> {
        void onSuccess(Model m, @Nullable String action);
        void onError(long modelId);
    }
}

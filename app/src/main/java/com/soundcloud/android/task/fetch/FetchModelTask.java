package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class FetchModelTask<Model extends ScResource> extends AsyncTask<Request, Void, Model> {
    private AndroidCloudAPI mApi;
    private ArrayList<WeakReference<FetchModelListener<Model>>> mListenerWeakReferences;
    private long mModelId;
    private Class<? extends Model> mModel;

    public String action;

    public FetchModelTask(AndroidCloudAPI api, Class<? extends Model> model, long modelId) {
        mApi = api;
        mModel = model;
        mModelId = modelId;
    }

    public void addListener(FetchModelListener<Model> listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new ArrayList<WeakReference<FetchModelListener<Model>>>();
        }
        mListenerWeakReferences.add(new WeakReference<FetchModelListener<Model>>(listener));
    }

    @Override
    protected void onPostExecute(Model result) {
        if (mListenerWeakReferences != null) {
            for (WeakReference<FetchModelListener<Model>> listenerRef : mListenerWeakReferences) {
                FetchModelListener<Model> listener = listenerRef.get();
                if (listener != null) {
                    if (result != null) {
                        listener.onSuccess(result, action);
                    } else {
                        listener.onError(mModelId);
                    }
                }
            }
        }
    }

    @Override
    public Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need path to executeAppendTask");

        try {
            HttpResponse resp = mApi.get(request[0]);
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

    public interface FetchModelListener<Model extends Parcelable> {
        void onSuccess(Model m, @Nullable String action);
        void onError(long modelId);
    }
}

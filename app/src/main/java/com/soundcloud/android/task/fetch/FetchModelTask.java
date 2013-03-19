package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;

import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class FetchModelTask<Model extends ScResource> extends ParallelAsyncTask<Request, Void, Model> {
    private AndroidCloudAPI mApi;
    private Set<WeakReference<Listener<Model>>> mListenerWeakReferences;

    private Exception  mException;
    private StatusLine mStatusLine;
    private final long mModelId;

    public FetchModelTask(AndroidCloudAPI api) {
        this(api, -1);
    }

    public FetchModelTask(AndroidCloudAPI api, long id) {
        mApi = api;
        mModelId = id;
    }

    public void addListener(Listener<Model> listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new HashSet<WeakReference<Listener<Model>>>();
        }

        mListenerWeakReferences.add(new WeakReference<Listener<Model>>(listener));
    }


    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need request");
        return resolve(request[0]);
    }

    @Override
    protected void onPostExecute(Model result) {
        if (mListenerWeakReferences != null) {
            for (WeakReference<Listener<Model>> listenerRef : mListenerWeakReferences) {
                final Listener<Model> listener = listenerRef.get();
                if (listener != null) {
                    if (result != null) {
                        listener.onSuccess(result);
                    } else {
                        listener.onError(mModelId);
                    }
                }
            }
        }
    }

    //TODO (Matthias:) This method returns null in 4 different, unrelated cases, which makes it hard to properly deal
    // with API errors on the UI. We need to address this.
    @Nullable
    public Model resolve(Request request) {
        try {
            HttpResponse resp = mApi.get(request);
            if (isCancelled()) return null;

            switch (resp.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK: {
                    return (Model) mApi.getMapper().readValue(resp.getEntity().getContent(), ScResource.class);
                }

                case HttpStatus.SC_NOT_FOUND: return null;

                default: {
                    mStatusLine = resp.getStatusLine();
                    Log.w(TAG, "unexpected response " + resp.getStatusLine());
                    return null;
                }
            }
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error", e);
            return null;
        }
    }

    public boolean wasError() {
        return mException != null || mStatusLine != null;
    }

    public interface Listener<Model extends Parcelable> {
        void onSuccess(Model m);
        void onError(Object context);
    }
}

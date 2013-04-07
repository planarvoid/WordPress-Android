package com.soundcloud.android.task.fetch;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.soundcloud.android.AndroidCloudAPI.NotFoundException;
import static com.soundcloud.android.SoundCloudApplication.TAG;

public abstract class FetchModelTask<Model extends ScResource> extends ParallelAsyncTask<Request, Void, Model> {
    protected AndroidCloudAPI mApi;
    private Set<WeakReference<Listener<Model>>> mListenerWeakReferences;

    private Exception  mException;
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
            if (isCancelled()) return null;
            Model model = mApi.read(request);
            model.setUpdated();
            persist(model);
            SoundCloudApplication.MODEL_MANAGER.cache(model, ScResource.CacheUpdateMode.FULL);
            return model;
        } catch (NotFoundException e) {
            return null;
        } catch (IOException e) {
            mException = e;
            Log.e(TAG, "error", e);
            return null;
        }
    }

    protected abstract void persist(Model model);

    public boolean wasError() {
        return mException != null;
    }

    public interface Listener<Model extends Parcelable> {
        void onSuccess(Model m);
        void onError(Object context);
    }
}

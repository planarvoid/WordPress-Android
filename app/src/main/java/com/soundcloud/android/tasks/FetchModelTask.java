package com.soundcloud.android.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.api.legacy.PublicCloudAPI.NotFoundException;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.Nullable;

import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public abstract class FetchModelTask<Model extends PublicApiResource> extends ParallelAsyncTask<Request, Void, Model> {

    protected PublicCloudAPI api;
    private Set<WeakReference<Listener<Model>>> listenerWeakReferences;

    private final long modelId;

    public FetchModelTask(PublicCloudAPI api) {
        this(api, -1);
    }

    public FetchModelTask(PublicCloudAPI api, long id) {
        this.api = api;
        modelId = id;
    }

    public void addListener(Listener<Model> listener) {
        if (listenerWeakReferences == null) {
            listenerWeakReferences = new HashSet<>();
        }

        listenerWeakReferences.add(new WeakReference<>(listener));
    }


    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) {
            throw new IllegalArgumentException("need request");
        }
        return resolve(request[0]);
    }

    @Override
    protected void onPostExecute(Model result) {
        if (listenerWeakReferences != null) {
            for (WeakReference<Listener<Model>> listenerRef : listenerWeakReferences) {
                final Listener<Model> listener = listenerRef.get();
                if (listener != null) {
                    if (result != null) {
                        listener.onSuccess(result);
                    } else {
                        listener.onError(modelId);
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
            if (isCancelled()) {
                return null;
            }
            Model model = api.read(request);
            model.setUpdated();
            persist(model);
            SoundCloudApplication.sModelManager.cache(model, PublicApiResource.CacheUpdateMode.FULL);
            return model;
        } catch (NotFoundException e) {
            return null;
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    protected abstract void persist(Model model);

    public interface Listener<Model extends Parcelable> {
        void onSuccess(Model m);

        void onError(Object context);
    }
}

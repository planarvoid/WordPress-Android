package com.soundcloud.android.tasks;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import org.jetbrains.annotations.Nullable;

import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

public abstract class FetchModelTask<Model extends PublicApiResource> extends ParallelAsyncTask<ApiRequest, Void, Model> {

    protected ApiClient api;
    private Set<WeakReference<Listener<Model>>> listenerWeakReferences;

    private final long modelId;

    public FetchModelTask(ApiClient api) {
        this(api, -1);
    }

    public FetchModelTask(ApiClient api, long id) {
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
    protected Model doInBackground(ApiRequest... request) {
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

    @Nullable
    public Model resolve(ApiRequest request) {
        try {
            if (isCancelled()) {
                return null;
            }
            Model model = api.fetchMappedResponse(request, getGenericClassType());
            model.setUpdated();
            persist(model);
            SoundCloudApplication.sModelManager.cache(model, PublicApiResource.CacheUpdateMode.FULL);
            return model;
        } catch (ApiMapperException | ApiRequestException | IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Class<Model> getGenericClassType() {
        return (Class<Model>)
                ((ParameterizedType) getClass()
                        .getGenericSuperclass())
                        .getActualTypeArguments()[0];
    }

    protected abstract void persist(Model model);

    public interface Listener<Model extends Parcelable> {
        void onSuccess(Model m);

        void onError(Object context);
    }
}

package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class FetchModelTask<Model extends ScModel> extends AsyncTask<Request, Void, Model> {
    private AndroidCloudAPI mApi;
    private ArrayList<WeakReference<FetchModelListener>> mListenerWeakReferences;
    private long mModelId;
    private Class<? extends Model> mModel;

    public String action;

    public FetchModelTask(AndroidCloudAPI api, Class<? extends Model> model, long modelId) {
        mApi = api;
        mModel = model;
        mModelId = modelId;
    }

    protected WeakReference<ScListActivity> mActivityReference;

    public void setActivity(ScListActivity activity) {
        if (activity != null) {
            mActivityReference = new WeakReference<ScListActivity>(activity);
        }
    }

    public void addListener(FetchModelListener listener){
        if (mListenerWeakReferences == null){
            mListenerWeakReferences = new ArrayList<WeakReference<FetchModelListener>>();
        }
        mListenerWeakReferences.add(new WeakReference<FetchModelListener>(listener));
    }


    @Override
    protected void onPostExecute(Model result) {
        if (result != null) {
            if (mListenerWeakReferences != null) {
                for (WeakReference<FetchModelListener> listenerRef : mListenerWeakReferences) {
                    FetchModelListener listener = listenerRef.get();
                    if (listener != null){
                        listener.onSuccess(result, action);
                    }
                }
            }
        } else {
            if (mListenerWeakReferences != null){
                for (WeakReference<FetchModelListener> listenerRef : mListenerWeakReferences) {
                    FetchModelListener listener = listenerRef.get();
                    if (listener != null){
                        listener.onError(mModelId);
                    }
                }
            }
        }
    }

    @Override
    protected Model doInBackground(Request... request) {
        if (request == null || request.length == 0) throw new IllegalArgumentException("need path to executeAppendTask");

        try {
            HttpResponse resp = mApi.get(request[0]);
            if (isCancelled()) return null;

            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                Model model = mApi.getMapper().readValue(resp.getEntity().getContent(), mModel);
                if (mApi instanceof Context && model != null){
                    updateLocally(((Context) mApi).getContentResolver(), model);
                }
                return model;
            } else {
                Log.w(TAG, "unexpected response " + resp.getStatusLine());
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return null;
        }
    }

    public interface FetchModelListener<Model extends Parcelable> {
        void onSuccess(Model m, String action);
        void onError(long modelId);
    }

    abstract protected void updateLocally(ContentResolver resolver, Model model);
}

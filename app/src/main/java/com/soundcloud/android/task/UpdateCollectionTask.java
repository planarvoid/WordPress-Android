package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserlistItem;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class UpdateCollectionTask extends AsyncTask<Map<Long, ScModel>, String, Boolean> {
    protected SoundCloudApplication mApp;
    protected Class<?> mLoadModel;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;

    public UpdateCollectionTask(SoundCloudApplication app,Class<?> loadModel) {
        mApp = app;
        mLoadModel = loadModel;
        if (!(Track.class.equals(mLoadModel) || User.class.equals(mLoadModel))) {
            throw new IllegalArgumentException("Collection updating only allowed for tracks, users and Friends");
        }
    }

    public void setAdapter(LazyEndlessAdapter lazyEndlessAdapter) {
        mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
    }



    @Override
    protected void onProgressUpdate(String... values) {
        final LazyEndlessAdapter adapter = mAdapterReference.get();
        if (adapter != null){
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
    }

    @Override
    protected Boolean doInBackground(Map<Long, ScModel>... params) {
        Map<Long,ScModel> itemsToUpdate = params[0];
        Log.i(TAG,"Updating " + itemsToUpdate.size() + " items");
        try {
            HttpResponse resp = mApp.get(Request.to(Track.class.equals(mLoadModel) ? Endpoints.TRACKS : Endpoints.USERS)
                    .add("linked_partitioning", "1").add("ids", TextUtils.join(",", itemsToUpdate.keySet())));

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            CollectionHolder holder = null;
            List<Parcelable> objectsToWrite = new ArrayList<Parcelable>();
            if (Track.class.equals(mLoadModel)) {
                holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ScModel.TracklistItemHolder.class);
                for (TracklistItem t : (ScModel.TracklistItemHolder) holder) {
                    objectsToWrite.add(((Track) itemsToUpdate.get(t.id)).updateFrom(mApp, t));
                }
            } else if (User.class.equals(mLoadModel)) {
                holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ScModel.UserlistItemHolder.class);
                for (UserlistItem u : (ScModel.UserlistItemHolder) holder) {
                    objectsToWrite.add(((User) itemsToUpdate.get(u.id)).updateFrom(u));
                }
            }

            for (Parcelable p : objectsToWrite) {
                ((ScModel) p).resolve(mApp);
            }

            publishProgress();
            SoundCloudDB.bulkInsertParcelables(mApp.getContentResolver(), objectsToWrite);

            return true;

        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return false;
    }
}

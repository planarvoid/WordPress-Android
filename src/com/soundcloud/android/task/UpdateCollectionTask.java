package com.soundcloud.android.task;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.*;

public class UpdateCollectionTask extends AsyncTask<Map<Long, Resource>, String, Boolean> {
    protected SoundCloudApplication mApp;
    protected Class<?> mLoadModel;
    protected WeakReference<LazyEndlessAdapter> mAdapterReference;

    public UpdateCollectionTask(SoundCloudApplication app,Class<?> loadModel) {
        mApp = app;
        mLoadModel = loadModel;
        if (!(Track.class.equals(mLoadModel) || User.class.equals(mLoadModel))) {
            throw new IllegalArgumentException("Collection updating only allowed for tracks and users");
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
    protected Boolean doInBackground(Map<Long, Resource>... params) {
        Map<Long,Resource> itemsToUpdate = params[0];
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
                    objectsToWrite.add(itemsToUpdate.get(t.id).getTrack().updateFrom(t));
                }
            } else if (User.class.equals(mLoadModel)) {
                holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ScModel.UserlistItemHolder.class);
                for (UserlistItem u : (ScModel.UserlistItemHolder) holder) {
                    objectsToWrite.add(itemsToUpdate.get(u.id).getUser().updateFrom(u));
                }
            } else if (Friend.class.equals(mLoadModel)) {
                holder = mApp.getMapper().readValue(resp.getEntity().getContent(), ScModel.FriendHolder.class);
                for (UserlistItem u : (ScModel.UserlistItemHolder) holder) {
                    objectsToWrite.add(itemsToUpdate.get(u.id).getUser().updateFrom(u));
                }
            }
            publishProgress();
            SoundCloudDB.bulkInsertParcelables(mApp, objectsToWrite);


            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}

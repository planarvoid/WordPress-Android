package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.*;
import static com.soundcloud.android.model.LocalCollection.insertLocalCollection;
import static com.soundcloud.android.service.sync.ApiSyncer.getAdditionsFromIds;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.EmptyCollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.LocalCollectionPage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionTask extends AsyncTask<RemoteCollectionTask.CollectionParams, RemoteCollectionTask.ReturnData, RemoteCollectionTask.ReturnData> {

    protected SoundCloudApplication mApp;
    protected CollectionParams mParams;
    protected WeakReference<Callback> mCallback;
    public boolean keepGoing;
    /* package */ CollectionHolder<? extends ScModel> mNewItems = new EmptyCollectionHolder();

    protected String mNextHref;
    protected int mResponseCode = HttpStatus.SC_OK;

    public static class CollectionParams {
        public Class<?> loadModel;
        public Uri contentUri;
        public boolean isRefresh;
        public Request request;
        public boolean refreshPageItems;
        public int startIndex;
        public int maxToLoad;
        public long timestamp;

        @Override
        public String toString() {
            return "CollectionParams{" +
                    "loadModel=" + loadModel +
                    ", contentUri=" + contentUri +
                    ", isRefresh=" + isRefresh +
                    ", request=" + request +
                    ", refreshPageItems=" + refreshPageItems +
                    ", startIndex=" + startIndex +
                    ", maxToLoad=" + maxToLoad +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public static class ReturnData {
        public CollectionHolder newItems;
        public String nextHref;
        public int responseCode;
        public boolean keepGoing;
        public boolean wasRefresh;
        public boolean success;

        public ReturnData(CollectionHolder newItems, String nextHref, int responseCode, boolean keepGoing, boolean refresh, boolean success) {
            this.newItems = newItems;
            this.nextHref = nextHref;
            this.responseCode = responseCode;
            this.keepGoing = keepGoing;
            this.wasRefresh= refresh;
            this.success = success;
        }

        @Override
        public String toString() {
            return "ReturnData{" +
                    "keepGoing=" + keepGoing +
                    ", newItems=" + newItems +
                    ", nextHref='" + nextHref + '\'' +
                    ", responseCode=" + responseCode +
                    ", wasRefresh=" + wasRefresh +
                    ", success=" + success +
                    '}';
        }
    }

    public interface Callback {
        void onPostTaskExecute(ReturnData data);
    }

    public RemoteCollectionTask(SoundCloudApplication app) {
        mApp = app;
    }

    public RemoteCollectionTask(SoundCloudApplication app, Callback callback) {
        this(app);
        mCallback = new WeakReference<Callback>(callback);
    }

    @Override
    protected void onPostExecute(ReturnData returnData) {
        respond(returnData);
    }

    protected void onProgressUpdate(ReturnData returnData) {
        respond(returnData);
    }

    protected void respond(ReturnData returnData){
        Callback callback = mCallback == null ? null : mCallback.get();
        if (callback != null) {
            callback.onPostTaskExecute(returnData);
        }
    }

    @Override
    protected ReturnData doInBackground(CollectionParams... params) {
        mParams = params[0];
        Log.i(TAG, getClass().getSimpleName() + "Loading collection with params: " + mParams);

        if (mParams.contentUri == null && mParams.request != null) {
            return doRemoteLoad();

        } else if (mParams.contentUri != null) {

            LocalData localData = new LocalData(mApp, mParams);
            keepGoing = localData.idList.size() == mParams.maxToLoad;
            try {
                insertMissingItems(localData.idList);
            } catch (IOException e) {
                // todo, keepgoing should be false??
                Log.e(TAG, "error", e);
                keepGoing = false;
            }

            mNewItems = loadLocalContent();
            for (ScModel m : mNewItems) {
                m.resolve(mApp);
            }
            return buildReturnData(true);

        } else {
            // no local content, fail
            keepGoing = false;
            return buildReturnData(false);
        }
    }

    protected ReturnData buildReturnData(boolean success){
        return new ReturnData(mNewItems, mNextHref, mResponseCode, keepGoing, mParams.isRefresh, success);
    }

    protected ReturnData doRemoteLoad() {
        try {
            HttpResponse resp = mApp.get(mParams.request);
            mResponseCode = resp.getStatusLine().getStatusCode();
            if (mResponseCode != HttpStatus.SC_OK) {
                throw new IOException("Invalid response: " + resp.getStatusLine());
            }

            // process new items and publish them
            CollectionHolder holder = ScModel.getCollectionFromStream(resp.getEntity().getContent(), mApp.getMapper(),
                    mParams.loadModel);
            mNextHref = holder == null || TextUtils.isEmpty(holder.next_href) ? null : holder.next_href;
            keepGoing = !TextUtils.isEmpty(mNextHref);
            holder.resolve(mApp);

            return buildReturnData(true);

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            keepGoing = false;
        }
        return buildReturnData(false);
    }

    private int insertMissingItems(List<Long> pageIds) throws IOException {
        Content c = Content.match(mParams.contentUri);
        return SoundCloudDB.bulkInsertParcelables(
                mApp.getContentResolver(),
                getAdditionsFromIds(mApp, mApp.getContentResolver(), pageIds, c, false));
    }

    private static class LocalData {
        LocalCollection localCollection;
        LocalCollectionPage localCollectionPage;
        List<Long> idList;

        public LocalData(SoundCloudApplication app, CollectionParams mParams) {
            localCollectionPage = null;
            localCollection = com.soundcloud.android.model.LocalCollection.fromContentUri(mParams.contentUri, app.getContentResolver(), false);
            if (localCollection == null) {
                localCollection = insertLocalCollection(mParams.contentUri, app.getContentResolver());
                 idList = new ArrayList<Long>();
            } else {
                idList = Content.match(mParams.contentUri).getLocalIds(app.getContentResolver(), SoundCloudApplication.getUserId(), mParams.startIndex, mParams.maxToLoad);
            }
        }

        @Override
        public String toString() {
            return "LocalData{" +
                    "localCollection=" + localCollection +
                    ", localCollectionPage=" + localCollectionPage +
                    ", idList=" + idList +
                    '}';
        }
    }

    protected CollectionHolder<? extends ScModel> loadLocalContent(){
        Cursor itemsCursor = mApp.getContentResolver().query(
                SoundCloudDB.addPagingParams(mParams.contentUri, mParams.startIndex, mParams.maxToLoad)
                , null, null, null, null);

        if (Track.class.equals(mParams.loadModel)) {
            return TrackHolder.fromCursor(itemsCursor);
        } else if (User.class.equals(mParams.loadModel)) {
            return UserHolder.fromCursor(itemsCursor);
        } else {
            throw new IllegalArgumentException("NOT HANDLED YET " + mParams.loadModel);
        }
    }
}

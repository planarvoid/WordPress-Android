package com.soundcloud.android.service.sync;

import android.content.ContentValues;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.app.Service;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executor;

public class ApiSyncService extends Service {
    public static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    public static final String ACTION_APPEND = "com.soundcloud.android.sync.action.APPEND";

    public static final String EXTRA_SYNC_URIS = "com.soundcloud.android.sync.extra.SYNC_URIS";
    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_IS_UI_RESPONSE = "com.soundcloud.android.sync.extra.IS_UI_RESPONSE";

    public static final int STATUS_SYNC_ERROR = 0x2;
    public static final int STATUS_SYNC_FINISHED = 0x3;

    public static final int STATUS_APPEND_ERROR = 0x4;
    public static final int STATUS_APPEND_FINISHED = 0x5;

    public static final int MAX_TASK_LIMIT = 3;
    private int mActiveTaskCount;

    /* package */ final List<ApiSyncRequest> mRequests = new ArrayList<ApiSyncRequest>();
    /* package */ final LinkedList<UriSyncRequest> mPendingUriRequests = new LinkedList<UriSyncRequest>();
    /* package */ final List<UriSyncRequest> mRunningRequests = new ArrayList<UriSyncRequest>();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Collections.SYNC_STATE,LocalCollection.SyncState.IDLE);
        getContentResolver().update(Content.COLLECTIONS.uri,cv,null,null);
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (Log.isLoggable(LOG_TAG, Log.DEBUG)) Log.d(LOG_TAG, "onStart("+intent+")");
        if (intent != null){
            enqueueRequest(new ApiSyncRequest((SoundCloudApplication) getApplication(), intent));
        }
        flushUriRequests();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* package */ void enqueueRequest(ApiSyncRequest apiRequest) {
        mRequests.add(apiRequest);

        for (UriSyncRequest request : apiRequest.uriRequests){
            // ghetto linked list search
            UriSyncRequest existingRequest = null;
            for (UriSyncRequest pendingRequest : mPendingUriRequests){
                if (pendingRequest.equals(request)) {
                    existingRequest = pendingRequest;
                    break;
                }
            }
            if (existingRequest == null && !mRunningRequests.contains(request)) {
                request.init(true);
                if (apiRequest.isUIResponse){
                    mPendingUriRequests.add(0, request);
                } else {
                    mPendingUriRequests.add(request);
                }
            } else if (existingRequest != null && apiRequest.isUIResponse && !mPendingUriRequests.getFirst().equals(existingRequest)){
                mPendingUriRequests.remove(existingRequest);
                mPendingUriRequests.addFirst(existingRequest);
            }
        }
    }

    /* package */ void onUriSyncResult(UriSyncRequest uriRequest){
        for (ApiSyncRequest apiRequest : new ArrayList<ApiSyncRequest>(mRequests)){
            if (apiRequest.onUriResult(uriRequest)){
                mRequests.remove(apiRequest);
            }
        }
        mRunningRequests.remove(uriRequest);
    }

    void flushUriRequests() {
        if (mPendingUriRequests.isEmpty() && mRunningRequests.isEmpty()){
            stopSelf();
        } else {
            while (mActiveTaskCount < MAX_TASK_LIMIT && !mPendingUriRequests.isEmpty()) {
                final UriSyncRequest syncRequest = mPendingUriRequests.poll();
                mRunningRequests.add(syncRequest);
                new ApiTask().executeOnThreadPool(syncRequest);
            }
        }
    }

    static List<Uri> getUrisToSync(Intent intent) {
        ArrayList<Uri> contents = intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        if (contents == null) {
            contents = new ArrayList<Uri>();
        }
        if (intent.getData() != null) {
            contents.add(intent.getData());
        }
        return contents;
    }

    private class ApiTask extends AsyncTask<UriSyncRequest, UriSyncRequest, Void> {

        public final android.os.AsyncTask<UriSyncRequest, UriSyncRequest, Void> executeOnThreadPool(
                UriSyncRequest... params) {
            if (Build.VERSION.SDK_INT < 11) {
                // The execute() method uses a thread pool
                return execute(params);
            } else {
                // The execute() method uses a single thread,
                // so call executeOnExecutor() instead.
                try {
                    Method method = android.os.AsyncTask.class.getMethod("executeOnExecutor",
                            Executor.class, Object[].class);
                    Field field = android.os.AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
                    Object executor = field.get(null);
                    method.invoke(this, executor, params);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Unexpected NoSuchMethodException", e);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("Unexpected NoSuchFieldException", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Unexpected IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Unexpected InvocationTargetException", e);
                }
                return this;
            }
        }

        @Override
        protected void onPreExecute() {
            mActiveTaskCount++;
        }

        @Override
        protected Void doInBackground(UriSyncRequest... tasks) {
            for (UriSyncRequest task : tasks) {
                publishProgress(task.execute());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(UriSyncRequest... progress) {
            for (UriSyncRequest request : progress) {
                onUriSyncResult(request);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            mActiveTaskCount--;
            flushUriRequests();
        }
    }

    public static void appendSyncStats(SyncResult from, SyncResult to) {
        to.stats.numAuthExceptions += from.stats.numAuthExceptions;
        to.stats.numIoExceptions += from.stats.numIoExceptions;
        to.stats.numParseExceptions += from.stats.numParseExceptions;
        // TODO more stats?
    }
}

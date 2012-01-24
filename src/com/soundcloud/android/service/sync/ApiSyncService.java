package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.api.CloudAPI;

import android.app.Service;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
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

    /* package */ final List<ApiRequest> mRequests = new ArrayList<ApiRequest>();
    /* package */ final LinkedList<UriRequest> mPendingUriRequests = new LinkedList<UriRequest>();
    /* package */ final List<UriRequest> mRunningRequests = new ArrayList<UriRequest>();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(LOG_TAG, "onStart("+intent+")");
        if (intent != null){
            enqueueRequest(new ApiRequest((SoundCloudApplication) getApplication(), intent));
        }
        flushUriRequests();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* package */ void enqueueRequest(ApiRequest apiRequest) {
        mRequests.add(apiRequest);

        for (UriRequest request : apiRequest.uriRequests){
            // ghetto linked list search
            UriRequest existingRequest = null;
            for (UriRequest pendingRequest : mPendingUriRequests){
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

    /* package */ void onUriSyncResult(UriRequest uriRequest){
        for (ApiRequest apiRequest : new ArrayList<ApiRequest>(mRequests)){
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
                final UriRequest syncRequest = mPendingUriRequests.poll();
                mRunningRequests.add(syncRequest);
                new ApiTask().executeOnThreadPool(syncRequest);
            }
        }
    }

    private static List<Uri> getUrisToSync(Intent intent) {
        ArrayList<Uri> contents = intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS);
        if (contents == null) {
            contents = new ArrayList<Uri>();
        }
        if (intent.getData() != null) {
            contents.add(intent.getData());
        }
        return contents;
    }

    /* package */ static class ApiRequest {
        private final SoundCloudApplication app;
        private final ResultReceiver resultReceiver;
        private final String action;
        private final Set<UriRequest> requestsRemaining;

        public final List<UriRequest> uriRequests;
        public boolean isUIResponse;

        // results
        private final Bundle resultData = new Bundle();
        private final SyncResult requestResult = new SyncResult();

        public ApiRequest(SoundCloudApplication application, Intent intent) {
            app = application;
            resultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
            isUIResponse = intent.getBooleanExtra(EXTRA_IS_UI_RESPONSE, false);
            action = intent.getAction();

            final List<Uri> urisToSync = getUrisToSync(intent);
            uriRequests = new ArrayList<UriRequest>();
            for (Uri uri : urisToSync){
                uriRequests.add(new UriRequest(application,uri,action));
            }
            requestsRemaining = new HashSet<UriRequest>(uriRequests);
        }

        public boolean onUriResult(UriRequest uriRequest) {
            if (requestsRemaining.contains(uriRequest)) {
                requestsRemaining.remove(uriRequest);
                resultData.putBoolean(uriRequest.uri.toString(), uriRequest.result.wasChanged);
                if (!uriRequest.result.success) appendSyncStats(uriRequest.result.syncResult, this.requestResult);
            }

            if (requestsRemaining.isEmpty()) {
                if (resultReceiver != null) {
                    if (uriRequest.result.success) {
                        resultReceiver.send(action != null && action.equals(ACTION_APPEND) ? STATUS_APPEND_FINISHED : STATUS_SYNC_FINISHED, resultData);
                    } else {
                        final Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_SYNC_RESULT, requestResult);
                        resultReceiver.send(action != null && action.equals(ACTION_APPEND) ? STATUS_APPEND_ERROR : STATUS_SYNC_ERROR, bundle);
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /* package */ static class UriRequest {
        private final SoundCloudApplication app;
        private final Uri uri;
        private final String action;
        private LocalCollection localCollection;

        public ApiSyncer.Result result;
        // results

        public UriRequest(SoundCloudApplication application, Uri uri, String action){
            this.app = application;
            this.uri = uri;
            this.action = action;
        }

        public void init(boolean setIsPending){
            localCollection = LocalCollection.fromContentUri(uri,app.getContentResolver(),true);
            if (setIsPending){
                localCollection.updateSyncState(LocalCollection.SyncState.PENDING, app.getContentResolver());
            }
        }

        public UriRequest execute() {
            ApiSyncer syncer = new ApiSyncer(app);
            localCollection.updateSyncState(LocalCollection.SyncState.SYNCING, app.getContentResolver());
            try {
                result = syncer.syncContent(uri, action);
                localCollection.onSyncComplete(result, app.getContentResolver());
                result.success = true;
            } catch (CloudAPI.InvalidTokenException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
                result = ApiSyncer.Result.fromAuthException(uri);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
                result = ApiSyncer.Result.fromIOException(uri);
            }
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UriRequest that = (UriRequest) o;

            if (action != null ? !action.equals(that.action) : that.action != null) return false;
            if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = uri != null ? uri.hashCode() : 0;
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }

        public Uri getUri(){
            return uri;
        }

        @Override
        public String toString() {
            return "UriRequest{" +
                    "uri=" + uri +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

     private class ApiTask extends AsyncTask<UriRequest, UriRequest, Void> {

        public final android.os.AsyncTask<UriRequest, UriRequest, Void> executeOnThreadPool(
                UriRequest... params) {
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
        protected Void doInBackground(UriRequest... tasks) {
            for (UriRequest task : tasks) {
                publishProgress(task.execute());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(UriRequest... progress) {
            for (UriRequest request : progress) {
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
        // TODO more stats?
    }
}

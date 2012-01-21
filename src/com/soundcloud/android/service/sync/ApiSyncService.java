package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
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

    public static final String EXTRA_SYNC_URIS = "com.soundcloud.android.sync.extra.SYNC_URIS";
    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_IS_UI_RESPONSE = "com.soundcloud.android.sync.extra.IS_UI_RESPONSE";

    public static final int STATUS_SYNC_ERROR = 0x2;
    public static final int STATUS_SYNC_FINISHED = 0x3;

    public static final int MAX_TASK_LIMIT = 3;
    private int mActiveTaskCount;

    /* package */ final List<ApiSyncRequest> mRequests = new ArrayList<ApiSyncRequest>();
    /* package */ final LinkedList<UriSyncRequest> mPendingUriRequests = new LinkedList<UriSyncRequest>();
    /* package */ final List<Uri> mRunningRequestUris = new ArrayList<Uri>();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(LOG_TAG, "onStart("+intent+")");

        enqueueRequest(new ApiSyncRequest((SoundCloudApplication) getApplication(), intent));
        flushUriRequests();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* package */ void enqueueRequest(ApiSyncRequest request) {
        mRequests.add(request);

        for (Uri uri : request.urisToSync){
            // ghetto linked list search
            UriSyncRequest existingRequest = null;
            for (UriSyncRequest req : mPendingUriRequests){
                if (req.uri.equals(uri)) {
                    existingRequest = req;
                    break;
                }
            }
            if (existingRequest == null && !mRunningRequestUris.contains(uri)) {
                final UriSyncRequest uriSyncRequest = new UriSyncRequest((SoundCloudApplication) getApplication(), uri, true);
                if (request.isUIResponse){
                    mPendingUriRequests.add(0, uriSyncRequest);
                } else {
                    mPendingUriRequests.add(uriSyncRequest);
                }
            } else if (existingRequest != null && request.isUIResponse && !mPendingUriRequests.getFirst().equals(existingRequest)){
                mPendingUriRequests.remove(existingRequest);
                mPendingUriRequests.addFirst(existingRequest);
            }
        }
    }

    /* package */ void onUriSyncResult(ApiSyncer.Result result){
        for (ApiSyncRequest request : new ArrayList<ApiSyncRequest>(mRequests)){
            if (request.onUriResult(result)){
                mRequests.remove(request);
            }
        }
        mRunningRequestUris.remove(result.uri);
    }

    void flushUriRequests() {
        if (mPendingUriRequests.isEmpty() && mRunningRequestUris.isEmpty()){
            stopSelf();
        } else {
            while (mActiveTaskCount < MAX_TASK_LIMIT && !mPendingUriRequests.isEmpty()) {
                final UriSyncRequest syncRequest = mPendingUriRequests.poll();
                mRunningRequestUris.add(syncRequest.uri);
                new ApiSyncTask().executeOnThreadPool(syncRequest);
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

    /* package */ static class ApiSyncRequest {
        private final SoundCloudApplication app;
        private final ResultReceiver resultReceiver;
        private final List<Uri> urisToSync;
        private final Set<Uri> urisRemaining;

        public boolean isUIResponse;

        // results
        private final Bundle resultData = new Bundle();
        private final SyncResult requestResult = new SyncResult();

        public ApiSyncRequest(SoundCloudApplication application, Intent intent) {
            app = application;
            resultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
            isUIResponse = intent.getBooleanExtra(EXTRA_IS_UI_RESPONSE, false);
            urisToSync = getUrisToSync(intent);
            urisRemaining = new HashSet<Uri>(urisToSync);
        }

        public boolean onUriResult(ApiSyncer.Result uriResult) {
            if (urisRemaining.contains(uriResult.uri)) {
                urisRemaining.remove(uriResult.uri);
                resultData.putBoolean(uriResult.uri.toString(), uriResult.wasChanged);
                if (!uriResult.success) appendSyncStats(uriResult.syncResult, this.requestResult);
            }

            if (urisRemaining.isEmpty()) {
                if (resultReceiver != null) {
                    if (uriResult.success) {
                        resultReceiver.send(STATUS_SYNC_FINISHED, resultData);
                    } else {
                        final Bundle bundle = new Bundle();
                        bundle.putParcelable(EXTRA_SYNC_RESULT, requestResult);
                        resultReceiver.send(STATUS_SYNC_ERROR, bundle);
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /* package */ static class UriSyncRequest {
        private final SoundCloudApplication app;
        private final Uri uri;
        private LocalCollection localCollection;
        // results

        public UriSyncRequest(SoundCloudApplication application, Uri uri){
              this(application,uri,false);
        }

        public UriSyncRequest(SoundCloudApplication application, Uri uri, boolean setIsPending){
            this.app = application;
            this.uri = uri;
            localCollection = LocalCollection.fromContentUri(uri,app.getContentResolver(),true);
            if (setIsPending){
                localCollection.updateSyncState(LocalCollection.SyncState.PENDING, app.getContentResolver());
            }
        }

        public ApiSyncer.Result execute() {
            ApiSyncer syncer = new ApiSyncer(app);
            localCollection.updateSyncState(LocalCollection.SyncState.SYNCING, app.getContentResolver());
            try {
                ApiSyncer.Result result = syncer.syncContent(Content.byUri(uri));
                localCollection.onSyncComplete(result, app.getContentResolver());
                result.success = true;
                return result;

            } catch (CloudAPI.InvalidTokenException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
                return ApiSyncer.Result.fromAuthException(uri);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                localCollection.updateSyncState(LocalCollection.SyncState.IDLE, app.getContentResolver());
                return ApiSyncer.Result.fromIOException(uri);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UriSyncRequest that = (UriSyncRequest) o;

            if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return uri != null ? uri.hashCode() : 0;
        }

        public Uri getUri(){
            return uri;
        }
    }

     private class ApiSyncTask extends AsyncTask<UriSyncRequest, ApiSyncer.Result, Void> {

        public final android.os.AsyncTask<UriSyncRequest, ApiSyncer.Result, Void> executeOnThreadPool(
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
        protected void onProgressUpdate(ApiSyncer.Result... values) {
            for (ApiSyncer.Result result : values) {
                onUriSyncResult(result);
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

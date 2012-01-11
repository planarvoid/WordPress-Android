package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.api.CloudAPI;

import android.app.IntentService;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class ApiSyncService extends IntentService {
    public static final String LOG_TAG = ApiSyncer.class.getSimpleName();

    public static final String EXTRA_STATUS_RECEIVER = "com.soundcloud.android.sync.extra.STATUS_RECEIVER";
    public static final String EXTRA_SYNC_RESULT = "com.soundcloud.android.sync.extra.SYNC_RESULT";
    public static final String EXTRA_CHECK_PERFORM_LOOKUPS = "com.soundcloud.android.sync.extra.PERFORM_LOOKUPS";
    public static final String EXTRA_IS_MANUAL_SYNC = "com.soundcloud.android.sync.extra.IS_MANUAL_SYNC";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_SYNC_ERROR = 0x2;
    public static final int STATUS_SYNC_FINISHED = 0x3;
    public static final int STATUS_REFRESH_PAGE_FINISHED = 0x4;
    public static final int STATUS_REFRESH_PAGE_ERROR = 0x4;

    public static final String SYNC_COLLECTION_ACTION = "com.soundcloud.android.sync.action.SYNC_COLLECTION";
    public static final String REFRESH_PAGE_ACTION = "com.soundcloud.android.sync.action.REFRESH_PAGE";

    public static final int MAX_TASK_LIMIT = 3;
    private int mActiveTaskCount;
    private final LinkedList<ApiSyncRequest> mRequests = new LinkedList<ApiSyncRequest>();

    public ApiSyncService() {
        super(ApiSyncService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Cloud Api service started");
        enqueueRequest(new ApiSyncRequest((SoundCloudApplication) getApplication(), intent));
    }

    private void enqueueRequest(ApiSyncRequest request) {
        mRequests.add(request);
        flushRequests();
    }


    void flushRequests() {
        while (mActiveTaskCount < MAX_TASK_LIMIT && !mRequests.isEmpty()) {
            new ApiSyncTask().executeOnThreadPool(mRequests.poll());
        }
    }

    private List<Uri> getUrisToSync(Intent intent) {
        ArrayList<Uri> contents = new ArrayList<Uri>();
        final ArrayList<String> syncUriStrings = intent.getStringArrayListExtra("syncUris");
        if (syncUriStrings != null) {
            for (String s : intent.getStringArrayListExtra("syncUris")) {
                contents.add(Uri.parse(s));
            }
        }

        if (intent.getData() != null) {
            contents.add(intent.getData());
        }
        return contents;
    }

    private void sendSyncError(ResultReceiver receiver, SyncResult syncResult){
        Log.i(LOG_TAG,"Sending sync error:" + syncResult);
        if (receiver != null) {
            final Bundle bundle = new Bundle();
            if (syncResult != null) bundle.putParcelable(EXTRA_SYNC_RESULT, syncResult);
            receiver.send(STATUS_SYNC_ERROR, bundle);
        }
    }

    private class ApiSyncRequest {
        private final SoundCloudApplication app;
        private final ResultReceiver resultReceiver;
        private final Intent intent;

        // results
        private boolean success;
        private final Bundle resultData = new Bundle();
        private final SyncResult syncResult = new SyncResult();

        public ApiSyncRequest(SoundCloudApplication application, Intent intent){
            this.intent = intent;
            app = application;
            resultReceiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        }

        public ApiSyncRequest execute() {
            ApiSyncer syncer = new ApiSyncer((SoundCloudApplication) getApplication());

            try {
                if (intent.getAction() == REFRESH_PAGE_ACTION) {
                        syncer.loadContent(intent.getData());
                } else {

                    final long startSync = System.currentTimeMillis();
                    for (Uri u : getUrisToSync(intent)) {
                        Log.i(LOG_TAG, "Syncing content with uri: " + u.toString());
                        resultData.putBoolean(u.toString(), syncer.syncContent(Content.byUri(u)));
                    }
                    syncer.performDbAdditions(intent.getBooleanExtra(EXTRA_CHECK_PERFORM_LOOKUPS, true));
                    Log.d(LOG_TAG, "Done sync in " + (System.currentTimeMillis() - startSync) + " ms");

                }

                success = true;
                return this;

            } catch (CloudAPI.InvalidTokenException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                syncResult.stats.numAuthExceptions++;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem while syncing", e);
                syncResult.stats.numIoExceptions++;
            }

            success = false;
            return this;
        }

        public void publishResult() {
            if (resultReceiver != null){
                if (intent.getAction() == REFRESH_PAGE_ACTION) {
                    resultReceiver.send(success ? STATUS_REFRESH_PAGE_FINISHED : STATUS_REFRESH_PAGE_ERROR, resultData);
                }  else {
                    if (success){
                        resultReceiver.send(STATUS_SYNC_FINISHED, resultData);
                    } else {
                        sendSyncError(resultReceiver, syncResult);
                    }
                }

            }
        }
    }

     private class ApiSyncTask extends AsyncTask<ApiSyncRequest, ApiSyncRequest, Void> {

        public final android.os.AsyncTask<ApiSyncRequest, ApiSyncRequest, Void> executeOnThreadPool(
                ApiSyncRequest... params) {
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
        protected Void doInBackground(ApiSyncRequest... requests) {
            for (ApiSyncRequest request : requests) {
                    publishProgress(request.execute());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(ApiSyncRequest... values) {
            for (ApiSyncRequest request : values) {
                request.publishResult();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            mActiveTaskCount--;
            flushRequests();
        }
    }
}

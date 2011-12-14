package com.soundcloud.android.c2dm;

import static com.soundcloud.android.c2dm.C2DMReceiver.TAG;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

public class DeleteRegIdTask extends AsyncApiTask<String, Void, Boolean> {
    private PowerManager.WakeLock lock;

    public DeleteRegIdTask(AndroidCloudAPI api, PowerManager.WakeLock wakeLock) {
        super(api);
        lock = wakeLock;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (lock != null) lock.acquire();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (lock != null) lock.release();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            HttpResponse resp = mApi.delete(Request.to(params[0]));
            final int code = resp.getStatusLine().getStatusCode();
            switch (code) {
                case HttpStatus.SC_OK:
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "deleted remote device "+params[0]);
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "already deleted remote device "+params[0]);
                    return true;

                default:
                    Log.w(TAG, DeleteRegIdTask.class.getSimpleName()+": unexpected status code "
                            + resp.getStatusLine());
                    return false;
            }
        } catch (IOException e) {
            Log.w(TAG, getClass().getSimpleName()+": unexpected IO error", e);
            return false;
        }
    }
}

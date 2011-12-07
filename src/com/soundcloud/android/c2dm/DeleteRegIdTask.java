package com.soundcloud.android.c2dm;

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
                    Log.d(C2DMReceiver.TAG, "deleted remote device "+params[0]);
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    Log.d(C2DMReceiver.TAG, "already deleted remote device "+params[0]);
                    return true;

                default:
                    Log.w(C2DMReceiver.TAG, DeleteRegIdTask.class.getSimpleName()+": unexpected status code "
                            + resp.getStatusLine());
                    return false;
            }
        } catch (IOException e) {
            Log.w(C2DMReceiver.TAG, getClass().getSimpleName()+": unexpected IO error", e);
            return false;
        }
    }
}


package com.soundcloud.android.task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.CloudAPI;
import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;

import com.soundcloud.utils.http.ProgressListener;
import org.apache.http.message.BasicNameValuePair;

public abstract class UploadTask extends AsyncTask<UploadTask.Params, Long, Boolean> implements ProgressListener {
    private static final String TAG = UploadTask.class.getSimpleName();

    private long transferred;
    private CloudAPI api;

    public static class Params {
        private File trackFile = null;
        private File artworkFile = null;
        private List<NameValuePair> params = new ArrayList<NameValuePair>();

        public Params(File file, HashMap<String, String> map) {
            this.trackFile = file;

            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (!(entry.getKey().contentEquals("pcm_path") ||
                      entry.getKey().contentEquals("image_path")))

                    params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            if (!TextUtils.isEmpty(map.get("artwork_path"))) {
                artworkFile = new File(map.get("artwork_path"));
            }
        }
    }

    private long size(File f) {
        return f == null ? 0 : f.length();
    }

    public UploadTask(CloudAPI api) {
        this.api = api;
    }


    @Override
    protected Boolean doInBackground(final Params... params) {
        final Params param = params[0];

        final FileBody trackBody = new FileBody(param.trackFile);
        final FileBody artworkBody = param.artworkFile == null ? null : new FileBody(param.artworkFile);

        long totalTransfer = size(param.trackFile) + size(param.artworkFile);

        final Thread uploadThread = new Thread(new Runnable() {
            public void run() {
                try {
                     api.upload(trackBody, artworkBody, param.params, UploadTask.this);
                } catch (Exception e) {
                    Log.e(TAG, "error uploading", e);
                }
            }
        });

        uploadThread.start();
        while (uploadThread.isAlive()) {
            publishProgress(transferred,  totalTransfer);
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        return !isCancelled();
    }

    @Override
    public void transferred(long amount) {
        transferred = amount;
    }

}

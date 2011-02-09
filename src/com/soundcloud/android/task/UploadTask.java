package com.soundcloud.android.task;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.CloudAPI;
import com.soundcloud.utils.http.ProgressListener;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class UploadTask extends AsyncTask<UploadTask.Params, Long, UploadTask.Params> implements ProgressListener {
    private static final String TAG = UploadTask.class.getSimpleName();

    private long transferred;
    private CloudAPI api;

    public static class Params {
        public static final String PCM_PATH = "pcm_path";
        public static final String OGG_FILENAME = "ogg_filename";
        public static final String ARTWORK_PATH = "artwork_path";

        private boolean failed;

        public final File trackFile, encodedFile;
        public final File artworkFile;

        public File resizedFile;

        private final Map<String, String> map;

        public String get(String s) {
            return map.get(s);
        }

        public Params(Map<String, String> map) {
            this.map = map;

            this.trackFile   = new File(map.remove(PCM_PATH));
            this.encodedFile = new File(map.remove(OGG_FILENAME));

            if (TextUtils.isEmpty(map.get(ARTWORK_PATH))) {
                artworkFile = null;
            } else {
                artworkFile = new File(map.remove(ARTWORK_PATH));
            }
        }

        public File artworkFile() {
            return resizedFile != null ? resizedFile :
                   artworkFile != null ? artworkFile : null;
        }

        public Params fail() {
            this.failed = true;
            return this;
        }

        public boolean isSuccess() {
            return !failed;
        }
    }

    public UploadTask(CloudAPI api) {
        this.api = api;
    }

    @Override
    protected Params doInBackground(final Params... params) {
        final Params param = params[0];

        final FileBody track = new FileBody(param.encodedFile);
        final FileBody artwork = param.artworkFile() == null ? null : new FileBody(param.artworkFile());

        long totalTransfer = track.getContentLength() +
                             (artwork == null ? 0 : artwork.getContentLength());


        final Thread uploadThread = new Thread(new Runnable() {
            public void run() {
                try {
                    final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
                    for (Map.Entry<String, String> entry : param.map.entrySet()) {
                        apiParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                    }
                    HttpResponse response = api.upload(track, artwork, apiParams, UploadTask.this);
                    StatusLine status = response.getStatusLine();

                    if (status.getStatusCode() == HttpStatus.SC_CREATED) {
                        Log.d(TAG, "Upload successful");
                    } else {
                        Log.w(TAG, String.format("Upload failed: %d (%s)",
                                   status.getStatusCode(),
                                   status.getReasonPhrase()));

                        param.fail();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error uploading", e);
                }
            }
        });
        uploadThread.start();

        while (uploadThread.isAlive()) {
            publishProgress(transferred, totalTransfer);
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        return isCancelled() ? param.fail() : param;
    }

    @Override
    public void transferred(long amount) {
        transferred = amount;
    }
}

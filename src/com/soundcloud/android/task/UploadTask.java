package com.soundcloud.android.task;

import android.os.AsyncTask;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UploadTask extends AsyncTask<UploadTask.Params, Long, UploadTask.Params> implements ProgressListener {
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

        private final Map<String, ?> map;

        public String get(String s) {
            return map.get(s).toString();
        }

        public Params(Map<String, ?> map) {
            this.map = map;

            if (!map.containsKey(PCM_PATH) || !map.containsKey(OGG_FILENAME)) {
                throw new IllegalArgumentException("Need to specify both "
                        + PCM_PATH + " and " + OGG_FILENAME);
            }

            this.trackFile   = new File(String.valueOf(map.remove(PCM_PATH)));
            this.encodedFile = new File(String.valueOf(map.remove(OGG_FILENAME)));


            if (map.containsKey(ARTWORK_PATH)) {
                artworkFile = new File(String.valueOf(map.remove(ARTWORK_PATH)));
            } else {
                artworkFile = null;
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

        public List<NameValuePair> getApiParams() {
            final List<NameValuePair> apiParams = new ArrayList<NameValuePair>();
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                 if (entry.getValue() instanceof Iterable) {
                     for (Object o : (Iterable)entry.getValue()) {
                         apiParams.add(new BasicNameValuePair(entry.getKey(), o.toString()));
                     }
                 } else {
                    apiParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
                 }
            }
            return apiParams;
        }
    }

    public UploadTask(CloudAPI api) {
        this.api = api;
    }

    @Override
    protected Params doInBackground(final Params... params) {
        final Params param = params[0];

        if (!param.encodedFile.exists()) {
            throw new IllegalArgumentException("Encoded file does not exist");
        }

        if (param.encodedFile.length() == 0) {
            throw new IllegalArgumentException("Encoded file is empty");
        }

        final FileBody track = new FileBody(param.encodedFile);
        final FileBody artwork = param.artworkFile() == null ? null : new FileBody(param.artworkFile());

        long totalTransfer = track.getContentLength() +
                             (artwork == null ? 0 : artwork.getContentLength());


        final Thread uploadThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Log.v(TAG, "starting upload of " + param.encodedFile);

                    HttpResponse response = api.upload(track, artwork, param.getApiParams(), UploadTask.this);
                    StatusLine status = response.getStatusLine();

                    if (status.getStatusCode() == HttpStatus.SC_CREATED) {
                        Log.d(TAG, "Upload successful");
                    } else {
                        Log.w(TAG, String.format("Upload failed: %d (%s)",
                                   status.getStatusCode(),
                                   status.getReasonPhrase()));

                        param.fail();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error uploading", e);
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

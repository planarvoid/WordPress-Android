package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class UploadTask extends AsyncTask<UploadTask.Params, Long, UploadTask.Params> implements Request.TransferProgressListener {
    private long transferred;
    private CloudAPI api;

    public static class Params {
        public static final String LOCAL_RECORDING_ID  = "local_recording_id";
        public static final String SOURCE_PATH  = "source_path";
        public static final String OGG_FILENAME = "ogg_filename";
        public static final String ARTWORK_PATH = "artwork_path";
        public static final String ENCODE       = "encode";

        private boolean failed;
        public final boolean encode;

        public long local_recording_id;

        public final File trackFile, encodedFile;
        public final File artworkFile;

        public File resizedFile;

        private final Map<String, ?> map;

        public String get(String s) {
            return map.get(s).toString();
        }

        public Params(Map<String, ?> map) {
            this.map = map;
            this.encode = map.remove(ENCODE) != null;
            if (map.containsKey(LOCAL_RECORDING_ID)) {
                this.local_recording_id = (Long) map.remove(LOCAL_RECORDING_ID);
            }

            if (!map.containsKey(SOURCE_PATH)) {
                throw new IllegalArgumentException("Need to specify " + SOURCE_PATH);
            }

            if (encode && !map.containsKey(OGG_FILENAME)) {
                throw new IllegalArgumentException("Need to specify " + OGG_FILENAME);
            }

            this.trackFile   = new File(String.valueOf(map.remove(SOURCE_PATH)));
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

        public Request getRequest(File file, Request.TransferProgressListener listener) {
            final Request request = new Request(Endpoints.TRACKS);
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                 if (entry.getValue() instanceof Iterable) {
                     for (Object o : (Iterable)entry.getValue()) {
                         request.add(entry.getKey(), o.toString());
                     }
                 } else {
                    request.add(entry.getKey(), entry.getValue().toString());
                 }
            }
            return request.withFile(com.soundcloud.api.Params.Track.ASSET_DATA, file)
                          .withFile(com.soundcloud.api.Params.Track.ARTWORK_DATA, artworkFile())
                          .setProgressListener(listener);
        }
    }

    public UploadTask(CloudAPI api) {
        this.api = api;
    }

    @Override
    protected Params doInBackground(final Params... params) {
        final Params param = params[0];
        final File toUpload = param.encode ? param.encodedFile : param.trackFile;

        if (!toUpload.exists()) {
            throw new IllegalArgumentException("File to be uploaded does not exist");
        }

        if (toUpload.length() == 0) {
            throw new IllegalArgumentException("File to be uploaded is empty");
        }

        final FileBody track = new FileBody(toUpload);
        final FileBody artwork = param.artworkFile() == null ? null : new FileBody(param.artworkFile());

        long totalTransfer = track.getContentLength() +
                             (artwork == null ? 0 : artwork.getContentLength());


        final Thread uploadThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Log.v(TAG, "starting upload of " + toUpload);
                    // TODO hold wifi lock during upload
                    HttpResponse response = api.post(param.getRequest(toUpload, UploadTask.this));
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
                    param.fail();
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

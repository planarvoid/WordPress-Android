package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.model.Upload;
import com.soundcloud.android.utils.record.CloudRecorder;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

public class UploadTask extends AsyncTask<UploadTask.Params, Long, UploadTask.Params> {
    private CloudAPI api;

    public UploadTask(CloudAPI api) {
            this.api = api;
    }


    // XXX get rid of this class, use Upload
    public static class Params {

        private boolean failed;
        private final Map<String, ?> map;

        public File artworkFile;
        public File encodedFile;
        public File trackFile;
        public boolean encode;
        public File resizedFile;
        public boolean is_native_recording;

        public String get(String s) {
            return map.get(s).toString();
        }

        public Params(Upload upload) {
            map = upload.toTrackMap();
            encode = upload.encode;
            trackFile = new File(upload.trackPath);
            encodedFile = upload.encodedFile;
            is_native_recording = upload.is_native_recording;
            if (!TextUtils.isEmpty(upload.artworkPath)) artworkFile = new File(upload.artworkPath);
        }

        public Params fail() {
            this.failed = true;
            return this;
        }

        public boolean isSuccess() {
            return !failed;
        }

        public File artworkFile() {
            return resizedFile != null ? resizedFile :
                   artworkFile != null ? artworkFile : null;
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


            final String fileName;
            if (is_native_recording) {
                final String title = map.get(com.soundcloud.api.Params.Track.TITLE) == null ? "unknown" :
                                     map.get(com.soundcloud.api.Params.Track.TITLE).toString();

                fileName = String.format("%s.%s", URLEncoder.encode(title.replace(" ", "_")), encode ? "ogg" : "mp4");
            } else {
                fileName = file.getName();
            }

            return request.withFile(com.soundcloud.api.Params.Track.ASSET_DATA, file, fileName)
                          .withFile(com.soundcloud.api.Params.Track.ARTWORK_DATA, artworkFile())
                          .setProgressListener(listener);
        }
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

        final long totalTransfer = track.getContentLength() +
                             (artwork == null ? 0 : artwork.getContentLength());

        try {
            Log.v(TAG, "starting upload of " + toUpload);
            // TODO hold wifi lock during upload
            HttpResponse response = api.post(param.getRequest(toUpload, new Request.TransferProgressListener() {
                long lastPublished;

                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) throw new IOException("canceled");
                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        lastPublished = System.currentTimeMillis();
                        publishProgress(transferred, totalTransfer);
                    }
                }
            }));

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
        return isCancelled() ? param.fail() : param;
    }
}

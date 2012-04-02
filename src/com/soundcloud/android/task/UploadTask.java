package com.soundcloud.android.task;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.model.Upload;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class UploadTask extends AsyncTask<Upload, Long, Upload> {
    private CloudAPI api;

    public UploadTask(CloudAPI api) {
            this.api = api;
    }

    @Override
    protected Upload doInBackground(final Upload... params) {
        final Upload upload = params[0];
        final File toUpload = upload.encodedSoundFile != null ? upload.encodedSoundFile : upload.soundFile;

        if (!toUpload.exists()) {
            throw new IllegalArgumentException("File to be uploaded does not exist");
        }

        if (toUpload.length() == 0) {
            throw new IllegalArgumentException("File to be uploaded is empty");
        }

        final FileBody soundBody = new FileBody(toUpload);
        final FileBody artworkBody = upload.artworkFile == null ? null : new FileBody(upload.artworkFile);

        final long totalTransfer = soundBody.getContentLength() + (artworkBody == null ? 0 : artworkBody.getContentLength());

        try {
            if (isCancelled()) throw new CanceledUploadException();

            Log.v(TAG, "starting upload of " + toUpload);
            // TODO hold wifi lock during upload

            HttpResponse response = api.post(upload.getRequest(toUpload, new Request.TransferProgressListener() {
                long lastPublished;

                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) throw new CanceledUploadException();

                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        lastPublished = System.currentTimeMillis();
                        publishProgress(transferred, totalTransfer);
                    }
                }
            }));

            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == HttpStatus.SC_CREATED) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Upload successful");
                return upload.succeed();
            } else {
                final String message = String.format("Upload failed: %d (%s)",
                        status.getStatusCode(),
                        status.getReasonPhrase());

                Log.w(TAG, message);
                return upload.setUploadException(new IOException(message));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error uploading", e);
            return upload.setUploadException(e);
        }
    }

    public static class CanceledUploadException extends IOException {}
}

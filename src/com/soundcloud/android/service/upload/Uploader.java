package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.model.Recording;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Uploader implements Runnable {
    private CloudAPI api;
    private Recording upload;
    private boolean mCanceled;
    private LocalBroadcastManager mBroadcastManager;

    public Uploader(CloudAPI api, Recording recording) {
        this.api = api;
        this.upload = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance((Context) api);
        mBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cancel();
            }
        }, new IntentFilter(UploadService.UPLOAD_CANCEL));
    }

    public boolean isCancelled() {
        return mCanceled;
    }

    public void cancel() {
        mCanceled = true;
    }


    @Override
    public void run() {
        try {
            upload();
        } catch (IllegalArgumentException e) {
            onUploadFailed(e);
        }
    }

    private void upload() {
        final File toUpload = upload.encoded_audio_path != null ? upload.encoded_audio_path : upload.audio_path;

        if (toUpload == null || !toUpload.exists()) {
            throw new IllegalArgumentException("File to be uploaded does not exist");
        }

        if (toUpload.length() == 0) {
            throw new IllegalArgumentException("File to be uploaded is empty");
        }

        final FileBody soundBody = new FileBody(toUpload);
        final FileBody artworkBody = upload.artwork_path == null ? null : new FileBody(upload.artwork_path);

        final long totalTransfer = soundBody.getContentLength() + (artworkBody == null ? 0 : artworkBody.getContentLength());

        try {
            if (isCancelled()) throw new CanceledUploadException();
            Log.v(TAG, "starting upload of " + toUpload);

            // TODO hold wifi lock during upload
            broadcast(UploadService.UPLOAD_STARTED);
            HttpResponse response = api.post(upload.getRequest((Context) api, toUpload, new Request.TransferProgressListener() {
                long lastPublished;
                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) throw new CanceledUploadException();

                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        final int progress = (int) Math.min(100, (100 * transferred) / totalTransfer);
                        mBroadcastManager.sendBroadcast(new Intent(UploadService.UPLOAD_PROGRESS)
                                .putExtra(UploadService.EXTRA_RECORDING, upload)
                                .putExtra(UploadService.EXTRA_TRANSFERRED, transferred)
                                .putExtra(UploadService.EXTRA_PROGRESS, progress)
                                .putExtra(UploadService.EXTRA_TOTAL, totalTransfer));

                        lastPublished = System.currentTimeMillis();
                    }
                }
            }));

            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == HttpStatus.SC_CREATED) {
                onUploadSuccess();
            } else {
                final String message = String.format("Upload failed: %d (%s)",
                        status.getStatusCode(),
                        status.getReasonPhrase());

                Log.w(TAG, message);
                onUploadFailed(new IOException(message));
            }
        } catch (CanceledUploadException e) {
            onUploadCancelled(e);
        } catch (IOException e) {
            onUploadFailed(e);
        }
    }

    protected void onUploadCancelled(CanceledUploadException e) {
        upload.setUploadException(e);
        broadcast(UploadService.UPLOAD_CANCELLED);
    }

    protected void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        upload.setUploadException(e);
        broadcast(UploadService.UPLOAD_ERROR);
    }

    protected void onUploadSuccess() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Upload successful");
        upload.onUploaded();
        broadcast(UploadService.UPLOAD_SUCCESS);
    }

    protected void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, upload));
    }


    public static class CanceledUploadException extends IOException {}
}

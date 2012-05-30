package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
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

public class Uploader extends BroadcastReceiver implements Runnable {
    private SoundCloudApplication app;
    private Recording mUpload;
    private volatile boolean mCanceled;
    private LocalBroadcastManager mBroadcastManager;

    public Uploader(SoundCloudApplication app, Recording recording) {
        this.app = app;
        mUpload = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance(app);
        mBroadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
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
        } finally {
            mBroadcastManager.unregisterReceiver(this);
            mUpload.updateStatus(app.getContentResolver());
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    private void upload() {
        final File toUpload = mUpload.getUploadFile();
        if (toUpload == null || !toUpload.exists()) throw new IllegalArgumentException("File to be uploaded does not exist");
        if (toUpload.length() == 0) throw new IllegalArgumentException("File to be uploaded is empty");

        final FileBody soundBody = new FileBody(toUpload);
        final FileBody artworkBody = mUpload.hasArtwork() ? new FileBody(mUpload.getArtwork()) : null;

        final long totalTransfer = soundBody.getContentLength() + (artworkBody == null ? 0 : artworkBody.getContentLength());

        try {
            if (isCancelled()) throw new UserCanceledException();
            Log.v(TAG, "starting upload of " + toUpload);

            broadcast(UploadService.UPLOAD_STARTED);
            HttpResponse response = app.post(mUpload.getRequest(app, toUpload, new Request.TransferProgressListener() {
                long lastPublished;

                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) throw new UserCanceledException();

                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        final int progress = (int) Math.min(100, (100 * transferred) / totalTransfer);
                        mBroadcastManager.sendBroadcast(new Intent(UploadService.UPLOAD_PROGRESS)
                                .putExtra(UploadService.EXTRA_RECORDING, mUpload)
                                .putExtra(UploadService.EXTRA_TRANSFERRED, transferred)
                                .putExtra(UploadService.EXTRA_PROGRESS, progress)
                                .putExtra(UploadService.EXTRA_TOTAL, totalTransfer));

                        lastPublished = System.currentTimeMillis();
                    }
                }
            }));

            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == HttpStatus.SC_CREATED) {
                onUploadSuccess(response);
            } else {
                final String message = String.format("Upload failed: %d (%s)",
                        status.getStatusCode(),
                        status.getReasonPhrase());

                Log.w(TAG, message);
                onUploadFailed(new IOException(message));
            }
        } catch (UserCanceledException e) {
            onUploadCancelled(e);
        } catch (IOException e) {
            onUploadFailed(e);
        }
    }

    private void onUploadCancelled(UserCanceledException e) {
        mUpload.setUploadException(e);
        broadcast(UploadService.UPLOAD_CANCELLED);
    }

    private void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        mUpload.setUploadException(e);
        broadcast(UploadService.UPLOAD_ERROR);
    }

    private void onUploadSuccess(HttpResponse response) {
        Track track = null;
        try {
            track = app.getMapper().readValue(response.getEntity().getContent(), Track.class);
            mUpload.track_id = track.id;
            SoundCloudDB.insertTrack(app.getContentResolver(), track);
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        //request to update my collection
        LocalCollection.forceToStale(Content.ME_TRACKS.uri, app.getContentResolver());
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Upload successful : " + ( track == null ? "<track_parsing_error>" : track ));

        mUpload.onUploaded();
        broadcast(UploadService.UPLOAD_SUCCESS);
    }

    private void broadcast(String action) {
        mBroadcastManager.sendBroadcast(new Intent(action)
                .putExtra(UploadService.EXTRA_RECORDING, mUpload));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
        if (mUpload.equals(recording)) {
            Log.d(TAG, "canceling upload of "+ mUpload);
            cancel();
        }
    }
}

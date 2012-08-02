package com.soundcloud.android.service.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.AndroidCloudAPI;
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
    private AndroidCloudAPI api;
    private Recording mUpload;
    private volatile boolean mCanceled;
    private LocalBroadcastManager mBroadcastManager;

    private static final int MAX_TRIES = 1;

    public Uploader(AndroidCloudAPI api, Recording recording) {
        this.api = api;
        mUpload = recording;
        mBroadcastManager = LocalBroadcastManager.getInstance(api.getContext());
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
        Log.d(UploadService.TAG, "Uploader.run("+ mUpload+")");

        try {
            upload(0);
        } catch (IllegalArgumentException e) {
            onUploadFailed(e);
        } finally {
            mBroadcastManager.unregisterReceiver(this);
            mUpload.updateStatus(api.getContext().getContentResolver());
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    private boolean upload(int tries) {
        final File toUpload = mUpload.getUploadFile();
        if (toUpload == null || !toUpload.exists()) throw new IllegalArgumentException("File to be uploaded does not exist");
        if (toUpload.length() == 0) throw new IllegalArgumentException("File to be uploaded is empty");

        final FileBody soundBody = new FileBody(toUpload);
        final FileBody artworkBody = mUpload.hasArtwork() ? new FileBody(mUpload.getArtwork()) : null;

        final long totalTransfer = soundBody.getContentLength() + (artworkBody == null ? 0 : artworkBody.getContentLength());

        try {
            if (isCancelled()) throw new UserCanceledException();
            Log.v(TAG, "starting upload of " + toUpload);

            broadcast(UploadService.TRANSFER_STARTED);
            HttpResponse response = api.post(mUpload.getRequest(api.getContext(), toUpload, new Request.TransferProgressListener() {
                long lastPublished;

                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) throw new UserCanceledException();

                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        final int progress = (int) Math.min(100, (100 * transferred) / totalTransfer);
                        mBroadcastManager.sendBroadcast(new Intent(UploadService.TRANSFER_PROGRESS)
                                .putExtra(UploadService.EXTRA_RECORDING, mUpload)
                                .putExtra(UploadService.EXTRA_TRANSFERRED, transferred)
                                .putExtra(UploadService.EXTRA_PROGRESS, progress)
                                .putExtra(UploadService.EXTRA_TOTAL, totalTransfer));

                        lastPublished = System.currentTimeMillis();
                    }
                }
            }));
            StatusLine status = response.getStatusLine();
            switch (status.getStatusCode()) {
                case HttpStatus.SC_CREATED:
                    onUploadSuccess(response);
                    return true;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    // can happen on sandbox - retry once in this case
                    if (tries < MAX_TRIES) return upload(tries + 1);

                    //noinspection fallthrough
                default:
                    final String message = String.format("Upload failed: %d (%s), try=%d",
                            status.getStatusCode(),
                            status.getReasonPhrase(),
                            tries);

                    Log.w(TAG, message);
                    onUploadFailed(new IOException(message));
                    return false;
            }
        } catch (UserCanceledException e) {
            onUploadCancelled(e);
            return false;
        } catch (IOException e) {
            onUploadFailed(e);
            return false;
        }
    }

    private void onUploadCancelled(UserCanceledException e) {
        mUpload.setUploadException(e);
        broadcast(UploadService.TRANSFER_CANCELLED);
    }

    private void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        mUpload.setUploadException(e);
        broadcast(UploadService.TRANSFER_ERROR);
    }

    private void onUploadSuccess(HttpResponse response) {
        try {
            Track track = api.getMapper().readValue(response.getEntity().getContent(), Track.class);
            mUpload.track_id = track.id;
            SoundCloudDB.insertTrack(api.getContext().getContentResolver(), track);

            //request to update my collection
            LocalCollection.forceToStale(Content.ME_TRACKS.uri, api.getContext().getContentResolver());
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Upload successful : " + track);

            mUpload.onUploaded();
            broadcast(UploadService.TRANSFER_SUCCESS);
        } catch (IOException e) {
            onUploadFailed(e);
        }
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

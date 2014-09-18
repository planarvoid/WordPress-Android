package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.FileBody;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class Uploader extends BroadcastReceiver implements Runnable {
    private final SoundAssociationStorage soundAssociationStorage = new SoundAssociationStorage();
    private final TrackStorage trackStorage = new TrackStorage();
    private final RecordingStorage recordingStorage = new RecordingStorage();
    private final SyncStateManager syncStateManager;

    private final PublicCloudAPI api;
    private final Recording upload;
    private volatile boolean canceled;
    private final LocalBroadcastManager broadcastManager;
    private final Resources resources;

    private static final int MAX_TRIES = 1;

    public Uploader(Context context, PublicCloudAPI api, Recording recording) {
        this.api = api;
        upload = recording;
        broadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastManager.registerReceiver(this, new IntentFilter(UploadService.UPLOAD_CANCEL));
        resources = context.getResources();
        syncStateManager = new SyncStateManager(context);
    }

    public boolean isCancelled() {
        return canceled;
    }

    public void cancel() {
        canceled = true;
    }

    @Override
    public void run() {
        Log.d(UploadService.TAG, "Uploader.run(" + upload + ")");

        try {
            upload(0);
        } catch (IllegalArgumentException e) {
            onUploadFailed(e);
        } finally {
            broadcastManager.unregisterReceiver(this);
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    private boolean upload(int tries) {
        final File toUpload = upload.getUploadFile();
        if (toUpload == null || !toUpload.exists()) {
            throw new IllegalArgumentException("File to be uploaded does not exist");
        }
        if (toUpload.length() == 0) {
            throw new IllegalArgumentException("File to be uploaded is empty");
        }

        final FileBody soundBody = new FileBody(toUpload);
        final FileBody artworkBody = upload.hasArtwork() ? new FileBody(upload.getArtwork()) : null;

        final long totalTransfer = soundBody.getContentLength() + (artworkBody == null ? 0 : artworkBody.getContentLength());

        try {
            if (isCancelled()) {
                throw new UserCanceledException();
            }
            Log.v(TAG, "starting upload of " + toUpload);

            broadcast(UploadService.TRANSFER_STARTED);
            HttpResponse response = api.post(upload.getRequest(resources, toUpload, new Request.TransferProgressListener() {
                long lastPublished;

                @Override
                public void transferred(long transferred) throws IOException {
                    if (isCancelled()) {
                        throw new UserCanceledException();
                    }

                    if (System.currentTimeMillis() - lastPublished > 1000) {
                        final int progress = (int) Math.min(100, (100 * transferred) / totalTransfer);
                        broadcastManager.sendBroadcast(new Intent(UploadService.TRANSFER_PROGRESS)
                                .putExtra(UploadService.EXTRA_RECORDING, upload)
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
                    if (tries < MAX_TRIES) {
                        return upload(tries + 1);
                    }

                    //noinspection fallthrough
                default:
                    final String message = String.format(Locale.US, "Upload failed: %d (%s), try=%d",
                            status.getStatusCode(),
                            status.getReasonPhrase(),
                            tries);

                    Log.w(TAG, message);
                    onUploadFailed(new IOException(message));
                    return false;
            }
        } catch (UserCanceledException e) {
            onUploadCancelled();
            return false;
        } catch (IOException e) {
            onUploadFailed(e);
            return false;
        }
    }

    private void onUploadCancelled() {
        broadcast(UploadService.TRANSFER_CANCELLED);
    }

    private void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        broadcast(UploadService.TRANSFER_ERROR);
    }

    private void onUploadSuccess(HttpResponse response) {
        try {
            PublicApiTrack track = api.getMapper().readValue(response.getEntity().getContent(), PublicApiTrack.class);
            trackStorage.createOrUpdate(track);
            soundAssociationStorage.addCreation(track);

            //request to update my collection
            syncStateManager.forceToStale(Content.ME_SOUNDS);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Upload successful : " + track);
            }

            upload.markUploaded();
            if (!upload.external_upload) {
                IOUtils.deleteFile(upload.getFile());
                IOUtils.deleteFile(upload.getEncodedFile());
            }
            File artworkPath = upload.resized_artwork_path;
            if (artworkPath != null) {
                IOUtils.deleteFile(artworkPath);
            }

            recordingStorage.updateStatus(upload);

            broadcast(UploadService.TRANSFER_SUCCESS, track);
        } catch (IOException e) {
            onUploadFailed(e);
        }
    }

    private void broadcast(String action, PublicApiTrack... track) {
        final Intent intent = new Intent(action).putExtra(UploadService.EXTRA_RECORDING, upload);
        if (track.length > 0) {
            intent.putExtra(PublicApiTrack.EXTRA, track[0]);
        }
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
        if (upload.equals(recording)) {
            Log.d(TAG, "canceling upload of " + upload);
            cancel();
        }
    }
}

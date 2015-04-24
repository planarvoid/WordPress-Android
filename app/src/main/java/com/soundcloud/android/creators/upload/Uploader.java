package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.FilePart;
import com.soundcloud.android.api.StringPart;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.posts.PostProperty;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Params;
import com.soundcloud.propeller.PropertySet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;

public class Uploader extends BroadcastReceiver implements Runnable {

    private static final String BLOB_MEDIA_TYPE = "application/octet-stream";

    private final TrackStorage trackStorage = new TrackStorage();
    private final StorePostsCommand storePostsCommand;

    private final RecordingStorage recordingStorage = new RecordingStorage();
    private final SyncStateManager syncStateManager;

    private final ApiClient apiClient;
    private final Recording recording;
    private volatile boolean canceled;
    private final LocalBroadcastManager broadcastManager;
    private final Resources resources;

    public Uploader(Context context, ApiClient apiClient, Recording recording, StorePostsCommand storePostsCommand) {
        this.apiClient = apiClient;
        this.recording = recording;
        this.storePostsCommand = storePostsCommand;
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
        Log.d(UploadService.TAG, "Uploader.run(" + recording + ")");

        try {
            upload();
        } catch (IllegalArgumentException e) {
            onUploadFailed(e);
        } finally {
            broadcastManager.unregisterReceiver(this);
        }
    }

    private void upload() {
        final File recordingFile = recording.getUploadFile();
        if (recordingFile == null || !recordingFile.exists()) {
            throw new IllegalArgumentException("File to be uploaded does not exist");
        }
        if (recordingFile.length() == 0) {
            throw new IllegalArgumentException("File to be uploaded is empty");
        }

        try {
            if (isCancelled()) {
                throw new UserCanceledException();
            }
            Log.v(TAG, "starting upload of " + recordingFile);

            broadcast(UploadService.TRANSFER_STARTED);

            final ApiRequest request = buildUploadRequest(resources, recording);
            onUploadFinished(apiClient.fetchMappedResponse(request, PublicApiTrack.class));

        } catch (UserCanceledException e) {
            onUploadCancelled();
        } catch (IOException | ApiMapperException | ApiRequestException e) {
            onUploadFailed(e);
        }
    }

    private ApiRequest buildUploadRequest(Resources resources, Recording recording) {
        final ApiRequest.Builder request = ApiRequest.post(ApiEndpoints.LEGACY_TRACKS.path()).forPublicApi();

        final Map<String, ?> map = recording.toParamsMap(resources);
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Iterable) {
                for (Object o : (Iterable) entry.getValue()) {
                    request.withFormPart(new StringPart(entry.getKey(), o.toString()));
                }
            } else {
                request.withFormPart(new StringPart(entry.getKey(), entry.getValue().toString()));
            }
        }
        final File recordingFile = recording.getUploadFile();
        final String fileName;
        if (!recording.external_upload) {
            String title = map.get(Params.Track.TITLE).toString();
            final String newTitle = title == null ? "unknown" : title;
            fileName = String.format("%s.%s", URLEncoder.encode(newTitle.replace(" ", "_")), VorbisReader.EXTENSION);
        } else {
            fileName = recordingFile.getName();
        }
        request.withFormPart(new FilePart(recordingFile, fileName, Params.Track.ASSET_DATA, BLOB_MEDIA_TYPE));
        if (recording.artwork_path != null) {
            request.withFormPart(new FilePart(recording.artwork_path, Params.Track.ARTWORK_DATA, BLOB_MEDIA_TYPE));
        }

        return request.build();
    }

    private void onUploadCancelled() {
        broadcast(UploadService.TRANSFER_CANCELLED);
    }

    private void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        broadcast(UploadService.TRANSFER_ERROR);
    }

    private void onUploadFinished(PublicApiTrack track) {
        trackStorage.createOrUpdate(track);
        createNewTrackPost(track);

        //request to update my collection
        syncStateManager.forceToStale(Content.ME_SOUNDS);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Upload successful : " + track);
        }

        recording.markUploaded();
        if (!recording.external_upload) {
            IOUtils.deleteFile(recording.getFile());
            IOUtils.deleteFile(recording.getEncodedFile());
        }
        File artworkPath = recording.resized_artwork_path;
        if (artworkPath != null) {
            IOUtils.deleteFile(artworkPath);
        }

        recordingStorage.updateStatus(recording);

        broadcast(UploadService.TRANSFER_SUCCESS, track);
    }

    private void createNewTrackPost(PublicApiTrack track) {
        storePostsCommand.with(Arrays.asList(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(track.getUrn()),
                        PostProperty.CREATED_AT.bind(track.getCreatedAt()),
                        PostProperty.IS_REPOST.bind(false)
                )
        )).call();
    }

    private void broadcast(String action, PublicApiTrack... track) {
        final Intent intent = new Intent(action).putExtra(UploadService.EXTRA_RECORDING, recording);
        if (track.length > 0) {
            intent.putExtra(PublicApiTrack.EXTRA, track[0]);
        }
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Recording recording = intent.getParcelableExtra(UploadService.EXTRA_RECORDING);
        if (this.recording.equals(recording)) {
            Log.d(TAG, "canceling upload of " + this.recording);
            cancel();
        }
    }
}

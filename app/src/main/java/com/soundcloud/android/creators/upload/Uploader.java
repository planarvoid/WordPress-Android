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
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Uploader extends BroadcastReceiver implements Runnable {

    static final String PARAM_TITLE = "track[title]";          // required
    static final String PARAM_TYPE = "track[track_type]";
    static final String PARAM_DESCRIPTION = "track[description]";
    static final String PARAM_POST_TO = "track[post_to][][id]";
    static final String PARAM_POST_TO_EMPTY = "track[post_to][]";
    static final String PARAM_TAG_LIST = "track[tag_list]";
    static final String PARAM_SHARING = "track[sharing]";
    static final String PARAM_STREAMABLE = "track[streamable]";
    static final String PARAM_DOWNLOADABLE = "track[downloadable]";
    static final String PARAM_GENRE = "track[genre]";
    static final String PARAM_SHARED_EMAILS = "track[shared_to][emails][][address]";
    static final String PARAM_SHARED_IDS = "track[shared_to][users][][id]";
    static final String PARAM_SHARING_NOTE = "track[sharing_note]";
    static final String PARAM_ASSET_DATA = "track[asset_data]";
    static final String PARAM_ARTWORK_DATA = "track[artwork_data]";

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

    private ApiRequest buildUploadRequest(Resources resources, final Recording recording) {
        final ApiRequest.Builder request = ApiRequest.post(ApiEndpoints.LEGACY_TRACKS.path()).forPublicApi();

        final Map<String, ?> params = buildRecordingParamMap(resources, recording);
        addRecordingFields(request, params);

        final File recordingFile = recording.getUploadFile();
        final String fileName;
        if (!recording.external_upload) {
            String title = params.get(PARAM_TITLE).toString();
            final String newTitle = title == null ? "unknown" : title;
            fileName = String.format("%s.%s", URLEncoder.encode(newTitle.replace(" ", "_")), VorbisReader.EXTENSION);
        } else {
            fileName = recordingFile.getName();
        }
        request.withFormPart(FilePart.from(PARAM_ASSET_DATA, recordingFile, fileName, FilePart.BLOB_MEDIA_TYPE));
        if (recording.artwork_path != null) {
            request.withFormPart(FilePart.from(PARAM_ARTWORK_DATA, recording.artwork_path, FilePart.BLOB_MEDIA_TYPE));
        }
        request.withProgressListener(new UploadProgressListener(recording));

        return request.build();
    }

    private Map<String, ?> buildRecordingParamMap(Resources resources, Recording recording) {
        Map<String, Object> data = new HashMap<>();
        recording.title = recording.sharingNote(resources);

        data.put(PARAM_TITLE, recording.title);
        data.put(PARAM_TYPE, "recording");
        data.put(PARAM_SHARING, recording.isPublic() ? Params.Track.PUBLIC : Params.Track.PRIVATE);
        data.put(PARAM_DOWNLOADABLE, false);
        data.put(PARAM_STREAMABLE, true);

        final String tagString = recording.tagString();
        if (!TextUtils.isEmpty(tagString)) {
            data.put(PARAM_TAG_LIST, tagString);
        }
        if (!TextUtils.isEmpty(recording.description)) {
            data.put(PARAM_DESCRIPTION, recording.description);
        }
        if (!TextUtils.isEmpty(recording.genre)) {
            data.put(PARAM_GENRE, recording.genre);
        }

        if (!TextUtils.isEmpty(recording.service_ids)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, recording.service_ids.split(","));
            data.put(PARAM_POST_TO, ids);
            data.put(PARAM_SHARING_NOTE, recording.title);
        } else {
            data.put(PARAM_POST_TO_EMPTY, "");
        }

        if (!TextUtils.isEmpty(recording.shared_emails)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, recording.shared_emails.split(","));
            data.put(PARAM_SHARED_EMAILS, ids);
        }

        if (recording.recipient_user_id > 0) {
            data.put(PARAM_SHARED_IDS, recording.recipient_user_id);
        } else if (!TextUtils.isEmpty(recording.shared_ids)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, recording.shared_ids.split(","));
            data.put(PARAM_SHARED_IDS, ids);
        }
        return data;
    }


    private void addRecordingFields(ApiRequest.Builder request, Map<String, ?> map) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Iterable) {
                for (Object o : (Iterable) entry.getValue()) {
                    request.withFormPart(StringPart.from(entry.getKey(), o.toString()));
                }
            } else {
                request.withFormPart(StringPart.from(entry.getKey(), entry.getValue().toString()));
            }
        }
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

    private class UploadProgressListener implements ApiRequest.ProgressListener {
        private final Recording recording;
        private long lastPublished;

        public UploadProgressListener(Recording recording) {
            this.recording = recording;
        }

        @Override
        public void update(long bytesWritten, long totalBytes) {
            if (System.currentTimeMillis() - lastPublished > 1000) {
                final int progress = (int) Math.min(100, (100 * bytesWritten) / totalBytes);
                broadcastManager.sendBroadcast(new Intent(UploadService.TRANSFER_PROGRESS)
                        .putExtra(UploadService.EXTRA_RECORDING, recording)
                        .putExtra(UploadService.EXTRA_TRANSFERRED, bytesWritten)
                        .putExtra(UploadService.EXTRA_PROGRESS, progress)
                        .putExtra(UploadService.EXTRA_TOTAL, totalBytes));

                lastPublished = System.currentTimeMillis();
            }
        }
    }
}

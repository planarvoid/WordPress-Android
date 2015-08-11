package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.FilePart;
import com.soundcloud.android.api.StringPart;
import com.soundcloud.android.api.legacy.Params;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.java.collections.PropertySet;
import rx.Subscription;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Uploader implements Runnable {
    static final String PARAM_TITLE = "track[title]";          // required
    static final String PARAM_TYPE = "track[track_type]";
    static final String PARAM_DESCRIPTION = "track[description]";
    static final String PARAM_TAG_LIST = "track[tag_list]";
    static final String PARAM_SHARING = "track[sharing]";
    static final String PARAM_STREAMABLE = "track[streamable]";
    static final String PARAM_DOWNLOADABLE = "track[downloadable]";
    static final String PARAM_GENRE = "track[genre]";
    static final String PARAM_ASSET_DATA = "track[asset_data]";
    static final String PARAM_ARTWORK_DATA = "track[artwork_data]";

    private final TrackStorage trackStorage = new TrackStorage();
    private final StorePostsCommand storePostsCommand;
    private final SyncStateManager syncStateManager;

    private final ApiClient apiClient;
    private final Recording recording;
    private volatile boolean canceled;
    private final Resources resources;
    private final Subscription subscription;

    private final EventBus eventBus;

    public Uploader(Context context, ApiClient apiClient, Recording recording, StorePostsCommand storePostsCommand, EventBus eventBus) {
        this.apiClient = apiClient;
        this.recording = recording;
        this.storePostsCommand = storePostsCommand;
        this.resources = context.getResources();
        this.syncStateManager = new SyncStateManager(context);
        this.subscription = eventBus.subscribe(EventQueue.UPLOAD, new EventSubscriber());
        this.eventBus = eventBus;
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
            subscription.unsubscribe();
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

            eventBus.publish(EventQueue.UPLOAD, UploadEvent.transferStarted(recording));

            final ApiRequest request = buildUploadRequest(resources, recording);
            onUploadFinished(apiClient.fetchMappedResponse(request, PublicApiTrack.class));
        } catch (IOException | ApiMapperException | ApiRequestException e) {
            if (!isCancelled()) {
                onUploadFailed(e);
            }
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

    private void onUploadFailed(Exception e) {
        Log.e(TAG, "Error uploading", e);
        eventBus.publish(EventQueue.UPLOAD, UploadEvent.error(recording));
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

        eventBus.publish(EventQueue.UPLOAD, UploadEvent.transferSuccess(recording, track));
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

    private final class EventSubscriber extends DefaultSubscriber<UploadEvent> {
        @Override
        public void onNext(UploadEvent uploadEvent) {
            if (uploadEvent.isCancelled()) {
                if (recording.getId() == uploadEvent.getRecording().getId()) {
                    Log.d(TAG, "canceling upload of " + recording);
                    cancel();
                }
            }
        }
    }

    private class UploadProgressListener implements ApiRequest.ProgressListener {
        private final Recording recording;
        private long lastPublished;

        public UploadProgressListener(Recording recording) {
            this.recording = recording;
        }

        @Override
        public void update(long bytesWritten, long totalBytes) throws IOException {
            if (isCancelled()) {
                throw new UserCanceledException();
            }

            if (System.currentTimeMillis() - lastPublished > 500) {
                final int progress = (int) Math.min(100, (100 * bytesWritten) / totalBytes);
                eventBus.publish(EventQueue.UPLOAD, UploadEvent.transferProgress(recording, progress));
                lastPublished = System.currentTimeMillis();
            }
        }
    }
}

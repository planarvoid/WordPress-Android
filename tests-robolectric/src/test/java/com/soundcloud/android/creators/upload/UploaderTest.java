package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.FilePart;
import com.soundcloud.android.api.StringPart;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
@RunWith(SoundCloudTestRunner.class)
public class UploaderTest {
    List<Intent> intents = new ArrayList<>();
    List<String> actions = new ArrayList<>();

    @Mock private ApiClient apiClient;

    @Before
    public void before() {
        LocalBroadcastManager.getInstance(Robolectric.application).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intents.add(intent);
                actions.add(intent.getAction());
            }
        }, UploadService.getIntentFilter());
    }

    private Uploader uploader(Recording r) {
        return new Uploader(Robolectric.application, apiClient, r, mock(StorePostsCommand.class));
    }

    @Test
    public void shouldErrorWhenFileIsMissing() throws Exception {
        Recording upload = new Recording(new File("/boom/"));
        uploader(upload).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_ERROR);
    }

    @Test
    public void shouldThrowWhenFileIsEmpty() throws Exception {
        uploader(Recording.create(null)).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_ERROR);
    }

    @Test
    public void shouldSetSuccessAfterFileUpload() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenReturn(ModelFixtures.create(PublicApiTrack.class));

        final Recording recording = RecordingTestHelper.getValidRecording();
        uploader(recording).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_STARTED, UploadService.TRANSFER_SUCCESS);
        expect(recording.isUploaded()).toBeTrue();
    }

    @Test
    public void shouldSendMultipartParametersFromRecording() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(PublicApiTrack.class)))
                .thenReturn(ModelFixtures.create(PublicApiTrack.class));

        final Recording recording = RecordingTestHelper.getValidRecording();
        uploader(recording).run();

        final File transcodedFile = new File(recording.audio_path.getAbsolutePath().replaceAll(".wav", ".ogg"));
        verify(apiClient).fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())
                        .withFormParts(
                                StringPart.from(Uploader.PARAM_TITLE, recording.title),
                                StringPart.from(Uploader.PARAM_TYPE, "recording"),
                                StringPart.from(Uploader.PARAM_STREAMABLE, "true"),
                                StringPart.from(Uploader.PARAM_SHARING, "public"),
                                StringPart.from(Uploader.PARAM_TAG_LIST, "soundcloud:source=android-record"),
                                StringPart.from(Uploader.PARAM_DOWNLOADABLE, "false"),
                                StringPart.from(Uploader.PARAM_POST_TO_EMPTY, ""),
                                FilePart.from(Uploader.PARAM_ASSET_DATA, transcodedFile,
                                        recording.title.replaceAll(" ", "_") + ".ogg",
                                        FilePart.BLOB_MEDIA_TYPE)
                        )),
                eq(PublicApiTrack.class));
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUpload() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenThrow(ApiRequestException.unexpectedResponse(null, 499));
        final Recording recording = RecordingTestHelper.getValidRecording();
        uploader(recording).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_STARTED, UploadService.TRANSFER_ERROR);
        expect(recording.isUploaded()).toBeFalse();
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUploadIOException() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenThrow(new IOException("network error"));
        final Recording recording = RecordingTestHelper.getValidRecording();
        uploader(recording).run();
        expect(recording.isUploaded()).toBeFalse();
    }

    @Test
    public void shouldNotSetSuccessIfTaskCanceled() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenReturn(ModelFixtures.create(PublicApiTrack.class));
        final Recording recording = RecordingTestHelper.getValidRecording();
        final Uploader uploader = uploader(recording);
        uploader.cancel();
        uploader.run();
        expect(recording.isUploaded()).toBeFalse();
    }
}

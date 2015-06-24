package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.SoundCloudMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
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
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UploadEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.List;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
@RunWith(SoundCloudTestRunner.class)
public class UploaderTest {
    private Recording recording;
    private TestEventBus eventBus = new TestEventBus();
    private Context context = Robolectric.application.getApplicationContext();


    @Mock private ApiClient apiClient;

    @Mock StorePostsCommand storePostsCommand;

    @Before
    public void before() throws Exception {
        recording = RecordingTestHelper.getValidRecording();
    }

    private Uploader uploader(Recording recording) {
        return new Uploader(context, apiClient, recording, storePostsCommand, eventBus);
    }

    @Test
    public void shouldErrorWhenFileIsMissing() throws Exception {
        Recording upload = new Recording(new File("/boom/"));

        uploader(upload).run();

        expect(eventBus.lastEventOn(EventQueue.UPLOAD)).toEqual(
                UploadEvent.error(upload));
    }

    @Test
    public void shouldThrowWhenFileIsEmpty() throws Exception {
        Recording upload = Recording.create();

        uploader(upload).run();

        expect(eventBus.lastEventOn(EventQueue.UPLOAD)).toEqual(
                UploadEvent.error(upload));
    }

    @Test
    public void shouldSetSuccessAfterFileUpload() throws Exception {
        PublicApiTrack apiTrack = ModelFixtures.create(PublicApiTrack.class);
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenReturn(apiTrack);

        uploader(recording).run();
        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);
        PublicApiTrack track = eventBus.lastEventOn(EventQueue.UPLOAD).getTrack();

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.transferStarted(recording),
                UploadEvent.transferSuccess(recording, track));

        expect(recording.isUploaded()).toBeTrue();
        expect(track.getUrn()).toEqual(apiTrack.getUrn());
    }

    @Test
    public void shouldSendMultipartParametersFromRecording() throws Exception {
        when(apiClient.fetchMappedResponse(any(ApiRequest.class), eq(PublicApiTrack.class)))
                .thenReturn(ModelFixtures.create(PublicApiTrack.class));

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

        uploader(recording).run();
        
        List<UploadEvent> events = eventBus.eventsOn(EventQueue.UPLOAD);

        expect(events).toNumber(3);
        expect(events).toContainExactly(
                UploadEvent.idle(),
                UploadEvent.transferStarted(recording),
                UploadEvent.error(recording));

        expect(recording.isUploaded()).toBeFalse();
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUploadIOException() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenThrow(new IOException("network error"));
        uploader(recording).run();
        expect(recording.isUploaded()).toBeFalse();
    }

    @Test
    public void shouldNotSetSuccessIfTaskCanceled() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("POST", ApiEndpoints.LEGACY_TRACKS.path())), eq(PublicApiTrack.class)))
                .thenReturn(ModelFixtures.create(PublicApiTrack.class));
        final Uploader uploader = uploader(recording);
        uploader.cancel();
        uploader.run();
        expect(recording.isUploaded()).toBeFalse();
    }
}

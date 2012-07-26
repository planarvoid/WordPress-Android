package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.utils.IOUtils.readInputStream;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
@RunWith(DefaultTestRunner.class)
public class UploaderTest {
    List<Intent> intents = new ArrayList<Intent>();
    List<String> actions = new ArrayList<String>();

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
        return new Uploader(DefaultTestRunner.application, r);
    }

    public void shouldErrorWhenFileIsMissing() throws Exception {
        Recording upload = new Recording(new File("/boom/"));
        uploader(upload).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_ERROR);
    }

    public void shouldThrowWhenFileIsEmpty() throws Exception {
        uploader(Recording.create()).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_ERROR);
    }

    @Test
    public void shouldSetSuccessAfterFileUpload() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(HttpStatus.SC_CREATED,
                readInputStream(getClass().getResourceAsStream("upload_response.json"))));
        Robolectric.addHttpResponseRule("GET", "/tracks/47204307", new TestHttpResponse(HttpStatus.SC_OK,
                        readInputStream(getClass().getResourceAsStream("track_finished.json"))));

        final Recording recording = TestApplication.getValidRecording();
        uploader(recording).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_STARTED, UploadService.TRANSFER_SUCCESS);
        expect(recording.isUploaded()).toBeTrue();
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUpload() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "Failz"));
        final Recording recording = TestApplication.getValidRecording();
        uploader(recording).run();
        expect(actions).toContainExactly(UploadService.TRANSFER_STARTED, UploadService.TRANSFER_ERROR);
        expect(recording.getUploadException()).not.toBeNull();
        expect(recording.isCanceled()).toBeFalse();
        expect(recording.isUploaded()).toBeFalse();
        expect(recording.getUploadException().getMessage()).toEqual("Upload failed: 503 (HTTP status 503), try=0");
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUploadIOException() throws Exception {
        TestHelper.addPendingIOException("/tracks");
        final Recording recording = TestApplication.getValidRecording();
        uploader(recording).run();
        expect(recording.getUploadException()).not.toBeNull();
        expect(recording.isUploaded()).toBeFalse();
        expect(recording.isCanceled()).toBeFalse();
        expect(recording.getUploadException().getMessage()).toEqual("boom");
    }

    @Test
    public void shouldNotSetSuccessIfTaskCanceled() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201,
                readInputStream(getClass().getResourceAsStream("upload_response.json"))));
        final Recording recording = TestApplication.getValidRecording();
        final Uploader uploader = uploader(recording);
        uploader.cancel();
        uploader.run();
        expect(recording.getUploadException() instanceof UserCanceledException).toBeTrue();
        expect(recording.isCanceled()).toBeTrue();
        expect(recording.isUploaded()).toBeFalse();
    }

    @Test
    public void shouldRetryOnceIfServerErrorIsReturned() throws Exception {
        Robolectric.addPendingHttpResponse(new TestHttpResponse(500, "Failz"));
        Robolectric.addPendingHttpResponse(new TestHttpResponse(201,
                readInputStream(getClass().getResourceAsStream("upload_response.json"))));

        Robolectric.addHttpResponseRule("GET", "/tracks/47204307", new TestHttpResponse(HttpStatus.SC_OK,
                readInputStream(getClass().getResourceAsStream("track_finished.json"))));

        final Recording recording = TestApplication.getValidRecording();
        uploader(recording).run();
        expect(actions).toContainExactly(
                UploadService.TRANSFER_STARTED,
                UploadService.TRANSFER_STARTED,
                UploadService.TRANSFER_SUCCESS);

        expect(recording.isUploaded()).toBeTrue();
    }
}

package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(DefaultTestRunner.class)
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class UploadTaskTest {
    UploadTask task;

    @Before
    public void setup() {
        task = new UploadTask(DefaultTestRunner.application);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenFileIsMissing() throws Exception {
        Upload upload = new Upload(new Recording(new File("/boom/")), Robolectric.application.getResources());
        task.doInBackground(upload);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenFileIsEmpty() throws Exception {
        File tmp = File.createTempFile("temp", ".ogg");
        Upload upload = new Upload(new Recording(tmp), Robolectric.application.getResources());
        task.doInBackground(upload);
    }

    @Test
    public void shouldSetSuccessAfterFileUpload() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        Upload upload = TestApplication.getValidUpload();
        expect(task.doInBackground(upload).isSuccess()).toBeTrue();
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUpload() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "Failz"));
        Upload upload = TestApplication.getValidUpload();
        expect(task.doInBackground(upload).isSuccess()).toBeFalse();
        expect(upload.getUploadException()).not.toBeNull();
        expect(upload.getUploadException().getMessage()).toEqual("Upload failed: 503 (HTTP status 503)");
    }

    @Test
    public void shouldNotSetSuccessAfterFailedUploadIOException() throws Exception {
        TestHelper.addPendingIOException("/tracks");
        Upload upload = TestApplication.getValidUpload();
        expect(task.doInBackground(upload).isSuccess()).toBeFalse();
        expect(upload.getUploadException()).not.toBeNull();
        expect(upload.getUploadException().getMessage()).toEqual("boom");
    }

    @Test
    public void shouldNotSetSuccessIfTaskCanceled() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        Upload upload = TestApplication.getValidUpload();
        task.cancel(true);
        task.execute(upload);
        expect(task.doInBackground(upload).isSuccess()).toBeFalse();
        expect(upload.getUploadException() instanceof UploadTask.CanceledUploadException).toBeTrue();
    }
}

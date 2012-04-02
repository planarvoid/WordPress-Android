package com.soundcloud.android.service.record;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.task.UploadTaskTest;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class CloudCreateServiceTest {
    CloudCreateService svc;

    @Before
    public void before() throws Exception {
        svc = new CloudCreateService();
        svc.onCreate();
    }

    @Test
    public void shouldNotifyAboutUploadSuccess() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        final Upload upload = TestApplication.getValidUpload();
        upload.title = "testing";
        svc.startUpload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification.tickerText).toEqual("Upload Finished");
        expect(shadowOf(notification).getLatestEventInfo().getContentText()).toEqual("testing has been uploaded");
        expect(shadowOf(notification).getLatestEventInfo().getContentTitle()).toEqual("Upload Finished");
    }

    @Test
    public void shouldNotifyAboutUploadFailure() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "ohnoez"));
        final Upload upload = TestApplication.getValidUpload();
        upload.title = "testing";
        svc.startUpload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification.tickerText).toEqual("Upload Error");
        expect(shadowOf(notification).getLatestEventInfo().getContentText()).toEqual("There was an error uploading testing");
        expect(shadowOf(notification).getLatestEventInfo().getContentTitle()).toEqual("Upload Error");
    }

    @Test
    public void shouldUpdateRecordingEntryDuringUploadAndAfterSuccess() throws Exception {
        Recording recording = SoundCloudDB.insertRecording(svc.getContentResolver(), TestApplication.getValidRecording());

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        Robolectric.getBackgroundScheduler().pause();
        svc.startUpload(new Upload(recording, Robolectric.application.getResources()));

        Cursor c = svc.getContentResolver().query(recording.toUri(), null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.moveToFirst()).toBeTrue();

        Recording updated = new Recording(c);
        expect(updated.upload_status).toEqual(Upload.Status.UPLOADING);

        Robolectric.getBackgroundScheduler().unPause();

        c = svc.getContentResolver().query(recording.toUri(), null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.moveToFirst()).toBeTrue();

        updated = new Recording(c);
        expect(updated.upload_status).toEqual(Upload.Status.UPLOADED);
    }


    @Test
    public void shouldUpdateRecordingEntryAfterFailure() throws Exception {
        Recording recording = SoundCloudDB.insertRecording(svc.getContentResolver(), TestApplication.getValidRecording());

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(401, "ERROR"));
        svc.startUpload(new Upload(recording, Robolectric.application.getResources()));

        Cursor c = svc.getContentResolver().query(recording.toUri(), null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.moveToFirst()).toBeTrue();

        Recording updated = new Recording(c);
        expect(updated.upload_status).toEqual(Upload.Status.NOT_YET_UPLOADED);
    }

    @Test
    public void shouldResizeArtworkIfSpecified() throws Exception {
        // cannot test this - just to execute code path
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        final Upload upload = TestApplication.getValidUpload();
        upload.artworkFile = File.createTempFile("some_artwork", ".png");
        svc.startUpload(upload);
    }
}

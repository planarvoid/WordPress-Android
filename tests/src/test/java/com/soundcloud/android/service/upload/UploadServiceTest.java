package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.LocalBinder;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import com.xtremelabs.robolectric.util.Scheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.io.File;

@RunWith(DefaultTestRunner.class)
public class UploadServiceTest {
    UploadService svc;

    @Before
    public void before() throws Exception {
        svc = new UploadService();
        svc.onCreate();
    }

    private Scheduler getServiceScheduler() {
        return Robolectric.shadowOf(svc.getServiceHandler().getLooper()).getScheduler();
    }

    private Scheduler getUploadScheduler() {
        return Robolectric.shadowOf(svc.getUploadHandler().getLooper()).getScheduler();
    }

    private Scheduler getMainScheduler() {
        return Robolectric.shadowOf(Robolectric.application.getMainLooper()).getScheduler();
    }

    @Test
    public void shouldUseLocalService() throws Exception {
        expect(svc.onBind(null) instanceof LocalBinder).toBeTrue();
    }


    @Test
    public void shouldNotifyAboutUploadSuccess() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.onUpload(upload);

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
        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.onUpload(upload);

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
        Recording recording = TestApplication.getValidRecording();

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        getUploadScheduler().pause();

        svc.onUpload(recording);

        Recording updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), recording.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.UPLOADING);

        getUploadScheduler().unPause();

        updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), recording.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.UPLOADED);
    }

    @Test
    public void shouldUpdateRecordingEntryAfterFailure() throws Exception {
        Recording recording = TestApplication.getValidRecording();

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(401, "ERROR"));

        svc.onUpload(recording);

        Recording updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), recording.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.ERROR);
    }

    @Test
    public void shouldResizeArtworkIfSpecified() throws Exception {
        // cannot test this - just to execute code path
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));
        final Recording upload = TestApplication.getValidRecording();
        upload.artwork_path = File.createTempFile("some_artwork", ".png");

        svc.onUpload(upload);

        expect(upload.isUploaded()).toBeTrue();
        expect(upload.resized_artwork_path).toEqual(upload.artwork_path);

        Recording updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), upload.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.UPLOADED);
    }

    @Test
    public void shouldHoldWifiAndWakelockDuringUpload() throws Exception {
        Recording recording = TestApplication.getValidRecording();

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201, "Created"));

        getServiceScheduler().pause();
        getMainScheduler().pause();

        svc.onUpload(recording);

        expect(svc.getWifiLock().isHeld()).toBeFalse();
        expect(svc.getWakeLock().isHeld()).toBeFalse();

        getServiceScheduler().runOneTask();
        getMainScheduler().runOneTask();

        expect(svc.getWifiLock().isHeld()).toBeFalse();
        expect(svc.getWakeLock().isHeld()).toBeFalse();
        // TODO needs BroadcastManager w/ step execution of queued runnables
    }

    @Test
    public void cancelUploadShouldRemoveAllMessagesFromTheQueue() throws Exception {
        Recording recording = TestApplication.getValidRecording();

        getUploadScheduler().pause();
        svc.onUpload(recording);
        expect(svc.getUploadHandler().hasMessages(0)).toBeTrue();

        svc.onCancel(recording);
        expect(svc.getUploadHandler().hasMessages(0)).toBeFalse();

        getUploadScheduler().unPause();
    }

    @Test
    public void shouldRespectLifecycle() throws Exception {
        svc.onDestroy();
    }
}

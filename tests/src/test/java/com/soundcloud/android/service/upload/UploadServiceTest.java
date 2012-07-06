package com.soundcloud.android.service.upload;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
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
import android.content.Intent;

import java.io.File;
import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class UploadServiceTest {
    UploadService svc;

    @Before
    public void before() throws Exception {
        svc = startService();
    }

    private UploadService startService() {
        UploadService service = new UploadService();
        service.onCreate();
        return service;
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
    public void shouldStopServiceAfterLastUploadCompletes() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "Error");

        getUploadScheduler().pause();
        // 2 uploads queued
        final Recording r1 = TestApplication.getValidRecording();
        final Recording r2 = TestApplication.getValidRecording();
        svc.upload(r1);
        svc.upload(r2);

        getUploadScheduler().runOneTask(); // 2 messages on
        getUploadScheduler().runOneTask(); // service queue

        getUploadScheduler().runOneTask(); // first track upload
        expect(shadowOf(svc).isStoppedBySelf()).toBeFalse();
        getUploadScheduler().runOneTask(); // second track upload

        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();

        expect(r1.upload_status).toEqual(Recording.Status.ERROR);
        expect(r2.upload_status).toEqual(Recording.Status.ERROR);
    }

    @Test
    public void shouldNotifyAboutUploadSuccess() throws Exception {
        mockSuccessfullTrackCreation();

        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Finished");
        expect(notification).toHaveText("testing has been uploaded");
        expect(notification).toHaveTitle("Upload Finished");
        expect(notification).toMatchIntent(new Intent(Actions.MY_PROFILE));
        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();

        Track t = SoundCloudDB.getTrackById(Robolectric.application.getContentResolver(), 12345l);
        expect(t).not.toBeNull();
        expect(t.state).toBe(Track.State.FINISHED);
    }

    @Test
    public void shouldNotifyMixedResults() throws Exception {
        mockSuccessfullTrackCreation();

        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.upload(upload);
        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Finished");
        expect(notification).toHaveText("testing has been uploaded");
        expect(notification).toHaveTitle("Upload Finished");
        expect(notification).toMatchIntent(new Intent(Actions.MY_PROFILE));

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "ohnoez"));
        final Recording upload2 = TestApplication.getValidRecording();
        upload2.what_text = "testing 2";

        svc.upload(upload2);

        expect(m.getAllNotifications().size()).toEqual(2);
        notification = m.getAllNotifications().get(1);
        expect(notification).toHaveTicker("Upload Error");
        expect(notification).toHaveText("There was an error uploading testing 2");
        expect(notification).toHaveTitle("Upload Error");
        expect(notification).toMatchIntent(new Intent(Actions.UPLOAD_MONITOR));
        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();
    }

    @Test
    public void shouldNotifyAboutUploadFailure() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "ohnoez"));
        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Error");
        expect(notification).toHaveText("There was an error uploading testing");
        expect(notification).toHaveTitle("Upload Error");
        expect(notification).toMatchIntent(new Intent(Actions.UPLOAD_MONITOR));
    }

    @Test
    public void shouldUpdateRecordingEntryDuringUploadAndAfterSuccess() throws Exception {
        Recording recording = TestApplication.getValidRecording();
        mockSuccessfullTrackCreation();

        getUploadScheduler().pause();

        svc.upload(recording);

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

        svc.upload(recording);

        Recording updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), recording.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.ERROR);
    }

    @Test
    public void shouldResizeArtworkIfSpecified() throws Exception {
        // cannot test this - just to execute code path
        mockSuccessfullTrackCreation();

        final Recording upload = TestApplication.getValidRecording();
        upload.artwork_path = File.createTempFile("some_artwork", ".png");

        svc.upload(upload);

        expect(upload.isUploaded()).toBeTrue();
        expect(upload.resized_artwork_path).toEqual(upload.artwork_path);

        Recording updated = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), upload.toUri());
        expect(updated.upload_status).toEqual(Recording.Status.UPLOADED);
    }

    @Test
    public void shouldHoldWifiAndWakelockDuringUpload() throws Exception {
        Recording recording = TestApplication.getValidRecording();
        mockSuccessfullTrackCreation();

        getServiceScheduler().pause();
        getMainScheduler().pause();

        svc.upload(recording);

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
        svc.upload(recording);
        expect(svc.getUploadHandler().hasMessages(0)).toBeTrue();

        svc.cancel(recording);
        expect(svc.getUploadHandler().hasMessages(0)).toBeFalse();

        getUploadScheduler().unPause();
    }

    @Test
    public void shouldRespectLifecycle() throws Exception {
        svc.onDestroy();
        expect(shadowOf(svc.getUploadHandler().getLooper()).hasQuit()).toBeTrue();
        expect(shadowOf(svc.getProcessingHandler().getLooper()).hasQuit()).toBeTrue();
        expect(shadowOf(svc.getServiceHandler().getLooper()).hasQuit()).toBeTrue();
    }


    @Test
    public void shouldCheckForStuckRecordingsOnStartup() throws Exception {
        Recording stuck = TestApplication.getValidRecording();
        stuck.upload_status = Recording.Status.UPLOADING;
        SoundCloudDB.insertRecording(svc.getContentResolver(), stuck);

        UploadService service = startService();
        Recording r = SoundCloudDB.getRecordingByUri(svc.getContentResolver(), stuck.toUri());
        expect(r.upload_status).toEqual(Recording.Status.NOT_YET_UPLOADED);
//        expect(shadowOf(service).isStoppedBySelf()).toBeTrue();
    }

    @Test
    public void shouldNotifyIfTranscodingFails() throws Exception {
        mockFailedTrackCreation();
        final Recording upload = TestApplication.getValidRecording();
        upload.what_text = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(2);
        final Notification first = m.getAllNotifications().get(0);
        final Notification second = m.getAllNotifications().get(1);

        expect(first).toHaveTicker("Transcoding Error");
        expect(first).toHaveText("There was an error transcoding recording on sunday night");
        expect(first).toHaveTitle("Transcoding Error");
        expect(first).toMatchIntent(new Intent(Actions.MY_PROFILE));

        expect(second).toHaveTicker("Upload Finished");
        expect(second).toHaveText("testing has been uploaded");
        expect(second).toHaveTitle("Upload Finished");
        expect(second).toMatchIntent(new Intent(Actions.MY_PROFILE));

        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();
    }


    private void mockSuccessfullTrackCreation() throws IOException {
        // track upload
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201,
                TestHelper.resource(getClass(), "track_processing.json")));

        // transcoding polling
        Robolectric.addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200,
                TestHelper.resource(getClass(), "track_finished.json")));
    }


    private void mockFailedTrackCreation() throws IOException {
        // track upload
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(201,
                TestHelper.resource(getClass(), "track_processing.json")));

        // transcoding polling
        Robolectric.addHttpResponseRule("GET", "/tracks/12345", new TestHttpResponse(200,
                TestHelper.resource(getClass(), "track_failed.json")));
    }
}

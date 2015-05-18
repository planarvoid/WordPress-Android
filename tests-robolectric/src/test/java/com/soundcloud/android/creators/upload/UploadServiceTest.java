package com.soundcloud.android.creators.upload;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.createRegexRequestMatcherForUriWithClientId;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.sync.posts.StorePostsCommand;
import com.soundcloud.android.testsupport.RecordingTestHelper;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import com.xtremelabs.robolectric.util.Scheduler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class UploadServiceTest {
    private TestEventBus eventBus = new TestEventBus();
    UploadService svc;

    @Before
    public void before() throws Exception {
        svc = startService();
    }

    private UploadService startService() {
        UploadService service = new UploadService(Mockito.mock(StorePostsCommand.class), eventBus);
        service.onCreate();
        return service;
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

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldStopServiceAfterLastUploadCompletes() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "Error");

        getUploadScheduler().pause();
        // 2 uploads queued
        final Recording r1 = RecordingTestHelper.getValidRecording();
        final Recording r2 = RecordingTestHelper.getValidRecording();
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

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldStopServiceAfterLastUploadCompletesSuccess() throws Exception {
        mockSuccessfullTrackCreation();
        getUploadScheduler().pause();
        final Recording r1 = RecordingTestHelper.getValidRecording();
        svc.upload(r1);

        getUploadScheduler().runOneTask();
        expect(shadowOf(svc).isStoppedBySelf()).toBeFalse();
        getUploadScheduler().runOneTask();

        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();

        expect(r1.upload_status).toEqual(Recording.Status.UPLOADED);
    }


    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldNotifyAboutUploadSuccess() throws Exception {
        mockSuccessfullTrackCreation();

        final Recording upload = RecordingTestHelper.getValidRecording();
        upload.title = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Finished");
        expect(notification).toHaveText("testing has been uploaded");
        expect(notification).toHaveTitle("Upload Finished");
        expect(notification).toMatchIntent(new Intent(Actions.YOUR_SOUNDS));
        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();

        PublicApiTrack t = SoundCloudApplication.sModelManager.getTrack(12345l);
        expect(t).not.toBeNull();
        expect(t.state).toBe(PublicApiTrack.State.FINISHED);
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldNotifyMixedResults() throws Exception {
        mockSuccessfullTrackCreation();

        final Recording upload = RecordingTestHelper.getValidRecording();
        upload.title = "testing";

        svc.upload(upload);
        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Finished");
        expect(notification).toHaveText("testing has been uploaded");
        expect(notification).toHaveTitle("Upload Finished");
        expect(notification).toMatchIntent(new Intent(Actions.YOUR_SOUNDS));

        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "ohnoez"));
        final Recording upload2 = RecordingTestHelper.getValidRecording();
        upload2.title = "testing 2";

        svc.upload(upload2);

        expect(m.getAllNotifications().size()).toEqual(2);
        notification = m.getAllNotifications().get(1);
        expect(notification).toHaveTicker("Upload Error");
        expect(notification).toHaveText("There was an error uploading testing 2");
        expect(notification).toHaveTitle("Upload Error");
        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldNotifyAboutUploadFailure() throws Exception {
        Robolectric.addHttpResponseRule("POST", "/tracks", new TestHttpResponse(503, "ohnoez"));
        final Recording upload = RecordingTestHelper.getValidRecording();
        upload.title = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(1);
        final Notification notification = m.getAllNotifications().get(0);
        expect(notification).toHaveTicker("Upload Error");
        expect(notification).toHaveText("There was an error uploading testing");
        expect(notification).toHaveTitle("Upload Error");
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldHoldWifiAndWakelockDuringUpload() throws Exception {
        Recording recording = RecordingTestHelper.getValidRecording();
        mockSuccessfullTrackCreation();

        getMainScheduler().pause();

        svc.upload(recording);

        expect(svc.getWifiLock().isHeld()).toBeFalse();
        expect(svc.getWakeLock().isHeld()).toBeFalse();

        getMainScheduler().runOneTask();

        expect(svc.getWifiLock().isHeld()).toBeFalse();
        expect(svc.getWakeLock().isHeld()).toBeFalse();
        // TODO needs BroadcastManager w/ step execution of queued runnables
    }

    @Ignore // fails with JNI error on Java 7
    @Test
    public void cancelUploadShouldRemoveAllMessagesFromTheQueue() throws Exception {
        Recording recording = RecordingTestHelper.getValidRecording();

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
    }


    @Ignore // fails with JNI error on Java 7
    @Test
    public void shouldNotifyIfTranscodingFails() throws Exception {
        mockFailedTrackCreation();
        final Recording upload = RecordingTestHelper.getValidRecording();
        upload.title = "testing";

        svc.upload(upload);

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        expect(m.getAllNotifications().size()).toEqual(2);
        final Notification first = m.getAllNotifications().get(0);
        final Notification second = m.getAllNotifications().get(1);

        expect(first).toHaveTicker("Transcoding Error");
        expect(first).toHaveText("There was an error transcoding recording on sunday night");
        expect(first).toHaveTitle("Transcoding Error");
        expect(first).toMatchIntent(new Intent(Actions.YOUR_SOUNDS));

        expect(second).toHaveTicker("Upload Finished");
        expect(second).toHaveText("testing has been uploaded");
        expect(second).toHaveTitle("Upload Finished");
        expect(second).toMatchIntent(new Intent(Actions.YOUR_SOUNDS));

        expect(shadowOf(svc).isStoppedBySelf()).toBeTrue();
    }


    private void mockSuccessfullTrackCreation() throws IOException {
        // track upload
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpPost.METHOD_NAME, "/tracks"), new TestHttpResponse(201,
                JsonFixtures.resourceAsBytes(getClass(), "track_processing.json")));

        // transcoding polling
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, "/tracks/12345"), new TestHttpResponse(200,
                JsonFixtures.resourceAsBytes(getClass(), "track_finished.json")));
    }


    private void mockFailedTrackCreation() throws IOException {
        // track upload
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpPost.METHOD_NAME, "/tracks"), new TestHttpResponse(201,
                JsonFixtures.resourceAsBytes(getClass(), "track_processing.json")));

        // transcoding polling
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, "/tracks/12345"), new TestHttpResponse(200,
                JsonFixtures.resourceAsBytes(getClass(), "track_failed.json")));
    }
}

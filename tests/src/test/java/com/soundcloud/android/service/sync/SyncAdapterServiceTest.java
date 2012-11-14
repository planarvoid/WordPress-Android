package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.sync.CollectionSyncRequestTest.NON_INTERACTIVE;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotification;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.TestIntentSender;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class SyncAdapterServiceTest {

    @Before
    public void before() {
        // don't want default syncing for tests
        SyncContent.setAllSyncEnabledPrefs(Robolectric.application, false);

        TestHelper.setBackgrounData(true);
        TestHelper.connectedViaWifi(true);

        // the current sc user, assumed to be already in the db
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, 133201L);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        Robolectric.application.getContentResolver().insert(Content.USERS.uri, cv);

        // always notify
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putString(Consts.PrefKeys.NOTIFICATIONS_FREQUENCY, 0+"")
                .commit();

        DefaultTestRunner.application.setCurrentUserId(100l);
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void testIncomingNotificationMessage() throws Exception {
        Activities activities = SoundCloudApplication.MODEL_MANAGER.getActivitiesFromJson(getClass().getResourceAsStream("e1_stream.json"));
        String message = Message.getIncomingNotificationMessage(
                DefaultTestRunner.application, activities);

        expect(message).toEqual("from WADR, T-E-E-D and others");
    }

    @Test
    public void testExclusiveNotificationMessage() throws Exception {
        Activities events = SoundCloudApplication.MODEL_MANAGER.getActivitiesFromJson(getClass().getResourceAsStream("e1_stream_1.json"));
        String message = Message.getExclusiveNotificationMessage(
                DefaultTestRunner.application, events);

        expect(message).toEqual("exclusives from Bad Panda Records, The Takeaway and others");
    }

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "empty_events.json", "empty_events.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.getInfo().getContentText().toString()).toEqual(
            "from Bad Panda Records, The Takeaway and others");
        expect(result.getInfo().getContentTitle().toString()).toEqual(
            "20 new sounds");
        expect(result.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldNotRepeatNotification() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "empty_events.json", "e1_activities_1_oldest.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.notifications.size()).toEqual(2);

        addCannedActivities("empty_events.json", "empty_events.json", "empty_events.json");
        result = doPerformSync(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }


    @Test
    public void shouldNotifyAboutIncomingAndExclusives() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "e1_stream_2_oldest.json", "empty_events.json");

        SoundCloudApplication app = DefaultTestRunner.application;
        List<NotificationInfo> notifications = doPerformSync(app, false, null).notifications;

        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.info.getContentTitle().toString())
                .toEqual("46 new sounds");

        expect(n.info.getContentText().toString())
                .toEqual("exclusives from WADR, T-E-E-D and others");

        expect(n.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldSendTwoSeparateNotifications() throws Exception {
        addCannedActivities("e1_stream_1_oldest.json", "empty_events.json", "e1_activities_2.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false, null).notifications;
        expect(notifications.size()).toEqual(2);

        expect(notifications.get(0).info.getContentTitle().toString())
                .toEqual("20 new sounds");
        expect(notifications.get(0).info.getContentText().toString())
                .toEqual("from Bad Panda Records, The Takeaway and others");

        expect(notifications.get(0).getIntent().getAction())
                .toEqual(Actions.STREAM);

        expect(notifications.get(1).info.getContentTitle().toString())
                .toEqual("10 new activities");
        expect(notifications.get(1).info.getContentText().toString())
                .toEqual("Comments and likes from D∃SIGNATED∀VΞ and Liraz Axelrad");

        expect(notifications.get(1).getIntent().getAction())
                .toEqual(Actions.ACTIVITY);
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingOne() throws Exception {
        assertNotification("own_one_favoriting.json",
                "New like",
                "A new like",
                "Paul Ko likes P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingTwo() throws Exception {
        assertNotification("own_two_favoritings.json",
                "2 new likes",
                "2 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");
    }

    @Test
    public void shouldNotifyAboutActivityFavoritingMultiple() throws Exception {
        assertNotification("own_multi_favoritings.json",
                "3 new likes",
                "3 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein, William Gibson & Cory Doctorow on 'Zero History'" +
                        " and other sounds");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingSingle() throws Exception {
        assertNotification("own_one_comment.json",
                "1 new comment",
                "1 new comment",
                "new comment on Autotune at MTV from fronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingTwo() throws Exception {
        assertNotification("own_two_comments.json",
                "2 new comments",
                "2 new comments",
                "2 new comments on Autotune at MTV from fronx and bronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingThree() throws Exception {
        assertNotification("own_three_comments.json",
                "3 new comments",
                "3 new comments",
                "3 new comments on Autotune at MTV from fronx, cronx and others");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingFour() throws Exception {
        assertNotification("own_four_comments_different_tracks.json",
                "4 new comments",
                "4 new comments",
                "Comments from fronx, bronx and others");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingTwoUsers() throws Exception {
        assertNotification("own_comment_favoriting_same_track.json",
                "2 new activities",
                "2 new activities",
                "from fronx on Autotune at MTV");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingTwoDifferentUsers() throws Exception {
        assertNotification("own_comment_favoriting_different_tracks_two_users.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from bronx and fronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingMultipleDifferentUsers() throws Exception {
        assertNotification("own_comment_favoriting_different_tracks.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from Paul Ko, changmangoo and others");
    }

    @Test
    public void shouldNotify99PlusItems() throws Exception {
        addCannedActivities(
                "e1_stream.json",
                "e1_stream_2.json",
                "e1_stream_oldest.json",
                "e1_stream_oldest.json",
                "empty_events.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false, null).notifications;
        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.info.getContentTitle().toString())
                .toEqual("99+ new sounds");
    }


    @Test
    public void shouldNotSyncWhenTokenIsInvalidAndFlagError() throws Exception {
        // will throw if actually syncing
        SyncResult result = doPerformSync(new TestApplication(new Token(null, null, null)), false, null).result;
        expect(result.hasError()).toBeTrue();
        expect(result.hasHardError()).toBeTrue();
        expect(result.hasSoftError()).toBeFalse();
    }

    @Test
    public void shouldFlagSoftErrorWhenIOError() throws Exception {
        addCannedActivities("empty_events.json");
        addPendingHttpResponse(500, "errors");
        addPendingHttpResponse(500, "errors");

        SyncResult result = doPerformSync(DefaultTestRunner.application, false, null).result;
        expect(result.hasHardError()).toBeFalse();
        expect(result.hasSoftError()).toBeTrue();
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "e1_activities.json"
        );
        expect(doPerformSync(DefaultTestRunner.application, true, null).notifications).toBeEmpty();
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotifications() throws Exception {
        addCannedActivities("empty_events.json", "empty_events.json", "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        addCannedActivities("empty_events.json", "empty_events.json", "e1_activities_2.json");
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotificationsWhenUserHasSeen() throws Exception {
        addCannedActivities("empty_events.json", "empty_events.json", "e1_activities_1_oldest.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("7 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("7 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from Liraz Axelrad, UnoFuego and others");

        // user has already seen some stuff
        DefaultTestRunner.application.setAccountData(
                User.DataKeys.LAST_OWN_SEEN,
                AndroidCloudAPI.CloudDateFormat.fromString("2011/07/23 11:51:29 +0000").getTime()
        );

       addCannedActivities("empty_events.json", "empty_events.json", "e1_activities_2.json"
       );
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("9 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("9 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from D∃SIGNATED∀VΞ, Liraz Axelrad and others");
    }

    @Test
    public void shouldCheckPushEventExtraParameterLike() throws Exception {
        addCannedActivities("e1_activities_1_oldest.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.LIKE.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldCheckPushEventExtraParameterComment() throws Exception {
        addCannedActivities("e1_activities_2.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.COMMENT.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldOnlySyncActivitiesFromPushEventLike() throws Exception {
        shouldOnlySyncActivitiesFromPushEvent(PushEvent.LIKE.type);
    }

    @Test
    public void shouldOnlySyncActivitiesFromPushEventComment() throws Exception {
        shouldOnlySyncActivitiesFromPushEvent(PushEvent.COMMENT.type);
    }

    private void shouldOnlySyncActivitiesFromPushEvent(String pushType) throws Exception {
        addCannedActivities("e1_activities_2.json");

        // add my sounds should sync
        SyncContent.MySounds.setEnabled(Robolectric.application, true);
        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");


        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, pushType);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        LocalCollection lc = LocalCollection.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).toBeNull();
        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldShowNewFetchedFollower() throws Exception {
        TestHelper.addIdResponse("/me/followers/ids?linked_partitioning=1"+NON_INTERACTIVE, 792584, 1255758, 308291);
        addResourceResponse("/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE+NON_INTERACTIVE, "users.json");

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("user.json"))));

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.FOLLOWER.type);
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT_URI, "soundcloud:users:12345");

        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(1);

        expect(result.getTicker()).toEqual("New follower");
        expect(result.getInfo().getContentTitle().toString()).toEqual("You have a new follower");
        expect(result.getInfo().getContentText().toString()).toEqual("SoundCloud Android @ MWC is now following you. Follow back?");
    }

    @Test
    public void shouldCheckPushEventExtraParameterUnknown() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "alien-sync");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(0);
    }


    @Test
    public void shouldSyncLocalCollections() throws Exception {
        SyncContent.MySounds.setEnabled(Robolectric.application, true);

        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1"+NON_INTERACTIVE, 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3"+NON_INTERACTIVE, "tracks.json");

        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "empty_events.json");

        doPerformSync(DefaultTestRunner.application, false, null);

        LocalCollection lc = LocalCollection.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).not.toBeNull();
        expect(lc.extra).toEqual("0");
        expect(lc.size).toEqual(3);
        expect(lc.last_sync_success).not.toEqual(0L);

        // reset sync time & rerun sync
        addCannedActivities(
                "empty_events.json",
                "empty_events.json",
                "empty_events.json");

        lc.updateLastSyncSuccessTime(0, DefaultTestRunner.application.getContentResolver());

        doPerformSync(DefaultTestRunner.application, false, null);

        lc = LocalCollection.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).not.toBeNull();
        expect(lc.extra).toEqual("1");    // 1 miss
        expect(lc.size).toEqual(3);
        expect(lc.last_sync_success).not.toEqual(0L);
    }

    @Test
    public void performSyncShouldReturnFalseIfNoSyncStarted() throws Exception {
        TestHelper.connectedViaWifi(false);
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putBoolean(Consts.PrefKeys.NOTIFICATIONS_WIFI_ONLY, true)
                .commit();

        boolean hasSynced = SyncAdapterService.performSync(DefaultTestRunner.application,
                new Account("foo", "bar"),
                new Bundle(),
                new SyncResult(),
                null);

        expect(hasSynced).toBeFalse();
    }


    static class SyncOutcome {
        List<NotificationInfo> notifications;
        SyncResult result;
        Intent intent;

        Intent getIntent() {
            expect(notifications.size()).toEqual(1);
            return notifications.get(0).getIntent();
        }

        ShadowNotification.LatestEventInfo getInfo() {
            expect(notifications.size()).toEqual(1);
            return notifications.get(0).info;

        }
        String getTicker() {
            expect(notifications.size()).toEqual(1);
            return notifications.get(0).n.tickerText.toString();
        }
    }

    private static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime, @Nullable Bundle extras)
            throws Exception {
        if (!firstTime) app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        if (extras == null) extras = new Bundle();

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));
        m.cancelAll();

        SyncResult result = new SyncResult();
        SyncAdapterService.performSync(
                app,
                new Account("foo", "bar"),
                extras, result, null);

        Intent intent = Robolectric.shadowOf(app).peekNextStartedService();

        if (intent != null) {
            // robolectric doesn't run the service code for us, need to do it manually
            @SuppressWarnings("unchecked")
            Class<? extends Service> klazz =
                    (Class<? extends Service>) Class.forName(intent.getComponent().getClassName());

            Service svc = klazz.newInstance();
            svc.onCreate();
            svc.onStart(intent, 0);
        }

        List<NotificationInfo> list = new ArrayList<NotificationInfo>();
        for (Notification n : m.getAllNotifications()) {
            list.add(new NotificationInfo(n, shadowOf(n).getLatestEventInfo()));
        }
        SyncOutcome outcome = new SyncOutcome();
        outcome.notifications = list;
        outcome.result = result;
        outcome.intent = intent;
        return outcome;
    }

    private void assertNotification(String resource, String ticker, String title, String content) throws Exception {
         addCannedActivities(
                 "empty_events.json",
                 "empty_events.json",
                 resource
         );

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1l);
        List<NotificationInfo> notifications = doPerformSync(app, false, null).notifications;
        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.n.tickerText.toString()).toEqual(ticker);
        expect(n.info.getContentTitle().toString()).toEqual(title);
        expect(n.info.getContentText().toString()).toEqual(content);
    }

    static class NotificationInfo {
        public Notification n;
        public ShadowNotification.LatestEventInfo info;

        NotificationInfo(Notification n, ShadowNotification.LatestEventInfo info) {
            this.n = n;
            this.info = info;
        }

        public Intent getIntent() {
            return ((TestIntentSender) n.contentIntent.getIntentSender()).intent;
        }
    }

    void addCannedActivities(String... resources) throws IOException {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class, resources);
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }
}
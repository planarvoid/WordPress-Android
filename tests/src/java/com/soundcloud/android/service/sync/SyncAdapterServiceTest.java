package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.newInstanceOf;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotification;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class SyncAdapterServiceTest {

    @Before
    public void before() {
        Robolectric.application.onCreate();

        // pretend we're connected via wifi
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Robolectric.shadowOf(cm).setBackgroundDataSetting(true);
        Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI,
                newInstanceOf(NetworkInfo.class));

        // the current sc user, assumed to be already in the db
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Users._ID, 133201L);
        cv.put(DBHelper.Users.USERNAME, "Foo Bar");
        Robolectric.application.getContentResolver().insert(Content.USERS.uri, cv);

        // always notify
        PreferenceManager.getDefaultSharedPreferences(Robolectric.application)
                .edit()
                .putString(SyncAdapterService.PREF_NOTIFICATIONS_FREQUENCY, 0+"")
                .commit();
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void testIncomingNotificationMessage() throws Exception {
        Activities activities = Activities.fromJSON(getClass().getResourceAsStream("incoming_2.json"));
        String message = SyncAdapterService.getIncomingNotificationMessage(
                DefaultTestRunner.application, activities);

        expect(message).toEqual("from All Tomorrows Parties, DominoRecordCo and others");
    }

    @Test
    public void testExclusiveNotificationMessage() throws Exception {
        Activities events = Activities.fromJSON(getClass().getResourceAsStream("incoming_2.json"));
        String message = SyncAdapterService.getExclusiveNotificationMessage(
                DefaultTestRunner.application, events);

        expect(message).toEqual("exclusives from All Tomorrows Parties, DominoRecordCo and others");
    }

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addCannedActivities("incoming_2.json", "empty_events.json", "empty_events.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.getInfo().getContentText().toString()).toEqual(
            "from All Tomorrows Parties, DominoRecordCo and others");
        expect(result.getInfo().getContentTitle().toString()).toEqual(
            "49 new sounds");
        expect(result.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldNotRepeatNotification() throws Exception {
        addCannedActivities("incoming_2.json", "empty_events.json", "own_2.json");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, null);

        expect(result.notifications.size()).toEqual(2);

        addCannedActivities("empty_events.json", "empty_events.json", "empty_events.json");
        result = doPerformSync(DefaultTestRunner.application, false, null);
        expect(result.notifications).toBeEmpty();
    }


    @Test
    public void shouldNotifyAboutIncomingAndExclusives() throws Exception {
        addCannedActivities("incoming_2.json", "exclusives_1.json", "empty_events.json");

        SoundCloudApplication app = DefaultTestRunner.application;
        List<NotificationInfo> notifications = doPerformSync(app, false, null).notifications;

        expect(notifications.size()).toEqual(1);
        NotificationInfo n = notifications.get(0);
        expect(n.info.getContentTitle().toString())
                .toEqual("53 new sounds");

        expect(n.info.getContentText().toString())
                .toEqual("exclusives from jberkel_testing, xla and others");

        expect(n.getIntent().getAction()).toEqual(Actions.STREAM);
    }

    @Test
    public void shouldSendTwoSeparateNotifications() throws Exception {
        addCannedActivities("incoming_2.json", "empty_events.json", "own_1.json", "own_2.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false, null).notifications;
        expect(notifications.size()).toEqual(2);

        expect(notifications.get(0).info.getContentTitle().toString())
                .toEqual("49 new sounds");
        expect(notifications.get(0).info.getContentText().toString())
                .toEqual("from All Tomorrows Parties, DominoRecordCo and others");

        expect(notifications.get(0).getIntent().getAction())
                .toEqual(Actions.STREAM);

        expect(notifications.get(1).info.getContentTitle().toString())
                .toEqual("41 new activities");
        expect(notifications.get(1).info.getContentText().toString())
                .toEqual("Comments and likes from Paul Ko, jensnikolaus and others");

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
                "incoming_1.json",
                "incoming_2.json",
                "exclusives_1.json",
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
                "activities_1.json"
        );
        expect(doPerformSync(DefaultTestRunner.application, true, null).notifications).toBeEmpty();
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotifications() throws Exception {
        addCannedActivities("empty_events.json", "empty_events.json", "activities_1.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("39 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("39 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from EddieSongWriter, changmangoo and others");

        addCannedActivities("empty_events.json", "empty_events.json", "activities_2.json");
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("41 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("41 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from Paul Ko, jensnikolaus and others");
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotificationsWhenUserHasSeen() throws Exception {
        addCannedActivities("empty_events.json", "empty_events.json", "activities_1.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false, null);

        expect(first.getTicker()).toEqual("39 new activities");
        expect(first.getInfo().getContentTitle().toString()).toEqual("39 new activities");
        expect(first.getInfo().getContentText().toString()).toEqual("Comments and likes from EddieSongWriter, changmangoo and others");

        // user has already seen some stuff
        DefaultTestRunner.application.setAccountData(
                User.DataKeys.LAST_OWN_SEEN,
                AndroidCloudAPI.CloudDateFormat.fromString("2011/07/23 11:51:29 +0000").getTime()
        );

       addCannedActivities("empty_events.json", "empty_events.json", "activities_2.json"
       );
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false, null);

        expect(second.getTicker()).toEqual("3 new activities");
        expect(second.getInfo().getContentTitle().toString()).toEqual("3 new activities");
        expect(second.getInfo().getContentText().toString()).toEqual("Comments and likes from Paul Ko, jensnikolaus and others");
    }

    @Test
    public void shouldCheckPushEventExtraParameterLike() throws Exception {
        addCannedActivities("own_2.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.LIKE.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldCheckPushEventExtraParameterComment() throws Exception {
        addCannedActivities("own_2.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.COMMENT.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldCheckPushEventExtraParameterFollower() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.FOLLOWER.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(0);
    }

    @Test
    public void shouldCheckPushEventExtraParameterUnknown() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "alien-sync");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(0);
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

    private static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime, Bundle extras)
            throws Exception {
        if (!firstTime) {
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        }

        if (extras == null) {
            extras = new Bundle();
        }

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        m.cancelAll();

        SyncResult result = new SyncResult();
        Intent intent = SyncAdapterService.performSync(
                app,
                new Account("foo", "bar"),
                extras, null, result);


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
}

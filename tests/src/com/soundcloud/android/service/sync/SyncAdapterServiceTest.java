package com.soundcloud.android.service.sync;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.Actions;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotification;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.TestIntentSender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@RunWith(DefaultTestRunner.class)
public class SyncAdapterServiceTest extends ApiTests {
    @Test
    public void testGetNewIncomingEvents() throws Exception {
        addCannedEvents(
            "incoming_1.json",
            "incoming_2.json"
        );

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, null, 0l, false);

        assertThat(events.size(), is(100));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
    }

    @Test
    public void testGetNewIncomingEventsExclusive() throws Exception {
        addCannedEvents("exclusives_1.json");

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, null, 0l, true);

        assertThat(events.size(), is(4));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/tracks/exclusive?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
    }


    @Test
    public void testGetOwnEvents() throws Exception {
        addCannedEvents(
            "own_1.json",
            "own_2.json"
        );

        Activities events = SyncAdapterService.getOwnEvents(
                DefaultTestRunner.application, null, 0l);

        assertThat(events.size(), is(42));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/all/own?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
    }

    @Test
    public void testWithSince() throws Exception {
        addCannedEvents("incoming_1.json");
        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application,
                null,
                1310462679000l
                , false);

        assertThat(events.size(), is(1));

        addCannedEvents("incoming_1.json");
        events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application,
                null,
                1310462016000l
                , false);

        assertThat(events.size(), is(2));
    }

    @Test
    public void testGetUniqueUsersFromEvents() throws Exception {
        addCannedEvents("incoming_2.json");

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, null, 0l, false);
        assertThat(events.size(), is(50));

        List<User> users = events.getUniqueUsers();
        assertThat(users.size(), is(31));

        Set<Long> ids = new HashSet<Long>();
        for (User u : users) ids.add(u.id);
        assertThat(ids.size(), is(users.size()));
    }

    @Test
    public void testIncomingMessaging() throws Exception {
        addCannedEvents("incoming_2.json");

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, null, 0l, false);

        String message = SyncAdapterService.getIncomingMessaging(
                DefaultTestRunner.application, events);

        assertThat(message, equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
    }

    @Test
    public void testExclusiveMessaging() throws Exception {
        addCannedEvents("incoming_2.json");

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, null, 0l, false);

        String message = SyncAdapterService.getExclusiveMessaging(
                DefaultTestRunner.application, events);

        assertThat(message, equalTo("exclusives from All Tomorrows Parties, DominoRecordCo and others"));
    }

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addCannedEvents(
            "incoming_2.json",
            "empty_events.json",
            "empty_events.json"
        );

        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false);

        assertThat(result.getInfo().getContentText().toString(),
                equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
        assertThat(result.getInfo().getContentTitle().toString(),
                equalTo("49 new sounds"));
        assertThat(result.getIntent().getAction(), equalTo(Actions.STREAM));
    }

    @Test
    public void shouldNotRepeatNotification() throws Exception {
        addCannedEvents(
            "incoming_2.json",
            "empty_events.json",
            "own_2.json"
        );

        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false);

        assertThat(result.notifications.size(), is(2));

        addCannedEvents(
            "empty_events.json",
            "empty_events.json",
            "empty_events.json"
        );

        result = doPerformSync(DefaultTestRunner.application, false);

        assertThat(result.notifications.size(), is(0));
    }


    @Test
    public void shouldNotifyAboutIncomingAndExclusives() throws Exception {
        addCannedEvents(
            "incoming_2.json",
            "exclusives_1.json",
            "empty_events.json"
        );

        SoundCloudApplication app = DefaultTestRunner.application;
        List<NotificationInfo> notifications = doPerformSync(app, false).notifications;

        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo("53 new sounds"));

        assertThat(n.info.getContentText().toString(),
                equalTo("exclusives from jberkel_testing, xla and others"));

        assertThat(n.getIntent().getAction(), equalTo(Actions.STREAM));
    }

    @Test
    public void shouldSendTwoSeparateNotifications() throws Exception {
        addCannedEvents(
            "incoming_2.json",
            "empty_events.json",
            "own_1.json",
            "own_2.json"
        );

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false).notifications;
        assertThat(notifications.size(), is(2));

        assertThat(notifications.get(0).info.getContentTitle().toString(),
                equalTo("49 new sounds"));
        assertThat(notifications.get(0).info.getContentText().toString(),
                equalTo("from All Tomorrows Parties, DominoRecordCo and others"));

        assertThat(notifications.get(0).getIntent().getAction(),
                equalTo(Actions.STREAM));

        assertThat(notifications.get(1).info.getContentTitle().toString(),
                equalTo("42 new activities"));
        assertThat(notifications.get(1).info.getContentText().toString(),
                equalTo("Comments and likes from Paul Ko, jensnikolaus and others"));

        assertThat(notifications.get(1).getIntent().getAction(),
                equalTo(Actions.ACTIVITY));
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
                "3 new comments on Autotune at MTV from fronx, bronx and others");
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
                "Comments and likes from fronx and bronx");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoritingMultipleDifferentUsers() throws Exception {
        assertNotification("own_comment_favoriting_different_tracks.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from fronx, bronx and others");
    }

    @Test
    public void shouldNotify99PlusItems() throws Exception {
        addCannedEvents(
            "incoming_1.json",
            "incoming_2.json",
            "exclusives_1.json",
            "empty_events.json");

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false).notifications;
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo("99+ new sounds"));
    }


    @Test
    public void shouldNotSyncWhenTokenIsInvalidAndFlagError() throws Exception {
        // will throw if actually syncing
        SyncResult result = doPerformSync(new TestApplication(new Token(null, null, null)), false).result;
        assertThat(result.hasError(), is(true));
        assertThat(result.hasHardError(), is(true));
        assertThat(result.hasSoftError(), is(false));
    }

    @Test
    public void shouldFlagSoftErrorWhenIOError() throws Exception {
        addPendingHttpResponse(500, "errors");
        addPendingHttpResponse(500, "errors");

        SyncResult result = doPerformSync(DefaultTestRunner.application, false).result;
        assertThat(result.hasHardError(), is(false));
        assertThat(result.hasSoftError(), is(true));
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addCannedEvents("incoming_2.json");
        assertThat(doPerformSync(DefaultTestRunner.application, true).notifications.size(), is(0));
    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotifications() throws Exception {
        addCannedEvents("empty_events.json", "empty_events.json", "activities_1.json");
        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false);

        assertThat(first.getTicker(), equalTo("39 new activities"));
        assertThat(first.getInfo().getContentTitle().toString(), equalTo(first.getTicker()));
        assertThat(first.getInfo().getContentText().toString(), equalTo("Comments and likes from EddieSongWriter, changmangoo and others"));

        addCannedEvents("empty_events.json", "empty_events.json", "activities_2.json");
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false);

        assertThat(second.getTicker(), equalTo("41 new activities"));
        assertThat(second.getInfo().getContentTitle().toString(), equalTo(second.getTicker()));
        assertThat(second.getInfo().getContentText().toString(), equalTo("Comments and likes from Paul Ko, jensnikolaus and others"));

    }

    @Test
    public void shouldUseCachedActivitiesToUpdateNotificationsWhenUserHasSeen() throws Exception {
        addCannedEvents(
            "empty_events.json",
            "empty_events.json",
            "activities_1.json"
        );

        SyncOutcome first = doPerformSync(DefaultTestRunner.application, false);

        assertThat(first.getTicker(), equalTo("39 new activities"));
        assertThat(first.getInfo().getContentTitle().toString(), equalTo(first.getTicker()));
        assertThat(first.getInfo().getContentText().toString(), equalTo("Comments and likes from EddieSongWriter, changmangoo and others"));

        // user has already seen some stuff
        DefaultTestRunner.application.setAccountData(
            User.DataKeys.LAST_OWN_SEEN,
            AndroidCloudAPI.CloudDateFormat.fromString("2011/07/23 11:51:29 +0000").getTime()
        );

       addCannedEvents(
            "empty_events.json",
            "empty_events.json",
            "activities_2.json"
        );
        SyncOutcome second = doPerformSync(DefaultTestRunner.application, false);

        assertThat(second.getTicker(), equalTo("3 new activities"));
        assertThat(second.getInfo().getContentTitle().toString(), equalTo(second.getTicker()));
        assertThat(second.getInfo().getContentText().toString(), equalTo("Comments and likes from Paul Ko, jensnikolaus and others"));
    }

    static class SyncOutcome {
        List<NotificationInfo> notifications;
        SyncResult result;

        Intent getIntent() {
            assertThat(notifications.size(), is(1));
            return notifications.get(0).getIntent();
        }

        ShadowNotification.LatestEventInfo getInfo() {
            assertThat(notifications.size(), is(1));
            return notifications.get(0).info;

        }
        String getTicker() {
            assertThat(notifications.size(), is(1));
            return notifications.get(0).n.tickerText.toString();
        }
    }

    private static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime)
            throws OperationCanceledException {

        if (!firstTime) {
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        }

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        m.cancelAll();

        SyncResult result = new SyncResult();
        SyncAdapterService.performSync(
                app,
                new Account("foo", "bar"),
                null, null, result);


        List<NotificationInfo> list = new ArrayList<NotificationInfo>();
        for (Notification n : m.getAllNotifications()) {
            list.add(new NotificationInfo(n, shadowOf(n).getLatestEventInfo()));
        }
        SyncOutcome outcome = new SyncOutcome();
        outcome.notifications = list;
        outcome.result = result;
        return outcome;
    }

    private void assertNotification(String resource, String ticker, String title, String content) throws Exception {
         addCannedEvents(
                 "empty_events.json",
                 "empty_events.json",
                 resource
         );

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1l);
        List<NotificationInfo> notifications = doPerformSync(app, false).notifications;
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.n.tickerText.toString(), equalTo(ticker));
        assertThat(n.info.getContentTitle().toString(),
                equalTo(title));

        assertThat(n.info.getContentText().toString(),
                equalTo(content));
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

    void addCannedEvents(String... resources) throws IOException {
        for (String r : resources) {
            addPendingHttpResponse(200, resource(r));
        }
    }
}

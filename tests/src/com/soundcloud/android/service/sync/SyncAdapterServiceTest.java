package com.soundcloud.android.service.sync;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.TestApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.SyncAdapterService;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@RunWith(DefaultTestRunner.class)
public class SyncAdapterServiceTest extends ApiTests {

    @Test
    public void testGetNewIncomingEvents() throws Exception {
        addPendingHttpResponse(200, resource("incoming_1.json"));
        addPendingHttpResponse(200, resource("incoming_2.json"));

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, 0l, false);

        assertThat(events.size(), is(100));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/tracks?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));

        assertThat(DefaultTestRunner.application.getAccountData(User.DataKeys.INCOMING_FUTURE_HREF),
                equalTo(events.future_href));
    }

    @Test
    public void testGetNewIncomingEventsExclusive() throws Exception {
        addPendingHttpResponse(200, resource("exclusives_1.json"));

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, 0l, true);

        assertThat(events.size(), is(4));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/tracks/exclusive?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));
        assertThat(DefaultTestRunner.application.getAccountData(User.DataKeys.EXCLUSIVE_FUTURE_HREF),
                equalTo(events.future_href));
    }


    @Test
    public void testGetOwnEvents() throws Exception {
        addPendingHttpResponse(200, resource("own_1.json"));
        addPendingHttpResponse(200, resource("own_2.json"));

        Activities events = SyncAdapterService.getOwnEvents(
                DefaultTestRunner.application, 0l);

        assertThat(events.size(), is(42));
        assertThat(events.future_href,
                equalTo("https://api.soundcloud.com/me/activities/all/own?uuid[to]=e46666c4-a7e6-11e0-8c30-73a2e4b61738"));

        assertThat(DefaultTestRunner.application.getAccountData(User.DataKeys.OWN_FUTURE_HREF),
                equalTo(events.future_href));
    }

    @Test
    public void testWithSince() throws Exception {
        addPendingHttpResponse(200, resource("incoming_1.json"));
        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application,
                1310462679000l
                , false);

        assertThat(events.size(), is(1));

        addPendingHttpResponse(200, resource("incoming_1.json"));
        events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application,
                1310462016000l
                , false);

        assertThat(events.size(), is(2));
    }

    @Test
    public void testGetUniqueUsersFromEvents() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, 0l, false);
        assertThat(events.size(), is(50));

        List<User> users = events.getUniqueUsers();
        assertThat(users.size(), is(31));

        Set<Long> ids = new HashSet<Long>();
        for (User u : users) ids.add(u.id);
        assertThat(ids.size(), is(users.size()));
    }

    @Test
    public void testIncomingMessaging() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));

        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, 0l, false);

        String message = SyncAdapterService.getIncomingMessaging(
                DefaultTestRunner.application, events);

        assertThat(message, equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
    }

    @Test
    public void testExclusiveMessaging() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        Activities events = SyncAdapterService.getNewIncomingEvents(
                DefaultTestRunner.application, 0l, false);

        String message = SyncAdapterService.getExclusiveMessaging(
                DefaultTestRunner.application, events);

        assertThat(message, equalTo("exclusives from All Tomorrows Parties, DominoRecordCo and others"));
    }

    @Test
    public void shouldNotifyIfSyncedBefore() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));

        List<NotificationInfo> notifications = doPerformSync(DefaultTestRunner.application, false).notifications;
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentText().toString(),
                equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
        assertThat(n.info.getContentTitle().toString(),
                equalTo("49 new sounds"));

        assertThat(n.getIntent().getAction(), equalTo(Actions.STREAM));
    }


    @Test
    public void shouldNotifyAboutIncomingAndExclusives() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("exclusives_1.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));

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
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("own_1.json"));
        addPendingHttpResponse(200, resource("own_2.json"));

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
    public void shouldNotifyAboutActivityFavoriting() throws Exception {
        assertNotification("own_one_favoriting.json",
                "New like",
                "A new like",
                "Paul Ko likes P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");

        assertNotification("own_two_favoritings.json",
                "2 new likes",
                "2 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein");

        assertNotification("own_multi_favoritings.json",
                "3 new likes",
                "3 new likes",
                "on P. Watzlawick - Anleitung zum Ungl\u00fccklichsein, William Gibson & Cory Doctorow on 'Zero History'" +
                        " and other sounds");
    }

    @Test
    public void shouldNotifyAboutActivityCommenting() throws Exception {
        assertNotification("own_one_comment.json",
                "1 new comment",
                "1 new comment",
                "new comment on Autotune at MTV from fronx");

        assertNotification("own_two_comments.json",
                "2 new comments",
                "2 new comments",
                "2 new comments on Autotune at MTV from fronx and bronx");

        assertNotification("own_three_comments.json",
                "3 new comments",
                "3 new comments",
                "3 new comments on Autotune at MTV from fronx, bronx and others");

        assertNotification("own_four_comments_different_tracks.json",
                "4 new comments",
                "4 new comments",
                "Comments from fronx, bronx and others");
    }

    @Test
    public void shouldNotifyAboutActivityCommentingAndFavoriting() throws Exception {
        assertNotification("own_comment_favoriting_same_track.json",
                "2 new activities",
                "2 new activities",
                "from fronx on Autotune at MTV");

        assertNotification("own_comment_favoriting_different_tracks_two_users.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from fronx and bronx");

        assertNotification("own_comment_favoriting_different_tracks.json",
                "5 new activities",
                "5 new activities",
                "Comments and likes from fronx, bronx and others");
    }

    @Test
    public void shouldNotify99PlusItems() throws Exception {
        addPendingHttpResponse(200, resource("incoming_1.json"));
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("exclusives_1.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));

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
        SyncResult result = doPerformSync(DefaultTestRunner.application, false).result;
        assertThat(result.hasHardError(), is(false));
        assertThat(result.hasSoftError(), is(true));
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        assertThat(doPerformSync(DefaultTestRunner.application, true).notifications.size(), is(0));
    }

    static class SyncOutcome {
        List<NotificationInfo> notifications;
        SyncResult result;
    }

    private static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime)
            throws OperationCanceledException {

        if (!firstTime) {
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        }

        SyncResult result = new SyncResult();
        SyncAdapterService.performSync(
                app,
                new Account("foo", "bar"),
                null, null, result);

        ShadowNotificationManager m =shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

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
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource(resource));

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
}

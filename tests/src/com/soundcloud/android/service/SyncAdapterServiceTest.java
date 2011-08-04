package com.soundcloud.android.service;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.soundcloud.android.robolectric.DefaultTestRunner;
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
    }

    @Test
    public void testGetOwnEvents() throws Exception {
        addPendingHttpResponse(200, resource("own_1.json"));
        addPendingHttpResponse(200, resource("own_2.json"));

        Activities events = SyncAdapterService.getOwnEvents(
                DefaultTestRunner.application, 0l);

        assertThat(events.size(), is(42));
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

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);

        List<NotificationInfo> notifications = doPerformSync(app);
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentText().toString(),
                equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
        assertThat(n.info.getContentTitle().toString(),
                equalTo("49 new sounds"));

        assertThat(n.getIntent().getStringExtra("tabTag"), equalTo("incoming"));
    }


    @Test
    public void shouldNotifyAboutIncomingAndExclusives() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("exclusives_1.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);

        List<NotificationInfo> notifications = doPerformSync(app);
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo("53 new sounds"));

        assertThat(n.info.getContentText().toString(),
                equalTo("exclusives from jberkel_testing, xla and others"));

        assertThat(app.getAccountDataInt(User.DataKeys.NOTIFICATION_COUNT_INCOMING), is(53));
        assertThat(n.getIntent().getStringExtra("tabTag"), equalTo("exclusive"));
    }

    @Test
    public void shouldNotifyAboutActivity() throws Exception {
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("own_1.json"));
        addPendingHttpResponse(200, resource("own_2.json"));

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);

        List<NotificationInfo> notifications = doPerformSync(app);
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo("42 new activities"));

        assertThat(n.info.getContentText().toString(),
                equalTo("Comments and likes from Paul Ko, jensnikolaus and others"));

        assertThat(app.getAccountDataInt(User.DataKeys.NOTIFICATION_COUNT_OWN), is(42));
        assertThat(n.getIntent().getStringExtra("tabTag"), equalTo("activity"));
    }

    @Test
    public void shouldNotifyAboutActivityFavoriting() throws Exception {
        assertNotification("own_one_favoriting.json",
                "A new like",
                "Paul Ko likes P. Watzlawick - Anleitung zum Unglücklichsein");

        assertNotification("own_two_favoritings.json",
                "2 new likes",
                "on P. Watzlawick - Anleitung zum Unglücklichsein");

        assertNotification("own_multi_favoritings",
                "3 new likes",
                "on P. Watzlawick - Anleitung zum Unglücklichsein, William Gibson & Cory Doctorow on 'Zero History'"+
                " and other sounds");
    }


    @Test
    public void shouldNotify99PlusItems() throws Exception {
        addPendingHttpResponse(200, resource("incoming_1.json"));
        addPendingHttpResponse(200, resource("incoming_2.json"));
        addPendingHttpResponse(200, resource("exclusives_1.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);

        List<NotificationInfo> notifications = doPerformSync(app);
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo("99+ new sounds"));
    }

    @Test
    public void shouldNotNotifyOnFirstSync() throws Exception {
        addPendingHttpResponse(200, resource("incoming_2.json"));
        assertThat(doPerformSync(DefaultTestRunner.application).size(), is(0));
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

    private static List<NotificationInfo> doPerformSync(SoundCloudApplication app)
            throws OperationCanceledException {
        SyncAdapterService.performSync(
                app,
                new Account("foo", "bar"),
                null, null, null);

        ShadowNotificationManager m =shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        List<NotificationInfo> list = new ArrayList<NotificationInfo>();
        for (Notification n : m.getAllNotifications()) {
            list.add(new NotificationInfo(n, shadowOf(n).getLatestEventInfo()));
        }

        return list;
    }

    private void assertNotification(String resource, String title, String content) throws Exception {
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource("empty_events.json"));
        addPendingHttpResponse(200, resource(resource));

        SoundCloudApplication app = DefaultTestRunner.application;
        app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1l);
        app.setAccountData(User.DataKeys.NOTIFICATION_COUNT_OWN, 0l);
        List<NotificationInfo> notifications = doPerformSync(app);
        assertThat(notifications.size(), is(1));
        NotificationInfo n = notifications.get(0);
        assertThat(n.info.getContentTitle().toString(),
                equalTo(title));

        assertThat(n.info.getContentText().toString(),
                equalTo(content));
    }
}

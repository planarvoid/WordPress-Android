package com.soundcloud.android.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.ApiTests;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@RunWith(DefaultTestRunner.class)
public class SyncAdapterServiceTest extends ApiTests {
    @Before
    public void before() {
        Robolectric.application.onCreate();
    }

    @Test
    public void testGetNewIncomingEvents() throws Exception {
        addPendingHttpResponse(200, resource("tracks_1.json"));
        addPendingHttpResponse(200, resource("tracks_2.json"));

        List<Event> events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application, 0, false);

        assertThat(events.size(), is(100));
    }

    @Test
    public void testWithSince() throws Exception {
        addPendingHttpResponse(200, resource("tracks_1.json"));
        List<Event> events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application,
                1310462679000l
                , false);

        assertThat(events.size(), is(1));

        addPendingHttpResponse(200, resource("tracks_1.json"));
        events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application,
                1310462016000l
                , false);

        assertThat(events.size(), is(2));
    }


    @Test
    public void testGetUniqueUsersFromEvents() throws Exception {
        addPendingHttpResponse(200, resource("tracks_2.json"));

        List<Event> events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application, 0, false);
        assertThat(events.size(), is(50));

        List<User> users = SyncAdapterService.getUniqueUsersFromEvents(events);
        assertThat(users.size(), is(31));

        Set<Long> ids = new HashSet<Long>();
        for (User u : users) ids.add(u.id);
        assertThat(ids.size(), is(users.size()));
    }


    @Test
    public void testIncomingMessaging() throws Exception {

        addPendingHttpResponse(200, resource("tracks_2.json"));

        List<Event> events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application, 0, false);

        String message = SyncAdapterService.getIncomingMessaging(
                (SoundCloudApplication) Robolectric.application, events);

        assertThat(message, equalTo("from All Tomorrows Parties, DominoRecordCo and others"));
    }

    @Test
    public void testExclusiveMessaging() throws Exception {

        addPendingHttpResponse(200, resource("tracks_2.json"));
        List<Event> events = SyncAdapterService.getNewIncomingEvents(
                (SoundCloudApplication) Robolectric.application, 0, false);

        String message = SyncAdapterService.getExclusiveMessaging(
                (SoundCloudApplication) Robolectric.application, events);

        assertThat(message, equalTo("exclusives from All Tomorrows Parties, DominoRecordCo and others"));
    }


    @Test @Ignore
    public void testPerformSync() throws Exception {
        addPendingHttpResponse(200, resource("tracks_2.json"));

        SyncAdapterService.performSync(
                (SoundCloudApplication) Robolectric.application,
                Robolectric.application,
                new Account("foo", "bar"),
                new Bundle(),
                null, null, null);

        ShadowNotificationManager m =shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        List<Notification> list = m.getAllNotifications();
        assertThat(list.size(), is(1));
        Notification n = list.get(0);
        assertThat(n.tickerText.toString(), equalTo("50 new sounds"));
    }
}

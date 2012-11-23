package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotification;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
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


/**
 * Common SyncAdapter test code infrastructure.
 */
@RunWith(DefaultTestRunner.class)
public abstract class SyncAdapterServiceTestBase {
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

    protected static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime, @Nullable Bundle extras)
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

    protected void addCannedActivities(String... resources) throws IOException {
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class, resources);
    }

    protected void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }
}

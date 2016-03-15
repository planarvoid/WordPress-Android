package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.likes.MyLikesStateProvider;
import com.soundcloud.android.sync.stream.SoundStreamNotifier;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.users.UserAssociationStorage;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowNotification;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.content.TestIntentSender;
import android.os.Bundle;

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
        TestHelper.setBackgrounData(true);
        TestHelper.connectedViaWifi(true);

        TestHelper.setUserId(133201L);
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
    }

    protected static SyncOutcome doPerformSyncWithValidToken(SoundCloudApplication app, boolean firstTime, @Nullable Bundle extras) throws Exception {
        Token token = mock(Token.class);
        when(token.valid()).thenReturn(true);
        return doPerformSync(app, firstTime, extras, token);
    }

    protected static SyncOutcome doPerformSync(SoundCloudApplication app, boolean firstTime, @Nullable Bundle extras,
                                               Token token)
            throws Exception {
        if (!firstTime) {
            ContentStats.setLastSeen(app, Content.ME_SOUND_STREAM, 1);
        }
        if (extras == null) {
            extras = new Bundle();
        }

        ShadowNotificationManager m = shadowOf((NotificationManager)
                Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));
        m.cancelAll();
        SyncResult result = new SyncResult();

        final SyncServiceResultReceiver.Factory syncServiceResultReceiverFactory = new SyncServiceResultReceiver.Factory(app,
                Mockito.mock(SyncStateManager.class),
                new ContentStats(app),
                Mockito.mock(SyncConfig.class));

        SyncAdapterService.performSync(
                app,
                extras, result, token,
                Mockito.mock(UserAssociationStorage.class),
                null,
                Mockito.mock(SyncStateManager.class),
                syncServiceResultReceiverFactory,
                Mockito.mock(MyLikesStateProvider.class),
                Mockito.mock(PlaylistStorage.class),
                Mockito.mock(SyncConfig.class));

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

        List<NotificationInfo> list = new ArrayList<>();
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
        TestHelper.addPendingHttpResponse(SyncAdapterServiceTest.class, resources);
    }

}

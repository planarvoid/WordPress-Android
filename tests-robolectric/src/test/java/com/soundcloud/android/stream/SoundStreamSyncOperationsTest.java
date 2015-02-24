package com.soundcloud.android.stream;

import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamSyncOperationsTest {


    private static final long LAST_SEEN = 1000L;
    private static final Urn USER_URN = Urn.forUser(123L);
    private SoundStreamSyncOperations syncOperations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private AccountOperations accountOperations;
    @Mock private Context appContext;
    @Mock private ContentStats contentStats;
    @Mock private StreamNotificationBuilder streamNotificationBuilder;
    @Mock private NotificationManager notificationManager;
    @Mock private Notification notification;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        when(appContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        syncOperations = new SoundStreamSyncOperations(soundStreamStorage, accountOperations, appContext, streamNotificationBuilder, contentStats);
    }

    @Test
    public void createNotificationForUnseenItemsDoesNothingWithNoUnseenItems() throws Exception {
        when(contentStats.getLastSeen(Content.ME_SOUND_STREAM)).thenReturn(LAST_SEEN);
        when(soundStreamStorage.loadStreamItemsSince(LAST_SEEN, USER_URN, SoundStreamSyncOperations.MAX_NOTIFICATION_ITEMS)).thenReturn(new ArrayList<PropertySet>());

        syncOperations.createNotificationForUnseenItems();

        verifyZeroInteractions(streamNotificationBuilder);
    }

    @Test
    public void createNotificationForUnseenItemsDoesNothingWithNoNewerItemsThanLastNotified() throws Exception {
        final List<PropertySet> unseenItems = Arrays.asList(PropertySet.from(PlayableProperty.CREATED_AT.bind(new Date(1000L))));
        when(contentStats.getLastSeen(Content.ME_SOUND_STREAM)).thenReturn(LAST_SEEN);
        when(contentStats.getLastNotified(Content.ME_SOUND_STREAM)).thenReturn(LAST_SEEN);
        when(soundStreamStorage.loadStreamItemsSince(LAST_SEEN, USER_URN, SoundStreamSyncOperations.MAX_NOTIFICATION_ITEMS)).thenReturn(unseenItems);

        syncOperations.createNotificationForUnseenItems();

        verifyZeroInteractions(streamNotificationBuilder);
    }

    @Test
    public void createNotificationForUnseenItemsWithNewerItemThanLastSeenCreatesNotification() throws Exception {
        setupUnseenNotification(Arrays.asList(PropertySet.from(PlayableProperty.CREATED_AT.bind(new Date(1001L)))));

        syncOperations.createNotificationForUnseenItems();

        verify(notificationManager).notify(NotificationConstants.DASHBOARD_NOTIFY_STREAM_ID, notification);
    }

    @Test
    public void createNotificationForUnseenItemsWithNewerItemThanLastSeenSetsLastNotifiedTimestamp() throws Exception {
        setupUnseenNotification(Arrays.asList(PropertySet.from(PlayableProperty.CREATED_AT.bind(new Date(1001L)))));

        syncOperations.createNotificationForUnseenItems();

        verify(contentStats).setLastNotified(same(Content.ME_SOUND_STREAM), gt(0L));
    }

    @Test
    public void createNotificationForUnseenItemsWithNewerItemThanLastSeenSetsLastNotifiedItem() throws Exception {
        setupUnseenNotification(Arrays.asList(PropertySet.from(PlayableProperty.CREATED_AT.bind(new Date(1001L)))));

        syncOperations.createNotificationForUnseenItems();

        verify(contentStats).setLastNotifiedItem(same(Content.ME_SOUND_STREAM), eq(1001L));
    }

    private void setupUnseenNotification(List<PropertySet> unseenItems) {
        when(contentStats.getLastSeen(Content.ME_SOUND_STREAM)).thenReturn(LAST_SEEN);
        when(contentStats.getLastNotified(Content.ME_SOUND_STREAM)).thenReturn(LAST_SEEN);
        when(soundStreamStorage.loadStreamItemsSince(LAST_SEEN, USER_URN, SoundStreamSyncOperations.MAX_NOTIFICATION_ITEMS)).thenReturn(unseenItems);
        when(streamNotificationBuilder.notification(unseenItems)).thenReturn(Observable.just(notification));
    }
}
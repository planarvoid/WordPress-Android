package com.soundcloud.android.stream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import javax.inject.Inject;
import java.util.List;

public class SoundStreamSyncOperations {

    @VisibleForTesting
    static final int MAX_NOTIFICATION_ITEMS = 3;

    private final Context appContext;
    private final SoundStreamStorage soundStreamStorage;
    private final StreamNotificationBuilder streamNotificationBuilder;
    private final ContentStats contentStats;

    private final DefaultSubscriber<Notification> notificationSubscriber = new DefaultSubscriber<Notification>() {
        @Override
        public void onNext(Notification args) {
            ((NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NotificationConstants.DASHBOARD_NOTIFY_STREAM_ID, args);
        }
    };

    @Inject
    SoundStreamSyncOperations(SoundStreamStorage soundStreamStorage,
                              Context appContext, RichStreamNotificationBuilder streamNotificationBuilder,
                              ContentStats contentStats) {
        this.soundStreamStorage = soundStreamStorage;
        this.appContext = appContext;
        this.streamNotificationBuilder = streamNotificationBuilder;
        this.contentStats = contentStats;
    }

    public boolean createNotificationForUnseenItems(){
        final long lastStreamSeen = contentStats.getLastSeen(Content.ME_SOUND_STREAM);
        final List<PropertySet> itemsSince = soundStreamStorage.loadStreamItemsSince(lastStreamSeen, MAX_NOTIFICATION_ITEMS);
        final boolean hasItems = !itemsSince.isEmpty();

        if (hasItems) {
            final long lastNotifiedItem = contentStats.getLastNotifiedItem(Content.ME_SOUND_STREAM);
            if (hasItemNewerThan(itemsSince, lastNotifiedItem)) {
                streamNotificationBuilder.notification(itemsSince).subscribe(notificationSubscriber);
                setLastNotifiedTimes(itemsSince);
                return true;

            } else {
                Log.d("no new items, skip track notfication");
            }
        } else {
            Log.d("no new items, skip track notfication");
        }
        return false;
    }

    private void setLastNotifiedTimes(List<PropertySet> streamItems) {
        contentStats.setLastNotified(Content.ME_SOUND_STREAM, System.currentTimeMillis());
        contentStats.setLastNotifiedItem(Content.ME_SOUND_STREAM, getNewestStreamItemCreatedAt(streamItems));
    }

    private boolean hasItemNewerThan(List<PropertySet> streamItems, final long lastNotified) {
        return Iterables.tryFind(streamItems, new Predicate<PropertySet>() {
            @Override
            public boolean apply(PropertySet bindings) {
                return bindings.get(PlayableProperty.CREATED_AT).getTime() > lastNotified;
            }
        }).isPresent();
    }

    private long getNewestStreamItemCreatedAt(List<PropertySet> streamItems) {
        return streamItems.get(0).get(PlayableProperty.CREATED_AT).getTime();
    }
}

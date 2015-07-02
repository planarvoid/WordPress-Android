package com.soundcloud.android.stream;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import javax.inject.Provider;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

public class StreamNotificationBuilder {

    private static final int NOTIFICATION_MAX = 100;

    protected final Context appContext;
    private final Provider<Builder> builderProvider;
    private final int notificationMax;

    public StreamNotificationBuilder(Context appContext, Provider<Builder> builderProvider) {
        this(appContext, builderProvider, NOTIFICATION_MAX);
    }

    public StreamNotificationBuilder(Context appContext, Provider<Builder> builderProvider, int notificationMax) {
        this.appContext = appContext;
        this.builderProvider = builderProvider;
        this.notificationMax = notificationMax;
    }

    public Observable<Notification> notification(List<PropertySet> streamItems) {
        return Observable.just(getBuilder(streamItems).build());
    }

    protected Builder getBuilder(List<PropertySet> streamItems) {

        final PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 0, getIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        final CharSequence ticker = getTicker(streamItems);
        final CharSequence title = getTitle(streamItems);
        final CharSequence message = getIncomingNotificationMessage(streamItems);

        Builder builder = builderProvider.get();
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setTicker(ticker);
        builder.setContentTitle(title);
        builder.setContentText(message);
        return builder;
    }

    private Intent getIntent() {
        final Intent intent = new Intent(Actions.STREAM)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_REFRESH_STREAM, true);
        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.STREAM_NOTIFICATION.addToIntent(intent);
        return intent;
    }

    protected CharSequence getTitle(List<PropertySet> streamItems) {
        return appContext.getResources().getQuantityString(R.plurals.dashboard_notifications_title,
                streamItems.size(),
                getNotificationCount(streamItems));
    }

    protected CharSequence getTicker(List<PropertySet> streamItems) {
        return appContext.getResources().getQuantityString(R.plurals.dashboard_notifications_ticker,
                streamItems.size(),
                getNotificationCount(streamItems));
    }

    private String getNotificationCount(List<PropertySet> streamItems) {
        return String.valueOf(streamItems.size() > notificationMax ? notificationMax + "+" : streamItems.size());
    }

    /* package */
    public String getIncomingNotificationMessage(List<PropertySet> streamItems) {
        LinkedHashSet<String> uniqueUsers = getUniqueUsersFromStreamItems(streamItems);
        final Iterator<String> iterator = uniqueUsers.iterator();
        switch (uniqueUsers.size()) {
            case 1:
                return appContext.getString(R.string.dashboard_notifications_message_incoming, iterator.next());
            case 2:
                return appContext.getString(R.string.dashboard_notifications_message_incoming_2,
                        iterator.next(), iterator.next());
            default:
                return appContext.getString(R.string.dashboard_notifications_message_incoming_others,
                        iterator.next(), iterator.next());
        }
    }

    private LinkedHashSet<String> getUniqueUsersFromStreamItems(List<PropertySet> streamItems) {
        final LinkedHashSet<String> usernames = new LinkedHashSet();
        for (PropertySet bindings : streamItems) {
            usernames.add(bindings.get(PlayableProperty.CREATOR_NAME));
        }
        return usernames;
    }
}

package com.soundcloud.android.main;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.navigation.PendingIntentFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.NotificationCompat;

import javax.inject.Inject;

class DevEventLoggerMonitorNotificationController {

    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences sharedPreferences;

    @Inject
    DevEventLoggerMonitorNotificationController(Context context, NotificationManager notificationManager, SharedPreferences sharedPreferences) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.sharedPreferences = sharedPreferences;
    }

    void startMonitoring() {
        setMonitorMuteAction(false);
    }

    void stopMonitoring() {
        setMonitorMute(true);
        notificationManager.cancel(NotificationConstants.DEV_DRAWER_EVENT_LOGGER_MONITOR);
    }

    void setMonitorMuteAction(boolean monitorMute) {
        setMonitorMute(monitorMute);
        notificationManager.notify(NotificationConstants.DEV_DRAWER_EVENT_LOGGER_MONITOR, getNotification());
    }

    private Notification getNotification() {
        final boolean monitorMute = sharedPreferences.getBoolean(context.getString(R.string.dev_event_logger_monitor_mute_key), false);
        final PendingIntent notificationPendingIntent = PendingIntentFactory.createDevEventLoggerMonitorIntent(context);
        final PendingIntent actionPendingIntent = PendingIntentFactory.createDevEventLoggerMonitorReceiverIntent(context, monitorMute);
        final CharSequence actionTitle = monitorMute
                                         ? context.getString(R.string.dev_notification_event_logger_monitor_action_title_unmute)
                                         : context.getString(R.string.dev_notification_event_logger_monitor_action_title_mute);
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification_cloud)
                .setOngoing(true)
                .setContentTitle(context.getString(R.string.dev_notification_event_logger_monitor_title))
                .setContentIntent(notificationPendingIntent)
                .addAction(new NotificationCompat.Action.Builder(android.R.drawable.presence_audio_away, actionTitle, actionPendingIntent).build())
                .build();
    }

    private void setMonitorMute(boolean monitorMute) {
        sharedPreferences.edit()
                         .putBoolean(context.getString(R.string.dev_event_logger_monitor_mute_key), monitorMute)
                         .apply();
    }
}

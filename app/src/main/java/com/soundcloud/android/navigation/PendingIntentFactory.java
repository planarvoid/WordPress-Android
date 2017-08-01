package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHomeIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOfflineSettingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileIntent;
import static com.soundcloud.android.policies.DailyUpdateService.ACTION_START;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.AlarmManagerReceiver;
import com.soundcloud.android.settings.ChangeStorageLocationActivity;
import com.soundcloud.android.storage.DatabaseCleanupService;
import com.soundcloud.java.optional.Optional;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class PendingIntentFactory {

    private static final int POLICY_UPDATE_REQUEST_ID = R.id.policy_update_request_id;
    private static final int DATABASE_CLEANUP_REQUEST_ID = R.id.database_cleanup_request_id;
    private static final int NO_FLAGS = 0;

    public static PendingIntent createUpdateSchedulerIntent(Context context, int flag) {
        Intent intent = new Intent(context, AlarmManagerReceiver.class);
        intent.setAction(ACTION_START);
        return PendingIntent.getBroadcast(context, POLICY_UPDATE_REQUEST_ID, intent, flag);
    }

    public static PendingIntent createCleanupSchedulerIntent(Context context, int flag) {
        return PendingIntent.getService(context, DATABASE_CLEANUP_REQUEST_ID, DatabaseCleanupService.createIntent(context), flag);
    }

    public static PendingIntent createPendingOfflineSettings(Context context) {
        return PendingIntent.getActivity(context, 0, createOfflineSettingsIntent(context), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent createPendingChangeStorageLocation(Context context) {
        final Intent intent = new Intent(context, ChangeStorageLocationActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent createPendingHomeIntent(Context context) {
        Intent intent = createHomeIntent(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent createPendingCollectionIntent(Context context) {
        Intent intent = createCollectionIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent openProfileFromNotification(Context context, Urn user) {
        return PendingIntent.getActivity(context,
                                         NO_FLAGS,
                                         createProfileIntent(context, user, Optional.of(Screen.NOTIFICATION), Optional.absent(), Optional.absent())
                                                 .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK),
                                         PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent openProfileFromWidget(Context context, Urn user, int requestCode) {
        return PendingIntent.getActivity(context,
                                         requestCode,
                                         createProfileIntent(context, user, Optional.of(Screen.WIDGET), Optional.absent(), Optional.of(Referrer.PLAYBACK_WIDGET)),
                                         PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent createDevEventLoggerMonitorReceiverIntent(Context context, boolean monitorMute) {
        return PendingIntent.getBroadcast(context, 0, IntentFactory.createDevEventLoggerMonitorReceiverIntent(context, monitorMute), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent createDevEventLoggerMonitorIntent(Context context) {
        return PendingIntent.getActivity(context, 0, IntentFactory.createDevEventLoggerMonitorIntent(context), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntentFactory() {
        // hidden
    }
}

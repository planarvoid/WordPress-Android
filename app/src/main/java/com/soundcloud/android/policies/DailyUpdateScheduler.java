package com.soundcloud.android.policies;

import static com.soundcloud.android.policies.DailyUpdateService.ACTION_START;
import static com.soundcloud.android.policies.DailyUpdateService.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.AlarmManagerReceiver;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class DailyUpdateScheduler {

    static final int REQUEST_ID = R.id.policy_update_request_id;
    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
    static final long POLICY_UPDATE_DELAY = TimeUnit.DAYS.toMillis(1);

    private final Context context;
    private final AlarmManager alarmManager;
    private final DateProvider dateProvider;
    private final PendingIntentFactory pendingIntentFactory;

    @Inject
    public DailyUpdateScheduler(Context context, AlarmManager alarmManager, CurrentDateProvider dateProvider,
                                PendingIntentFactory pendingIntentFactory) {
        this.context = context;
        this.alarmManager = alarmManager;
        this.dateProvider = dateProvider;
        this.pendingIntentFactory = pendingIntentFactory;
    }

    public void schedule() {
        if (!isNextUpdateAlreadyScheduled()) {
            Log.d(TAG, "Scheduling new policy update");
            final PendingIntent intent = pendingIntentFactory.getPendingIntent(context,
                                                                               PendingIntent.FLAG_UPDATE_CURRENT);
            final long initialDelay = dateProvider.getCurrentTime() + POLICY_UPDATE_DELAY;
            alarmManager.setInexactRepeating(ALARM_TYPE, initialDelay, AlarmManager.INTERVAL_DAY, intent);
        }
    }

    private boolean isNextUpdateAlreadyScheduled() {
        return pendingIntentFactory.getPendingIntent(context, PendingIntent.FLAG_NO_CREATE) != null;
    }

    public static class PendingIntentFactory {

        @Inject
        PendingIntentFactory() {
            /* no -op */
        }

        public PendingIntent getPendingIntent(Context context, int flag) {
            Intent intent = new Intent(context, AlarmManagerReceiver.class);
            intent.setAction(ACTION_START);
            return PendingIntent.getBroadcast(context, REQUEST_ID, intent, flag);
        }

    }
}
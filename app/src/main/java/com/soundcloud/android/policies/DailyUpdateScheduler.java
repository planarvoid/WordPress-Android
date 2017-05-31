package com.soundcloud.android.policies;

import static com.soundcloud.android.policies.DailyUpdateService.TAG;

import com.soundcloud.android.navigation.PendingIntentFactory;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class DailyUpdateScheduler {

    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
    static final long POLICY_UPDATE_DELAY = TimeUnit.DAYS.toMillis(1);

    private final Context context;
    private final AlarmManager alarmManager;
    private final DateProvider dateProvider;

    @Inject
    DailyUpdateScheduler(Context context,
                         AlarmManager alarmManager,
                         CurrentDateProvider dateProvider) {
        this.context = context;
        this.alarmManager = alarmManager;
        this.dateProvider = dateProvider;
    }

    public void schedule() {
        if (!isNextUpdateAlreadyScheduled()) {
            Log.d(TAG, "Scheduling new policy update");
            final PendingIntent intent = PendingIntentFactory.createUpdateSchedulerIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
            final long initialDelay = dateProvider.getCurrentTime() + POLICY_UPDATE_DELAY;
            alarmManager.setInexactRepeating(ALARM_TYPE, initialDelay, AlarmManager.INTERVAL_DAY, intent);
        }
    }

    private boolean isNextUpdateAlreadyScheduled() {
        return PendingIntentFactory.createUpdateSchedulerIntent(context, PendingIntent.FLAG_NO_CREATE) != null;
    }
}

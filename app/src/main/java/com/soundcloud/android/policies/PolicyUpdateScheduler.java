package com.soundcloud.android.policies;

import static com.soundcloud.android.policies.PolicyUpdateService.ACTION_START;
import static com.soundcloud.android.policies.PolicyUpdateService.TAG;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.AlarmManagerReceiver;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateScheduler {

    static final int REQUEST_ID = R.id.policy_pdate_request_id;
    static final int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
    static final long POLICY_UPDATE_DELAY = TimeUnit.HOURS.toMillis(24);

    private final Context context;
    private final AlarmManager alarmManager;
    private final DateProvider dateProvider;
    private final PendingIntentFactory pendingIntentFactory;

    @Inject
    public PolicyUpdateScheduler(Context context, AlarmManager alarmManager, DateProvider dateProvider,
                                 PendingIntentFactory pendingIntentFactory) {
        this.context = context;
        this.alarmManager = alarmManager;
        this.dateProvider = dateProvider;
        this.pendingIntentFactory = pendingIntentFactory;
    }

    public void scheduleDailyPolicyUpdates() {
        if (!isNextUpdateAlreadyScheduled()) {
            Log.d(TAG, "Scheduling new policy update");
            final PendingIntent intent = pendingIntentFactory.getPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setInexactRepeating(ALARM_TYPE, dateProvider.getCurrentTime(), POLICY_UPDATE_DELAY, intent);
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

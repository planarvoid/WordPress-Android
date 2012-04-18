package com.soundcloud.android.activity.settings;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlaylistManager;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.TimePicker;

public final class AlarmClock {
    public static final String TAG = AlarmClock.class.getSimpleName();
    public static final String DEFAULT_URI   = Content.ME_SOUND_STREAM.uri.toString();
    public static final int NOTIFICATION_ID  = 9999;

    public static final String KEY     = "dev.alarm";
    public static final String DEFAULT = "10:00";

    public static final String EXTRA_URI = "uri";
    public static final String PREF_ALARM_CLOCK_ENABLED = "dev.alarmClock.enabled";

    private Context mContext;
    private int mHour, mMinute;

    /* package */ AlarmClock(Context context) {
        this.mContext = context;
    }

    public boolean set(int hour, int minute) {
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("invalid hour: " + hour);
        if (minute < 0 || minute > 59) throw new IllegalArgumentException("invalid minute: " + minute);

        mHour = hour;
        mMinute = minute;

        // cancel any previous alarm
        cancel();

        AlarmManager mgr = getAlarmManager();
        Time now = new Time();
        now.setToNow();
        Time alarm = getAlarmTime(now);

        Log.d(TAG, "setting alarm to: " + alarm.format2445());
        mgr.set(AlarmManager.RTC_WAKEUP, alarm.toMillis(false), getAlarmIntent(null));
        double in = (alarm.toMillis(false) - now.toMillis(false)) / 1000d;
        CloudUtils.showToast(mContext, R.string.dev_alarm_in,
                CloudUtils.getTimeString(mContext.getResources(), in, false));

        final String message = mContext.getString(R.string.dev_alarm_set, alarm.format("%k:%M"));
        getNotifications().notify(NOTIFICATION_ID, createNotification(message));
        setAlarmMessage(message);
        saveToPrefs();
        return true;
    }

    public void rescheduleDelayed(int seconds) {
        if (seconds > 0) {
            AlarmManager mgr = getAlarmManager();
            mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+(seconds*1000), getAlarmIntent(null));
        }
    }

    public boolean cancel() {
        getNotifications().cancel(NOTIFICATION_ID);
        getAlarmManager().cancel(getAlarmIntent(null));
        setAlarmMessage("");
        return true;
    }

    public void showDialog() {
        loadFromPrefs();

        new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                set(hourOfDay, minute);
            }
        }, mHour, mMinute, true)
        .show();
    }


    /* package */ void play(Context context, Uri uri) {
        // TODO: should be handled via intent parameter
        PlaylistManager.clearLastPlayed(context);

        if (!IOUtils.isConnected(context)) {
            // just use cached items if there is no network connection
            uri = uri.buildUpon().appendQueryParameter(
                    ScContentProvider.Parameter.CACHED, "1").build();
        }
        context.startService(new Intent(
                context,
                CloudPlaybackService.class)
                .setAction(CloudPlaybackService.PLAY)
                .setData(uri)
                .putExtra(CloudPlaybackService.EXTRA_UNMUTE, true));
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ALARM_CLOCK_ENABLED, false);
    }

    private Time getAlarmTime(Time now) {
        final Time alarm = new Time();
        alarm.set(0, mMinute, mHour, now.monthDay, now.month, now.year);
        if (alarm.before(now)) {
            alarm.set(alarm.toMillis(false) + 86400 * 1000);
        }
        return alarm;
    }

    private void loadFromPrefs() {
        final String alarm = PreferenceManager
                .getDefaultSharedPreferences(mContext)
                .getString(KEY, DEFAULT);

        if (!TextUtils.isEmpty(alarm)) {
            String[] components = alarm.split(":", 2);
            if (components != null && components.length == 2) {
                mHour = Integer.parseInt(components[0]);
                mMinute = Integer.parseInt(components[1]);
            }
        }
    }

    private void saveToPrefs() {
        PreferenceManager
                .getDefaultSharedPreferences(mContext)
                .edit()
                .putString(KEY, mHour + ":" + mMinute)
                .commit();
    }

    private PendingIntent getCancelAlarmIntent() {
        return PendingIntent.getBroadcast(mContext, 0,
                new Intent(Actions.CANCEL_ALARM), 0);
    }

    private PendingIntent getAlarmIntent(Uri uri) {
        return PendingIntent.getBroadcast(mContext, 0,
                new Intent(Actions.ALARM)
                        .putExtra(EXTRA_URI, uri), 0);
        // NB: uri is not set with setData() to make intent cancelable
        // http://developer.android.com/reference/android/content/Intent.html#filterEquals%28android.content.Intent%29
    }

    private Notification createNotification(String message) {
        String title = "Alarm set";
        final Notification n = new Notification(R.drawable.ic_status,
                title, System.currentTimeMillis());
        n.setLatestEventInfo(mContext,
                title,
                message + ", tap to cancel.",
                getCancelAlarmIntent());

        n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return n;
    }

    private Uri getPlayUri() {
        String uri = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(Settings.ALARM_CLOCK_URI, DEFAULT_URI);
        return Uri.parse(uri);
    }

    private void setAlarmMessage(String message) {
        // set lock screen info + status bar icon
        android.provider.Settings.System.putString(mContext.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED, message);

        setStatusBarIcon(!TextUtils.isEmpty(message));
    }

    private void setStatusBarIcon(boolean enabled) {
        Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
        alarmChanged.putExtra("alarmSet", enabled);
        mContext.sendBroadcast(alarmChanged);
    }

    private boolean disableAirplaneMode() {
        // switch off airplane mode if enabled
        final boolean isEnabled = android.provider.Settings.System.getInt(
                mContext.getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0) == 1;

        if (isEnabled) {
            android.provider.Settings.System.putInt(
                mContext.getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0);

            mContext.sendBroadcast(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                            .putExtra("state", false));
            return true;
        } else return false;
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    private NotificationManager getNotifications() {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static final class Receiver extends BroadcastReceiver {
        public static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (SECRET_CODE_ACTION.equals(action)) {
                toggleAlarmClockEnabled(context);
            } else if (Actions.ALARM.equals(action)) {
                onAlarm(context, intent);
            } else if (Actions.CANCEL_ALARM.equals(action)) {
                onAlarmCancel(context);
            } else {
                Log.w(TAG, "unhandled intent: "+intent);
            }
        }

        private void onAlarm(Context context, Intent intent) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            if (lock != null) lock.acquire();
            try {
                final AlarmClock alarm = new AlarmClock(context);
                alarm.cancel();
                if (alarm.disableAirplaneMode()) {
                    // if we had to disable airplane mode reschedule the alarm
                    // with a slight delay to allow the connection to come up
                    alarm.rescheduleDelayed(15);
                } else {
                    Uri uri = intent.getParcelableExtra(EXTRA_URI);
                    if (uri == null) {
                        uri = alarm.getPlayUri();
                    }
                    if (uri != null) {
                        Log.d(TAG, "alarm with uri=" + uri);
                        alarm.play(context, uri);
                    } else {
                        // TODO: should have some fallback here
                        Log.w(TAG, "no uri found, no alarm");
                    }
                }
            } finally {
                if (lock != null) lock.release();
            }
        }

        private void onAlarmCancel(Context context) {
            new AlarmClock(context).cancel();
        }

        private void toggleAlarmClockEnabled(Context context) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean newState = !prefs.getBoolean(PREF_ALARM_CLOCK_ENABLED, false);
            prefs.edit().putBoolean(PREF_ALARM_CLOCK_ENABLED, newState).commit();
            CloudUtils.showToast(context, "SC AlarmClock " + (newState ? "enabled" : "disabled"));
        }
    }
}

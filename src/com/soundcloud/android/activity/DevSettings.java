package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlaylistManager;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public final class DevSettings {
    public static final String PREF_KEY = "dev-settings";

    private DevSettings() {
    }

    public static void setup(final PreferenceActivity activity, final SoundCloudApplication app) {
        activity.findPreference("dev.clearNotifications").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, SyncAdapterService.CLEAR_ALL);
                        return true;
                    }
                });

        activity.findPreference("dev.rewindNotifications").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, SyncAdapterService.REWIND_LAST_DAY);
                        return true;
                    }
                });


        activity.findPreference("dev.syncNow").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SyncAdapterService.requestNewSync(app, -1);
                        return true;
                    }
                });


        activity.findPreference("dev.crash").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if (!CloudUtils.isUserAMonkey()) {
                            throw new RuntimeException("developer requested crash");
                        } else {
                            return true;
                        }
                    }
                });

        activity.findPreference("dev.http.proxy").setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object s) {
                        if (!TextUtils.isEmpty(s.toString())) {
                            try {
                                URL proxy = new URL(s.toString());
                                if (!"https".equals(proxy.getProtocol()) &&
                                        !"http".equals(proxy.getProtocol())) {
                                    throw new MalformedURLException("Need http/https url");
                                }
                            } catch (MalformedURLException e) {
                                Toast.makeText(activity, R.string.pref_dev_http_proxy_invalid_url, Toast.LENGTH_SHORT).show();
                                return false;
                            }
                        }
                        final Intent intent = new Intent(Actions.CHANGE_PROXY_ACTION);
                        if (!TextUtils.isEmpty(s.toString())) intent.putExtra("proxy", s.toString());
                        activity.sendBroadcast(intent);
                        return true;
                    }
                }
        );

        activity.findPreference("dev.alarmClock").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        new AlarmClock(activity).showDialog();
                        return true;
                    }
                }
        );

        SharedPreferencesUtils.listWithLabel(activity,
                R.string.pref_dev_alarm_play_uri,
                "dev.alarmClock.uri");
    }

    public static final class AlarmClock {
        public static final String TAG = AlarmClock.class.getSimpleName();
        public static final String PREF_URI      = "dev.alarmClock.uri";
        public static final String DEFAULT_URI = Content.ME_TRACKS.uri.toString();
        public static final int NOTIFICATION_ID = 9999;

        public static final String KEY = "dev.alarm";
        public static final String DEFAULT = "10:00";

        public static final String EXTRA_URI = "uri";

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
            CloudUtils.showToast(mContext, R.string.dev_alarm_set,
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
                    .getString(PREF_URI, DEFAULT_URI);
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
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                if (lock != null) lock.acquire();
                try {
                    final AlarmClock alarm = new AlarmClock(context);
                    if (Actions.ALARM.equals(intent.getAction())) {
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
                    } else if (Actions.CANCEL_ALARM.equals(intent.getAction())) {
                        alarm.cancel();
                    } else {
                        Log.w(TAG, "unhandled intent: "+intent);
                    }
                } finally {
                    if (lock != null) lock.release();
                }
            }
        }
    }
}

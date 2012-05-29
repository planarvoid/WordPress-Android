package com.soundcloud.android.activity.settings;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlaylistManager;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Constructor;

public final class AlarmClock {
    public static final String TAG = AlarmClock.class.getSimpleName();
    public static final String DEFAULT_URI   = Content.ME_SOUND_STREAM.uri.toString();

    public static final String KEY     = "dev.alarm";
    public static final String DEFAULT = "10:00";

    public static final String EXTRA_URI = "uri";


    private Context mContext;
    private int mHour, mMinute;

    private static AlarmClock sInstance;

    public static AlarmClock get(Context context) {
        if (sInstance == null) {
            sInstance = new AlarmClock(context.getApplicationContext());
            sInstance.loadFromPrefs();
        }
        return sInstance;
    }

    /* package */ AlarmClock(Context context) {
        this.mContext = context;
    }

    public boolean set(int hour, int minute) {
        if (hour < 0 || hour > 23) throw new IllegalArgumentException("invalid hour: " + hour);
        if (minute < 0 || minute > 59) throw new IllegalArgumentException("invalid minute: " + minute);

        mHour = hour;
        mMinute = minute;

        return setAlarm();
    }

    public boolean setAlarm() {
        // cancel any previous alarm
        cancel();

        AlarmManager mgr = getAlarmManager();
        Time now = new Time();
        now.setToNow();
        Time alarm = getAlarmTime(now);

        mgr.set(AlarmManager.RTC_WAKEUP, alarm.toMillis(false), getAlarmIntent(null));
        double in = (alarm.toMillis(false) - now.toMillis(false)) / 1000d;

        Toast toast = Toast.makeText(mContext, mContext.getString(R.string.dev_alarm_in,
                CloudUtils.getTimeString(mContext.getResources(), in, false)), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, toast.getXOffset() / 2, toast.getYOffset() / 2);
        toast.show();

        final String message = mContext.getString(R.string.dev_alarm_set, alarm.format("%k:%M"));
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
        getAlarmManager().cancel(getAlarmIntent(null));
        setAlarmMessage("");
        return true;
    }

    public void showDialog(Context context, final Runnable runnable) {
        new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mHour = hourOfDay;
                mMinute = minute;
                saveToPrefs();
                if (isAlarmSet()) {
                    setAlarm();
                }
                if (runnable != null) runnable.run();
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
                .setAction(CloudPlaybackService.PLAY_ACTION)
                .setData(uri)
                .putExtra(CloudPlaybackService.EXTRA_UNMUTE, true));
    }

    public static boolean isFeatureEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Consts.PrefKeys.DEV_ALARM_CLOCK_ENABLED, false);
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

    private PendingIntent getAlarmIntent(Uri uri) {
        return PendingIntent.getBroadcast(mContext, 0,
                new Intent(Actions.ALARM)
                        .putExtra(EXTRA_URI, uri), 0);
        // NB: uri is not set with setData() to make intent cancelable
        // http://developer.android.com/reference/android/content/Intent.html#filterEquals%28android.content.Intent%29
    }

    private Uri getPlayUri() {
        String uri = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(Consts.PrefKeys.DEV_ALARM_CLOCK_URI, DEFAULT_URI);
        return Uri.parse(uri);
    }

    private void setAlarmMessage(String message) {
        // set lock screen info + status bar icon
        android.provider.Settings.System.putString(mContext.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED, message);

        setStatusBarIcon(!TextUtils.isEmpty(message));
    }

    private boolean isAlarmSet() {
        // could be set by another alarm app
        return !TextUtils.isEmpty(
                android.provider.Settings.System.getString(mContext.getContentResolver(),
                android.provider.Settings.System.NEXT_ALARM_FORMATTED));
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

    /* package */ PreferenceGroup addPrefs(final Context context, final PreferenceGroup group) {
        boolean useICSSwitchPreference = false;
        final boolean isAlarmSet = isAlarmSet();
        Preference alarm = null;
        final Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    setAlarm();
                } else {
                    cancel();
                }
                return true;
            }
        };
        if (Build.VERSION.SDK_INT >= 14) {
            try {
                Class switchPrefClass = Class.forName("com.soundcloud.android.activity.settings.ICSSwitchPreference");
                Constructor ctor = switchPrefClass.getConstructor(Context.class, boolean.class);
                alarm = (Preference) ctor.newInstance(context, isAlarmSet);
                alarm.setOnPreferenceChangeListener(listener);
                alarm.setPersistent(false);
                useICSSwitchPreference = true;
            } catch (Exception ignored) {
                Log.w(TAG, ignored);
            }
        }
        if (alarm == null) {
            alarm = new Preference(context);
        }

        alarm.setTitle(context.getString(R.string.pref_dev_alarm_clock, mHour, mMinute));
        alarm.setSummary(R.string.pref_dev_alarm_clock_summary);
        final Preference theAlarm = alarm;
        alarm.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        showDialog(context, new Runnable() {
                            @Override
                            public void run() {
                                theAlarm.setTitle(context.getString(R.string.pref_dev_alarm_clock, mHour, mMinute));
                            }
                        });
                        return true;
                    }
                }
        );
        if (!useICSSwitchPreference) {
            CheckBoxPreference enabled = new CheckBoxPreference(context);
            enabled.setTitle(R.string.pref_dev_alarm_clock_enabled);
            enabled.setSummary(R.string.pref_dev_alarm_clock_enabled_summary);
            enabled.setPersistent(false);
            enabled.setOnPreferenceChangeListener(listener);
            enabled.setChecked(isAlarmSet);
            group.addPreference(enabled);
        }
        ListPreference alarmUri = new ListPreference(context);
        alarmUri.setValue(getPlayUri().toString());
        alarmUri.setKey(Consts.PrefKeys.DEV_ALARM_CLOCK_URI);
        alarmUri.setTitle(R.string.pref_dev_alarm_play_uri);
        alarmUri.setSummary(R.string.pref_dev_alarm_play_uri_summary);
        alarmUri.setEntries(R.array.dev_alarmClock_uri_entries);
        alarmUri.setEntryValues(R.array.dev_alarmClock_uri_values);
        SharedPreferencesUtils.listWithLabel(alarmUri, R.string.pref_dev_alarm_play_uri);

        group.addPreference(alarm);
        group.addPreference(alarmUri);
        return group;
    }

    public static final class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (Consts.SECRET_CODE_ACTION.equals(action)) {
                toggleAlarmClockFeatureEnabled(context);
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
                        fallbackAlarm(context);
                    }
                }
            } finally {
                if (lock != null) lock.release();
            }
        }

        private void fallbackAlarm(final Context context) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    playDefaultAlarm(context, 10);
                    Looper.loop();
                }
            }.start();
        }

        private void playDefaultAlarm(Context context, final int timeout) {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            final MediaPlayer mp = new MediaPlayer();
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    mp.stop();
                    mp.release();

                    Looper.myLooper().quit();
                }
            };
            try {
                mp.setDataSource(context, alert);
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
                mp.prepare();
                mp.setLooping(true);
                mp.start();

                handler.sendEmptyMessageDelayed(0, 1000 * timeout);
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }

        private void onAlarmCancel(Context context) {
            new AlarmClock(context).cancel();
        }

        private void toggleAlarmClockFeatureEnabled(Context context) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean newState = !prefs.getBoolean(Consts.PrefKeys.DEV_ALARM_CLOCK_ENABLED, false);
            prefs.edit().putBoolean(Consts.PrefKeys.DEV_ALARM_CLOCK_ENABLED, newState).commit();
            CloudUtils.showToast(context, "SC AlarmClock " + (newState ? "enabled" : "disabled"));
        }
    }
}

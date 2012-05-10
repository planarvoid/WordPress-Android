package com.soundcloud.android.utils;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CloudUtils {
    private CloudUtils() {}

    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\._%\\-\\+]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
    );

    private static HashMap<Context, HashMap<Class<? extends Service>, ServiceConnection>> sConnectionMap
            = new HashMap<Context, HashMap<Class<? extends Service>, ServiceConnection>>();


    public static ProgressDialog showProgress(Context context, int message) {
        return showProgress(context, message, 0);
    }

    /** I18N safe impl of {@link android.app.ProgressDialog#show()} */
    @SuppressWarnings("JavaDoc")
    public static ProgressDialog showProgress(Context context, int message, int titleId) {
        ProgressDialog dialog = new ProgressDialog(context);
        if (titleId > 0) dialog.setTitle(titleId);
        if (message > 0) dialog.setMessage(context.getString(message));
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setOnCancelListener(null);
        dialog.show();
        return dialog;
    }

    public static void showToast(Context c, int resId, Object... args) {
        Toast toast;
        if (args.length > 0) {
            toast = Toast.makeText(c, c.getString(resId, args), Toast.LENGTH_LONG);
        } else {
            toast = Toast.makeText(c, resId, Toast.LENGTH_LONG);
        }
        toast.show();
    }

    public static void showToast(Context c, CharSequence text) {
        Toast toast = Toast.makeText(c, text, Toast.LENGTH_LONG);
        toast.show();
    }

    public static String hexString(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }

    public static boolean bindToService(Context context, Class<? extends Service> service, ServiceConnection callback) {
        //http://blog.tourizo.com/2009/04/binding-services-while-in-activitygroup.html
        context.startService(new Intent(context, service));
        if (sConnectionMap.get(context) == null) {
            sConnectionMap.put(context, new HashMap<Class<? extends Service>, ServiceConnection>());
        }
        sConnectionMap.get(context).put(service, callback);

        boolean success =  context.getApplicationContext().bindService(
                new Intent(context, service),
                callback,
                0);
        if (!success) Log.w(TAG, "BIND TO SERVICE " + service.getSimpleName() + " FAILED");
        return success;
    }

    public static void unbindFromService(Context context, Class<? extends Service> service) {
        ServiceConnection sb = sConnectionMap.get(context).remove(service);
        if (sConnectionMap.get(context).isEmpty()) sConnectionMap.remove(context);
        if (sb != null) context.getApplicationContext().unbindService(sb);
    }

    public static boolean isTaskFinished(AsyncTask lt) {
        return lt == null || lt.getStatus() == AsyncTask.Status.FINISHED;

    }
    public static boolean isTaskPending(AsyncTask lt) {
        return lt != null && lt.getStatus() == AsyncTask.Status.PENDING;

    }

    /**
     * @param pos play position in ms
     * @return formatted time string in the form of 0.05 or 2.12.04
     */
    public static String formatTimestamp(long pos){
        StringBuilder builder = new StringBuilder();
        int secs = (int) (pos / 1000);
        int minutes = secs  / 60;
        int hours = minutes / 60;
        if (hours > 0) {
            builder.append(hours);
            builder.append('.');
        }
        minutes = minutes % 60;
        if (hours > 0 && minutes < 10) builder.append('0');
        secs = secs % 60;
        builder.append(minutes).append('.');
        if (secs < 10) builder.append('0');
        builder.append(secs);
        return builder.toString();
    }

    public static CharSequence getElapsedTimeString(Resources r, long start, boolean longerText) {
        double elapsed = Double.valueOf(Math.ceil((System.currentTimeMillis() - start) / 1000d)).longValue();
        return getTimeString(r, elapsed, longerText);
    }

    public static String getTimeString(Resources r, double elapsedSeconds, boolean longerText) {
        if (elapsedSeconds < 60)
            return r.getQuantityString(longerText ? R.plurals.elapsed_seconds_ago : R.plurals.elapsed_seconds, (int) elapsedSeconds, (int) elapsedSeconds);
        else if (elapsedSeconds < 3600)
            return r.getQuantityString(longerText ? R.plurals.elapsed_minutes_ago : R.plurals.elapsed_minutes, (int) (elapsedSeconds / 60), (int) (elapsedSeconds / 60));
        else if (elapsedSeconds < 86400)
            return r.getQuantityString(longerText ? R.plurals.elapsed_hours_ago : R.plurals.elapsed_hours, (int) (elapsedSeconds / 3600), (int) (elapsedSeconds / 3600));
        else if (elapsedSeconds < 2592000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_days_ago : R.plurals.elapsed_days, (int) (elapsedSeconds / 86400), (int) (elapsedSeconds / 86400));
        else if (elapsedSeconds < 31536000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_months_ago : R.plurals.elapsed_months, (int) (elapsedSeconds / 2592000), (int) (elapsedSeconds / 2592000));
        else
            return r.getQuantityString(longerText ? R.plurals.elapsed_years_ago : R.plurals.elapsed_years, (int) (elapsedSeconds / 31536000), (int) (elapsedSeconds / 31536000));
    }

    public static String getTimeElapsed(Resources r, long timestamp){
        return getTimeString(r, Math.max(0, (System.currentTimeMillis() - timestamp)/1000), false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static int getScreenOrientation(Activity a) {
        Display getOrient = a.getWindowManager().getDefaultDisplay();
        int orientation;
        if (getOrient.getWidth() == getOrient.getHeight()) {
            orientation = Configuration.ORIENTATION_SQUARE;
        } else {
            if (getOrient.getWidth() < getOrient.getHeight()) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    /**
     * Execute a function, but only once.
     * @param context the context
     * @param key an identifier for the function
     * @param fun the function to run
     * @return whether the function was executed
     */
    @SuppressWarnings("UnusedDeclaration")
    public static boolean doOnce(Context context, String key, Runnable fun) {
        final String k = "do.once."+key;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(k, false)) {
            fun.run();
            prefs.edit().putBoolean(k, true).commit();
            return true;
        } else {
            return false;
        }
    }

    public static String getAppVersion(Context context, String defaultVersion) {
        try {
            PackageInfo info = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return defaultVersion;
        }
    }

    public static int getAppVersionCode(Context context, int defaultVersion) {
        try {
            PackageInfo info = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return defaultVersion;
        }
    }

    public static String getPackagename(Context context) {
        try {
            PackageInfo info = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return info.packageName;
        } catch (PackageManager.NameNotFoundException ignored) {
            throw new RuntimeException(ignored);
        }
    }


    /**
     * @param context
     * @return a unique id for this device (MD5 of IMEI / {@link Settings.Secure#ANDROID_ID}) or null
     */
    public static String getUniqueDeviceID(Context context) {
        TelephonyManager tmgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        String id = tmgr == null ? null : tmgr.getDeviceId();
        if (TextUtils.isEmpty(id)) {
            id = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return TextUtils.isEmpty(id) ? null : IOUtils.md5(id);
    }

     @SuppressWarnings("UnusedDeclaration")
     public static void logScreenSize(Context context) {
        switch (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                Log.d("ScreenSize", "Current Screen Size : Small Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                Log.d("ScreenSize", "Current Screen Size : Normal Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                Log.d("ScreenSize", "Current Screen Size : Large Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                Log.d("ScreenSize", "Current Screen Size : XLarge Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                Log.d("ScreenSize", "Current Screen Size : Undefined Screen");
                break;
        }
    }

    public static boolean isUserAMonkey() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                return ActivityManager.isUserAMonkey();
            } catch (RuntimeException e) {
                // java.lang.RuntimeException: Unknown exception code: 1 msg null
                return true;
            }
        } else {
            return false;
        }
    }

    public static void setTextShadowForGrayBg(TextView tv){
        tv.setShadowLayer(1, 0, 1, Color.WHITE);
    }

    public static boolean checkEmail(CharSequence email) {
        return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    public static String suggestEmail(Context context) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (checkEmail(account.name)) {
                if (counts.get(account.name) == null) {
                    counts.put(account.name, 1);
                } else {
                    counts.put(account.name, counts.get(account.name) + 1);
                }
            }
        }
        if (counts.isEmpty()) {
            return null;
        } else {
            int max = 0;
            String candidate = null;
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                if (e.getValue() > max) {
                    max = e.getValue();
                    candidate = e.getKey();
                }
            }
            return candidate;
        }
    }

    public static String[] longListToStringArr(List<Long> deletions) {
        int i = 0;
        String[] idList = new String[deletions.size()];
        for (Long id : deletions) {
            idList[i] = String.valueOf(id);
            i++;
        }
        return idList;
    }

    public static String removeFileExtension(String str){
        return str.contains(".") ? str.substring(0, str.lastIndexOf(".")) : str;
    }
}

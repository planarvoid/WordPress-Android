package com.soundcloud.android.utils;

import static android.content.pm.PackageManager.GET_SIGNATURES;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Android related utility functions.
 */
public final class AndroidUtils {
    private AndroidUtils() {}

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
            if (context.getPackageManager() != null) {
                PackageInfo info = context
                        .getPackageManager()
                        .getPackageInfo(context.getPackageName(),
                        PackageManager.GET_META_DATA);
                return info.versionName;
            } else return defaultVersion;
        } catch (PackageManager.NameNotFoundException ignored) {
            return defaultVersion;
        }
    }

    public static int getAppVersionCode(Context context, int defaultVersion) {
        try {
            if (context.getPackageManager() != null) {
                PackageInfo info = context
                        .getPackageManager()
                        .getPackageInfo(context.getPackageName(),
                        PackageManager.GET_META_DATA);
                return info.versionCode;
            } else return defaultVersion;
        } catch (PackageManager.NameNotFoundException ignored) {
            return defaultVersion;
        }
    }

    public static String getPackagename(Context context) {
        try {
            if (context.getPackageManager() != null) {
                PackageInfo info = context
                        .getPackageManager()
                        .getPackageInfo(context.getPackageName(),
                        PackageManager.GET_META_DATA);
                return info.packageName;
            } else return null;
        } catch (PackageManager.NameNotFoundException ignored) {
            throw new RuntimeException(ignored);
        }
    }

    public static boolean isRunOnBuilder(Context context) {
        return appSignedBy(context, context.getString(R.string.builder_sig));
    }

    public static boolean appSignedBy(Context context, String... keys) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    GET_SIGNATURES);
            if (info != null && info.signatures != null) {
                final String sig =  info.signatures[0].toCharsString();
                Arrays.sort(keys);
                return Arrays.binarySearch(keys, sig) > -1;
            } else {
                return false;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
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

    @SuppressLint("NewApi")
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

    public static String suggestEmail(Context context) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (ScTextUtils.isEmail(account.name)) {
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
}

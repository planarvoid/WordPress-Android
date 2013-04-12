package com.soundcloud.android.utils;

import static android.content.pm.PackageManager.GET_SIGNATURES;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.*;

/**
 * Android related utility functions.
 */
public final class AndroidUtils {
    private AndroidUtils() {}

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
    public static boolean doOnce(Context context, String key, final Runnable fun) {
        final String k = "do.once."+key;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(k, false)) {
            new Thread() {
                @Override
                public void run() {
                    fun.run();
                    prefs.edit().putBoolean(k, true).commit();
                }
            }.start();
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
     * @param context the context
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
        try {
            return ActivityManager.isUserAMonkey();
        } catch (RuntimeException e) {
            // java.lang.RuntimeException: Unknown exception code: 1 msg null
            return true;
        }
    }

    public static void setTextShadowForGrayBg(TextView... views) {
        for (TextView tv : views) {
            tv.setShadowLayer(1, 0, 1, Color.WHITE);
        }
    }

    public static boolean accessibilityFeaturesAvailable(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager != null && accessibilityManager.isEnabled();
    }

    /**
     * Returns emails from the account manager paired and sorted by their frequency of usage
     *
     * @param context
     * @return
     */
    public static String[] listEmails(Context context){
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (ScTextUtils.isEmail(account.name)) {
                if (map.get(account.name) == null) {
                    map.put(account.name, 1);
                } else {
                    map.put(account.name, map.get(account.name) + 1);
                }
            }
        }
        return returnKeysSortedByValue(map);
    }

    /* package */ static String[] returnKeysSortedByValue(HashMap<String, Integer> map) {
        TreeMap<String, Integer> sortedMap = new TreeMap<String, Integer>(new MapValueComparator(map));
        sortedMap.putAll(map);
        return sortedMap.keySet().toArray(new String[map.size()]);
    }

    private static class MapValueComparator implements Comparator<String> {
        Map<String, Integer> map;
        public MapValueComparator(Map<String, Integer> map) {
            this.map = map;
        }

        public int compare(String a, String b) {
            if (map.get(a) >= map.get(b)) {
                return -1;
            } else {
                return 1;
            } // returning 0 would merge keys
        }
    }
}

package com.soundcloud.android.utils;

import static com.soundcloud.java.checks.Preconditions.checkState;

import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Android related utility functions.
 */
public final class AndroidUtils {
    private AndroidUtils() {
    }

    public static ProgressDialog showProgress(Context context, int message) {
        return showProgress(context, message, 0);
    }

    /**
     * I18N safe impl of {@link android.app.ProgressDialog#show()}
     */
    @SuppressWarnings("JavaDoc")
    public static ProgressDialog showProgress(Context context, int message, int titleId) {
        ProgressDialog dialog = new ProgressDialog(context);
        if (titleId > 0) {
            dialog.setTitle(titleId);
        }
        if (message > 0) {
            dialog.setMessage(context.getString(message));
        }
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
     *
     * @param context the context
     * @param key     an identifier for the function
     * @param fun     the function to run
     * @return whether the function was executed
     */
    @SuppressWarnings("UnusedDeclaration")
    public static boolean doOnce(Context context, String key, final Runnable fun) {
        final String k = "do.once." + key;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(k, false)) {
            new Thread() {
                @Override
                public void run() {
                    fun.run();
                    prefs.edit().putBoolean(k, true).apply();
                }
            }.start();
            return true;
        } else {
            return false;
        }
    }


    @SuppressWarnings("UnusedDeclaration")
    public static void logScreenSize(Context context) {
        final String SCREEN_SIZE = "ScreenSize";
        final String CURRENT_SCREEN_SIZE = "Current Screen Size : ";
        switch (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                Log.d(SCREEN_SIZE, CURRENT_SCREEN_SIZE + "Small Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                Log.d(SCREEN_SIZE, CURRENT_SCREEN_SIZE + "Normal Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                Log.d(SCREEN_SIZE, CURRENT_SCREEN_SIZE + "Large Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                Log.d(SCREEN_SIZE, CURRENT_SCREEN_SIZE + "XLarge Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                Log.d(SCREEN_SIZE, CURRENT_SCREEN_SIZE + "Undefined Screen");
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

    public static boolean accessibilityFeaturesAvailable(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return accessibilityManager != null && accessibilityManager.isEnabled();
    }

    /**
     * Returns emails from the account manager paired and sorted by their frequency of usage
     */
    public static String[] listEmails(Context context) {
        Map<String, Integer> map = new HashMap<>();
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

    public static void assertOnUiThread(String message) {
        checkState(Looper.getMainLooper().getThread() == Thread.currentThread(), String.format(message + "[ %s ]", Thread.currentThread()));
    }

    /* package */
    static String[] returnKeysSortedByValue(Map<String, Integer> map) {
        TreeMap<String, Integer> sortedMap = new TreeMap<>(new MapValueComparator(map));
        sortedMap.putAll(map);
        return sortedMap.keySet().toArray(new String[map.size()]);
    }

    public static String[] getAccountsByType(Context context, String accountType) {
        Account[] accounts = AccountManager.get(context).getAccountsByType(accountType);
        final String[] names = new String[accounts.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
        }
        return names;
    }

    /**
     * Idempotent version of {@link Context#unregisterReceiver(android.content.BroadcastReceiver)} which allows null
     * references and recovers from "receiver not registered" errors.
     *
     * @param context  the context from which to unregister
     * @param receiver the receiver to unregister
     */
    public static void safeUnregisterReceiver(final Context context, @Nullable final BroadcastReceiver receiver) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException receiverAlreadyUnregistered) {
                receiverAlreadyUnregistered.printStackTrace();
            }
        }
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

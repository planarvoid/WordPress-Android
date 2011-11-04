package com.soundcloud.android.utils;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.graphics.*;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.view.ScTabView;
import org.json.JSONException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class CloudUtils {
    private static final String DURATION_FORMAT_SHORT = "%2$d.%5$02d";
    private static final String DURATION_FORMAT_LONG  = "%1$d.%3$02d.%5$02d";
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.ENGLISH);

    private CloudUtils() {}


    public static boolean isSDCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static File getCacheDir(Context c) {
        if (isSDCardAvailable()) {
            return Consts.EXTERNAL_CACHE_DIRECTORY;
        } else {
            return c.getCacheDir();
        }
    }

    public static String getCacheDirPath(Context c) {
        return getCacheDir(c).getAbsolutePath();
    }

    public static File getCacheFile(Context c, String name) {
          return new File(getCacheDir(c), name);
    }

    public static void checkState(Context c) {
        checkDirs(c);
        if (Consts.DEPRECATED_DB_ABS_PATH.exists()) {
            if (Consts.NEW_DB_ABS_PATH.exists()) {
                if (!Consts.NEW_DB_ABS_PATH.delete()) {
                    Log.w(TAG, "error deleting "+Consts.NEW_DB_ABS_PATH);
                }
            }
            if (!Consts.DEPRECATED_DB_ABS_PATH.renameTo(Consts.NEW_DB_ABS_PATH)) {
                Log.w(TAG, "error renaming "+Consts.DEPRECATED_DB_ABS_PATH);
            }
        }
    }

    public static void showToast(Context c, int resId) {
        Toast toast = Toast.makeText(c, c.getText(resId), Toast.LENGTH_LONG);
        toast.show();
    }

    public static void showToast(Context c, CharSequence text) {
        Toast toast = Toast.makeText(c, text, Toast.LENGTH_LONG);
        toast.show();
    }

    public static void checkDirs(Context c) {
        // clear out old cache and make a new one
        if (!getCacheDir(c).exists()) {
            if (getCacheDir(c).getParentFile().exists())
                deleteDir(getCacheDir(c).getParentFile());
        } else {
            // deleteDir(getCacheDir(c));
        }

        mkdirs(getCacheDir(c));

        // create external storage directory
        if (isSDCardAvailable()) {
            // fix deprecated casing
            if (fileExistsCaseSensitive(Consts.DEPRECATED_EXTERNAL_STORAGE_DIRECTORY)) {
                boolean renamed = renameCaseSensitive(
                    Consts.DEPRECATED_EXTERNAL_STORAGE_DIRECTORY, Consts.EXTERNAL_STORAGE_DIRECTORY);
                Log.d(TAG, "Attempting to rename external storage: " + renamed);
            }
            mkdirs(Consts.EXTERNAL_STORAGE_DIRECTORY);
        }
        // do a check??
    }

    public static boolean fileExistsCaseSensitive(final File f) {
        return f.exists() && f.getParentFile() != null && f.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contentEquals(f.getName());
            }
        }).length > 0;
    }

    public static boolean renameCaseSensitive(File oldFile, File newFile){
        if (oldFile.equals(newFile)){
            return oldFile.renameTo(newFile);
        } else if (oldFile.getParentFile() == null) {
            return false;
        } else {
            File tmp = new File(oldFile.getParentFile(),"."+System.currentTimeMillis());
            return oldFile.renameTo(tmp) && tmp.renameTo(newFile);
        }
    }

    public static File ensureUpdatedDirectory(File newDir, File deprecatedDir) {
        mkdirs(newDir);
        if (deprecatedDir.exists()) {
            for (File f : deprecatedDir.listFiles()) {
                f.renameTo(new File(newDir, f.getName()));
            }
            CloudUtils.deleteDir(deprecatedDir);
        }
        return newDir;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File f : children) {
                    boolean success;
                    if (f.isDirectory()) {
                         success = deleteDir(f);
                    } else {
                        success = deleteFile(f);
                    }
                    if (!success) {
                        return false;
                    }
                }
            }
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }

    public static boolean deleteFile(File f) {
        if (f != null && f.exists()) {
            if (!f.delete()) {
                Log.w(TAG, "could not delete "+f);
                return  false;
            } else return true;
        } else return false;
    }


    public static String md5(String s) {
        return md5(new ByteArrayInputStream(s.getBytes()));
    }

    public static String md5(File f) {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            return md5(is);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) try { is.close(); } catch (IOException ignored) { }
        }
    }

    public static String md5(InputStream f) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = f.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
            return hexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "error", e);
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public static String hexString(byte[] bytes) {
        return String.format("%0" + (bytes.length << 1) + "x", new BigInteger(1, bytes));
    }

    public static void createTab(TabHost tabHost, String tabId,
                                 String indicatorText, Drawable indicatorIcon, final ScTabView tabView) {
        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(tabId);
        if (indicatorIcon == null) {
            spec.setIndicator(indicatorText);
        } else {
            spec.setIndicator(indicatorText, indicatorIcon);
        }

        spec.setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return tabView;

            }
        });

        tabHost.addTab(spec);
    }


    public static String stripProtocol(String url) {
        return url.replace("http://www.", "").replace("http://", "");
    }

    public static void configureTabs(Context context, TabWidget tabWidget, int height, int width,
            boolean scrolltabs) {

        // Convert the tabHeight depending on screen density
        final float scale = context.getResources().getDisplayMetrics().density;
        height = (int) (scale * height);

        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            tabWidget.getChildAt(i).getLayoutParams().height = height;
            if (width > -1)
                tabWidget.getChildAt(i).getLayoutParams().width = width;

            if (scrolltabs)
                tabWidget.getChildAt(i).setPadding(Math.round(30 * scale),
                        tabWidget.getChildAt(i).getPaddingTop(), Math.round(30 * scale),
                        tabWidget.getChildAt(i).getPaddingBottom());
        }

        tabWidget.getLayoutParams().height = height;
    }

    public static void setTabTextStyle(Context context, TabWidget tabWidget) {
        setTabTextStyle(context, tabWidget, false);
    }

    public static void setTabTextStyle(Context context, TabWidget tabWidget, boolean textOnly) {
        // a hacky way of setting the font of the indicator texts
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            if (tabWidget.getChildAt(i) instanceof RelativeLayout) {
                RelativeLayout relativeLayout = (RelativeLayout) tabWidget.getChildAt(i);
                for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                    if (relativeLayout.getChildAt(j) instanceof TextView) {
                        ((TextView) relativeLayout.getChildAt(j)).setTextAppearance(context,
                                R.style.TabWidgetTextAppearance);
                        if (textOnly) {
                            relativeLayout.getChildAt(j).getLayoutParams().width = FILL_PARENT;
                            relativeLayout.getChildAt(j).getLayoutParams().height = FILL_PARENT;
                            ((TextView) relativeLayout.getChildAt(j)).setGravity(Gravity.CENTER);
                        }

                    }
                }
                if (textOnly) {
                    for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                        if (!(relativeLayout.getChildAt(j) instanceof TextView)) {
                            relativeLayout.removeViewAt(j);
                        }
                    }
                }
            }
        }
    }

    public static boolean checkIconShouldLoad(String url) {
        return !(TextUtils.isEmpty(url)
                || url.toLowerCase().contentEquals("null")
                || url.contains("default_avatar"));
    }

    private static HashMap<Context, HashMap<Class<? extends Service>, ServiceConnection>> sConnectionMap = new HashMap<Context,HashMap<Class<? extends Service>, ServiceConnection>>();

    public static boolean bindToService(Activity context, Class<? extends Service> service, ServiceConnection callback) {
        //http://blog.tourizo.com/2009/04/binding-services-while-in-activitygroup.html
        context.startService(new Intent(context, service));
        if (sConnectionMap.get(context) == null) sConnectionMap.put(context, new HashMap<Class<? extends Service>,ServiceConnection>());
        sConnectionMap.get(context).put(service, callback);

        boolean success =  context.getApplicationContext().bindService(
                (new Intent()).setClass(context, service),
                callback,
                0);

        if (!success) Log.w(TAG, "BIND TO SERVICE " + service.getSimpleName() + " FAILED");
        return success;
    }

    public static void unbindFromService(Activity context, Class<? extends Service> service) {
        ServiceConnection sb = sConnectionMap.get(context).remove(service);
        if (sConnectionMap.get(context).isEmpty()) sConnectionMap.remove(context);
        if (sb != null) context.getApplicationContext().unbindService(sb);
    }

    @SuppressWarnings("unchecked")
    public static boolean isTaskFinished(AsyncTask lt) {
        return lt == null || lt.getStatus() == AsyncTask.Status.FINISHED;

    }

    public static boolean isTaskPending(AsyncTask lt) {
        return lt != null && lt.getStatus() == AsyncTask.Status.PENDING;

    }

    public static String formatTimestamp(long pos){
        return CloudUtils.makeTimeString(pos < 3600000 ? DURATION_FORMAT_SHORT
                : DURATION_FORMAT_LONG, pos / 1000);
    }

    /*
     * Try to use String.format() as little as possible, because it creates a
     * new Formatter every time you call it, which is very inefficient. Reusing
     * an existing Formatter more than tripled the speed of makeTimeString().
     * This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
     */
    private static StringBuilder sBuilder = new StringBuilder();

    private static Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());

    private static final Object[] sTimeArgs = new Object[5];

    public static String formatString(String stringFormat, Object arg) {
        sBuilder.setLength(0);
        return sFormatter.format(stringFormat, arg).toString();
    }

    /** @see {@link CloudUtils#formatTimestamp(long)} ()} */
    /* package */ static String makeTimeString(String durationformat, long secs) {
        // XXX global state
        sBuilder.setLength(0);
        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        // XXX perf optimise - run in player loop
        return sFormatter.format(durationformat, timeArgs).toString();
    }

    public static boolean checkThreadAlive(Thread t) {
        return t != null && t.isAlive();
    }

    // Show an event in the LogCat view, for debugging
    public static void dumpMotionEvent(MotionEvent event) {
        String names[] = {
                "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?",
                "8?", "9?"
        };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);
        if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }
        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++) {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }
        sb.append("]");
        Log.d(TAG, sb.toString());
    }

    public static TextView buildEmptyView(Context context, CharSequence emptyText) {
        TextView emptyView = new TextView(context);
        emptyView.setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
        emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        emptyView.setTextAppearance(context, R.style.txt_empty_view);
        emptyView.setText(emptyText);
        emptyView.setId(android.R.id.empty);
        return emptyView;
    }

    public static Comment buildComment( Context context, long userId, long trackId, long timestamp, String commentBody, long replyToId){
        return buildComment(context, userId, trackId, timestamp, commentBody, replyToId, "");
    }

    public static Comment buildComment( Context context, long userId, long trackId, long timestamp, String commentBody, long replyToId, String replyToUsername){
        Comment comment = new Comment();
        comment.track_id = trackId;
        comment.created_at = new Date(System.currentTimeMillis());
        comment.user_id = userId;
        comment.user = SoundCloudDB.getUserById(context.getContentResolver(), comment.user_id);
        comment.timestamp = timestamp;
        comment.body = commentBody;
        comment.reply_to_id = replyToId;
        comment.reply_to_username = replyToUsername;
        return comment;
    }

    public static CharSequence getElapsedTimeString(Resources r, long start, boolean longerText) {
        double elapsed = Double.valueOf(Math.ceil((System.currentTimeMillis() - start) / 1000d)).longValue();

        if (elapsed < 60)
            return r.getQuantityString(longerText ? R.plurals.elapsed_seconds_ago : R.plurals.elapsed_seconds, (int) elapsed, (int) elapsed);
        else if (elapsed < 3600)
            return r.getQuantityString(longerText ? R.plurals.elapsed_minutes_ago : R.plurals.elapsed_minutes, (int) (elapsed / 60), (int) (elapsed / 60));
        else if (elapsed < 86400)
            return r.getQuantityString(longerText ? R.plurals.elapsed_hours_ago : R.plurals.elapsed_hours, (int) (elapsed / 3600), (int) (elapsed / 3600));
        else if (elapsed < 2592000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_days_ago : R.plurals.elapsed_days, (int) (elapsed / 86400), (int) (elapsed / 86400));
        else if (elapsed < 31536000)
            return r.getQuantityString(longerText ? R.plurals.elapsed_months_ago : R.plurals.elapsed_months, (int) (elapsed / 2592000), (int) (elapsed / 2592000));
        else
            return r.getQuantityString(longerText ? R.plurals.elapsed_years_ago : R.plurals.elapsed_years, (int) (elapsed / 31536000), (int) (elapsed / 31536000));

    }

    public static int getDigitsFromSeconds(int secs) {
        if (secs < 600) return 3;
        if (secs < 3600) return 4;
        if (secs < 36000) return 5;
        return 6;
    }


    public static String generateRecordingSharingNote(Resources res, CharSequence what, CharSequence where, long created_at) {
        String note;
        if (!TextUtils.isEmpty(what)) {
            if (!TextUtils.isEmpty(where)) {
                note =  res.getString(R.string.recorded_at, what, where);
            } else {
                note = what.toString();
            }
        } else {
            note = res.getString(R.string.sounds_from, !TextUtils.isEmpty(where) ? where :
                    recordingDateString(res, created_at));
        }
        return note;
    }

    public static String recordingDateString(Resources res, long modified) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(modified);
        final int id;
        if (cal.get(Calendar.HOUR_OF_DAY) <= 12) {
            id = R.string.recorded_morning;
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 17) {
            id = R.string.recorded_afternoon;
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 21) {
           id = R.string.recorded_evening;
        } else {
           id = R.string.recorded_night;
        }
        return res.getString(id, DAY_FORMAT.format(cal.getTime()));
    }


    public static String getTimeElapsed(android.content.res.Resources r, long eventTimestamp){
        long elapsed = (System.currentTimeMillis() - eventTimestamp)/1000;
        if (elapsed < 0) elapsed = 0;

        if (elapsed < 60) {
            return r.getQuantityString(R.plurals.elapsed_seconds, (int) elapsed,(int) elapsed);
        } else if (elapsed < 3600) {
            return r.getQuantityString(R.plurals.elapsed_minutes, (int) (elapsed/60),(int) (elapsed/60));
        } else if (elapsed < 86400) {
            return r.getQuantityString(R.plurals.elapsed_hours, (int) (elapsed/3600),(int) (elapsed/3600));
        } else if (elapsed < 2592000) {
            return r.getQuantityString(R.plurals.elapsed_days, (int) (elapsed/86400),(int) (elapsed/86400));
        } else if (elapsed < 31536000) {
            return r.getQuantityString(R.plurals.elapsed_months, (int) (elapsed/2592000),(int) (elapsed/2592000));
        } else {
            return r.getQuantityString(R.plurals.elapsed_years, (int) (elapsed/31536000),(int) (elapsed/31536000));
        }
    }

    /**
     * Adapted from the {@link android.text.util.Linkify} class. Changes the
     * first instance of {@code link} into a clickable link attached to the given listener
     * @param view the textview
     * @param link the link to set, or null to use the whole text
     * @param listener the listener
     * @param underline underline the text
     * @return true if the link was added
     */
    public static boolean clickify(TextView view, final String link, final ClickSpan.OnClickListener listener, boolean underline) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener, underline);

        int start = 0, end = string.length();
        if (link != null) {
            start = string.indexOf(link);
            end = start + link.length();
            if (start == -1) return false;
        }

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setText(s);
        }
        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
        return true;
    }

    public static boolean mkdirs(File d) {
        if (!d.exists()) {
            final boolean success = d.mkdirs();
            if (!success) Log.w(TAG, "mkdir "+d.getAbsolutePath()+" returned false");
            return success;
        } else {
            return false;
        }
    }

    public static String readInputStream(InputStream in) throws IOException {
        StringBuilder stream = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            stream.append(new String(b, 0, n));
        }
        return stream.toString();
    }

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

    public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
        if (uri == null) return null;

        if ("file".equals(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equals(uri.getScheme())) {
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = resolver.query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        return new File(filePath);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return null;
    }

    /**
     * Execute a function, but only once.
     * @param context the context
     * @param key an identifier for the function
     * @param fun the function to run
     * @return whether the function was executed
     */
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

     public static void logScreenSize(Context context) {
        switch (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                Log.i("ScreenSize", "Current Screen Size : Small Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                Log.i("ScreenSize", "Current Screen Size : Normal Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                Log.i("ScreenSize", "Current Screen Size : Large Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                Log.i("ScreenSize", "Current Screen Size : XLarge Screen");
                break;
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                Log.i("ScreenSize", "Current Screen Size : Undefined Screen");
                break;
        }
    }

    public static boolean isScreenXL(Context context){
        return ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    public static AlertDialog createLogoutDialog(final Activity a) {
        return new AlertDialog.Builder(a).setTitle(R.string.menu_clear_user_title)
            .setMessage(R.string.menu_clear_user_desc).setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SoundCloudApplication app = (SoundCloudApplication) a.getApplication();
                                app.clearUserDbData();
                                app.trackPage(Consts.Tracking.LOGGED_OUT);
                                app.trackEvent(Consts.Tracking.Categories.AUTH, "logout");
                                app.clearSoundCloudAccount(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                a.finish();
                                            }
                                        },
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                new AlertDialog.Builder(a)
                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                .setMessage(R.string.settings_error_revoking_account_message)
                                                .setPositiveButton(android.R.string.ok, null)
                                                .create()
                                                .show();
                                            }
                                        }
                                );
                            }
                        })
           .setNegativeButton(android.R.string.cancel, null)
           .create();
    }

    public static void setStats(int stat1, TextView statTextView1, View separator1, int stat2, TextView statTextView2,
                                View separator2, int stat3, TextView statTextView3, boolean maintainSize) {
        statTextView1.setText(Integer.toString(stat1));
        statTextView2.setText(Integer.toString(stat2));
        statTextView3.setText(Integer.toString(stat3));

        statTextView1.setVisibility(stat1 == 0 ? View.GONE : View.VISIBLE);
        separator1.setVisibility(stat1 == 0 || (stat2 == 0 && stat3 == 0) ? View.GONE : View.VISIBLE);

        statTextView2.setVisibility(stat2 == 0 ? View.GONE : View.VISIBLE);
        separator2.setVisibility(stat2 == 0 || stat3 == 0 ? View.GONE : View.VISIBLE);

        statTextView3.setVisibility(stat3 == 0 ? maintainSize ? View.INVISIBLE : View.GONE : View.VISIBLE);
    }

    public static float getCurrentTransformY(View v){
        if (v.getAnimation() == null) return 0f;
        Transformation t = new Transformation();
        float[] values = new float[9];
        v.getAnimation().getTransformation(v.getDrawingTime(), t);
        t.getMatrix().getValues(values);
        return values[5];
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

    public static void drawBubbleOnCanvas(Canvas c, Paint bgPaint, Paint linePaint, int width, int height, int arc, int arrowWidth, int arrowHeight, int arrowOffset){

        /*
             A ---- B
           I          C
           H ----G-E- D
                 F

         */

        final boolean arrowLeft = arrowOffset <= width/2;

        final int Ax = arc;
        final int Ay = 0;
        final int Bx = width - arc;
        final int By = 0;
        final int Cx = width;
        final int Cy = arc;
        final int Dx = width;
        final int Dy = height;
        final int Ex = arrowLeft ? arrowWidth + arrowOffset : arrowOffset;
        final int Ey = height;
        final int Fx = arrowOffset;
        final int Fy = height + arrowHeight;
        final int Gx = arrowLeft ? arrowOffset : arrowOffset - arrowWidth;
        final int Gy = height;
        final int Hx = 0;
        final int Hy = height;
        final int Ix = 0;
        final int Iy = arc;

        Path ctx = new Path();
        ctx.moveTo(Ax, Ay);
        ctx.lineTo(Bx, By);
        ctx.arcTo(new RectF(Bx, By, Cx, Cy), 270, 90); //B-C arc

        ctx.lineTo(Dx, Dy);

        if (arrowWidth > 0){
            ctx.lineTo(Ex, Ey);
            ctx.lineTo(Fx, Fy);
            ctx.lineTo(Gx, Gy);
        }


        ctx.lineTo(Hx, Hy);
        ctx.lineTo(Ix, Iy);
        ctx.arcTo(new RectF(Ax - arc, Ay, Ix + arc, Iy), 180, 90); //F-A arc
        c.drawPath(ctx, bgPaint);

        if (linePaint != null){
            c.drawLine(arrowOffset,height,arrowOffset,height+arrowOffset,linePaint);
        }
    }

    public static void drawSquareBubbleOnCanvas(Canvas c, Paint bgPaint, Paint linePaint, int width, int height, int arrowWidth, int arrowHeight, int arrowOffset){

        /*
             A ---- B
           I          C
           H ----G-E- D
                 F

         */

        final boolean arrowLeft = arrowOffset <= width / 2;

        final int Ax = 0;
        final int Ay = 0;
        final int Bx = width;
        final int By = 0;
        final int Cx = width;
        final int Cy = height;
        final int Dx = arrowLeft ? arrowWidth + arrowOffset : arrowOffset;
        final int Dy = height;
        final int Ex = arrowOffset;
        final int Ey = height + arrowHeight;
        final int Fx = arrowLeft ? arrowOffset : arrowOffset - arrowWidth;
        final int Fy = height;
        final int Gx = 0;
        final int Gy = height;

        Path ctx = new Path();
        ctx.moveTo(Ax, Ay);
        ctx.lineTo(Bx, By);
        ctx.lineTo(Cx, Cy);

        if (arrowWidth > 0) {
            ctx.lineTo(Dx, Dy);
            ctx.lineTo(Ex, Ey);
            ctx.lineTo(Fx, Fy);
        }

        ctx.lineTo(Gx, Gy);
        c.drawPath(ctx, bgPaint);
        if (linePaint != null) {
            c.drawLine(arrowOffset, height, arrowOffset, height + arrowOffset, linePaint);
        }
    }

    public static boolean isConnectionException(Exception e){
        return (e instanceof UnknownHostException
                || e instanceof SocketException
                || e instanceof JSONException);
    }

    public static String inMbFormatted(double size) {
        DecimalFormat maxDigitsFormatter = new DecimalFormat("#.#");
        return maxDigitsFormatter.format(size);
    }

    public static long dirSize(File dir) {
        long result = 0;
        File[] fileList = dir.listFiles();
        if (fileList == null) return 0;
        for (File aFileList : fileList) {
            if (aFileList.isDirectory()) {
                result += dirSize(aFileList);
            } else {
                result += aFileList.length();
            }
        }
        return result;
    }

    public static void setTextShadowForGrayBg(TextView tv){
        tv.setShadowLayer(1, 0, 1, Color.WHITE);
    }
}

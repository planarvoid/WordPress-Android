package com.soundcloud.android.utils;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.markupartist.android.widget.PullToRefreshListView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
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
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class CloudUtils {
    private static final String DURATION_FORMAT_SHORT = "%2$d.%5$02d";
    private static final String DURATION_FORMAT_LONG  = "%1$d.%3$02d.%5$02d";
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE");

    public static File getCacheDir(Context c) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
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
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
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
            for (File f : deprecatedDir.listFiles()){
                f.renameTo(new File(newDir, f.getName()));
            }
            deprecatedDir.delete();
        }
        return newDir;
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            // The directory is now empty so delete it
            return dir.delete();
        }
        return false;
    }


    public static String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) hexString.append(Integer.toHexString(0xFF & aMessageDigest));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "error", e);
            return "";
        }
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

    public static void setTabText(TabWidget tabWidget, int index, String newText) {
        // a hacky way of setting the font of the indicator texts

        if (tabWidget.getChildAt(index) instanceof RelativeLayout) {
            RelativeLayout relativeLayout = (RelativeLayout) tabWidget.getChildAt(index);
            for (int j = 0; j < relativeLayout.getChildCount(); j++) {
                if (relativeLayout.getChildAt(j) instanceof TextView) {
                    ((TextView) relativeLayout.getChildAt(j)).setHorizontallyScrolling(false);
                    ((TextView) relativeLayout.getChildAt(j)).setEllipsize(null);
                    relativeLayout.getChildAt(j).getLayoutParams().width = LayoutParams.WRAP_CONTENT;
                    ((TextView) relativeLayout.getChildAt(j)).setText(newText);
                    relativeLayout.getChildAt(j).requestLayout();
                }
            }
            relativeLayout.getLayoutParams().width =  LayoutParams.WRAP_CONTENT;
            relativeLayout.forceLayout();
        }

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


    public static String getLocationString(String city, String country) {
        if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
            return city + ", " + country;
        } else if (!TextUtils.isEmpty(city)) {
            return city;
        } else if (!TextUtils.isEmpty(country)) {
            return country;
        }

        return "";

    }

    public static String formatGraphicsUrl(String url, String targetSize) {
        return url == null ? null : url.replace("large", targetSize);
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

    /** @see CloudUtils.formatTimestamp() */
    public static String makeTimeString(String durationformat, long secs) {
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
        return (!(t == null || !t.isAlive()));
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

    public static void resolveListParcelable(Context c, Parcelable p, long user_id) {
        if (p instanceof Track) {
            ((Track)p).updateUserPlayedFromDb(c.getContentResolver(), user_id);
        } else if (p instanceof Event && ((Event)p).getTrack() != null) {
            ((Event)p).getTrack().updateUserPlayedFromDb(c.getContentResolver(), user_id);
        } else if (p instanceof User) {
            // check if they are a follower
        }
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

    public static String generateRecordingSharingNote(CharSequence what, CharSequence where, long created_at) {
        String note;
        if (!TextUtils.isEmpty(what)) {
            if (!TextUtils.isEmpty(where)) {
                note = String.format("%s at %s", what, where);
            } else {
                note = what.toString();
            }
        } else {
            if (!TextUtils.isEmpty(where)) {
                note = String.format("Sounds from %s", where);
            } else {
                note = String.format("Sounds from %s", recordingDateString(created_at));
            }
        }
        return note;
    }

    public static String recordingDateString(long modified) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(modified);

        final String day = DAY_FORMAT.format(cal.getTime());
        final String dayTime;

        if (cal.get(Calendar.HOUR_OF_DAY) <= 12) {
            dayTime = "morning";
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 17) {
            dayTime = "afternoon";
        } else if (cal.get(Calendar.HOUR_OF_DAY) <= 21) {
           dayTime = "evening";
        } else {
           dayTime = "night";
        }
        return day + " " + dayTime;
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
     */
    public static void clickify(TextView view, final String clickableText, final ClickSpan.OnClickListener listener, boolean underline) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener, underline);

        int start = string.indexOf(clickableText);
        int end = start + clickableText.length();
        if (start == -1) return;

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

    public static boolean isLandscape(Activity a) {
        return CloudUtils.getScreenOrientation(a) == Configuration.ORIENTATION_LANDSCAPE;
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
}

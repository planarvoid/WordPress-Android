package com.soundcloud.android.utils;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
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
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class CloudUtils {
    private static final String TAG = "CloudUtils";
    public static final String DURATION_FORMAT_SHORT = "%2$d.%5$02d";
    public static final String DURATION_FORMAT_LONG  = "%1$d.%3$02d.%5$02d";
    public static final int GRAPHIC_DIMENSIONS_BADGE = 47;

    public static final String DEPRECATED_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/Overcast";
    public static final String NEW_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/SoundCloud.db";

    public static final String DEPRECATED_EXTERNAL_STORAGE_DIRECTORY_PATH = Environment.getExternalStorageDirectory()+"/Soundcloud";

    public static final String DEPRECATED_RECORDINGS_FOLDER_PATH = Environment.getExternalStorageDirectory()+"/Soundcloud/.rec";
    public static final String NEW_RECORDINGS_ABS_PATH = Environment.getExternalStorageDirectory()+"/SoundCloud/recordings";

    public static final File EXTERNAL_CACHE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "Android/data/com.soundcloud.android/files/.cache/");

    public static final File EXTERNAL_STORAGE_DIRECTORY = new File(
            Environment.getExternalStorageDirectory(),
            "SoundCloud");

    public interface RequestCodes {
        int GALLERY_IMAGE_PICK = 9000;
        int GALLERY_IMAGE_TAKE = 9001;
    }

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED  = 2;
        int DIALOG_CANCEL_UPLOAD = 3;
        int DIALOG_RESET_RECORDING = 5;
    }

    public interface OptionsMenu {
        int SETTINGS = 200;
        int VIEW_CURRENT_TRACK = 201;
        int REFRESH = 202;
        int CANCEL_CURRENT_UPLOAD = 203;
        int INCOMING = 204;
    }

    public interface GraphicsSizes {
        String T500  = "t500x500";
        String CROP  = "crop";
        String LARGE = "large";
        String BADGE = "badge";
        String SMALL = "small";
    }

    public interface ListId {
        int LIST_INCOMING = 1001;
        int LIST_EXCLUSIVE = 1002;
        int LIST_USER_TRACKS = 1003;
        int LIST_USER_FAVORITES = 1004;
        int LIST_USER_FOLLOWINGS = 1006;
        int LIST_USER_FOLLOWERS = 1007;
    }

    public static File getCacheDir(Context c) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return EXTERNAL_CACHE_DIRECTORY;
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

        File f = new File(DEPRECATED_DB_ABS_PATH);
        if (f.exists()) {
            File newDb = new File(NEW_DB_ABS_PATH);
            if (newDb.exists()) {
                newDb.delete();
            }
            f.renameTo(newDb);
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

        getCacheDir(c).mkdirs();

        // create external storage directory
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // fix deprecated casing
            if (fileExistsCaseSensitive(DEPRECATED_EXTERNAL_STORAGE_DIRECTORY_PATH)) {
                Log.i(TAG,
                        "Attempting to rename external storage: "
                                + renameCaseSensitive(new File(
                                DEPRECATED_EXTERNAL_STORAGE_DIRECTORY_PATH),
                                        EXTERNAL_STORAGE_DIRECTORY));
            }

            EXTERNAL_STORAGE_DIRECTORY.mkdirs();
        }
        // do a check??
    }

    public static boolean fileExistsCaseSensitive(String filepath) {
        final File f = new File(filepath);
        if (!f.exists())
            return false;

        if (f.getParentFile() == null)
            return false;

        if (f.getParentFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contentEquals(f.getName());
            }
        }).length > 0)
            return true;
        return false;
    }

    public static boolean renameCaseSensitive(File oldFile, File newFile){
        if (oldFile.equals(newFile)){
            return oldFile.renameTo(newFile);
        }

        if (oldFile.getParentFile() == null) return false;

        File tmp = new File(oldFile.getParentFile(),"."+System.currentTimeMillis());
        if (!oldFile.renameTo(tmp)) return false;
        if (!tmp.renameTo(newFile)) return false;

        oldFile = newFile;
        return true;
    }

    public static File ensureUpdatedDirectory(String validPath, String deprecatedPath){
        File depDir = new File(deprecatedPath);
        File newDir = new File(validPath);
        if (!newDir.exists()) newDir.mkdirs();

        if (depDir.exists()){
            for (File f : depDir.listFiles()){
                f.renameTo(new File(newDir,f.getName()));
            }
            depDir.delete();
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

    public static LazyListView createTabList(ScActivity activity,
                                   FrameLayout listHolder,
                                   LazyEndlessAdapter adpWrap,
                                   int listId, OnTouchListener touchListener) {

        listHolder.setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
        LazyListView lv = activity.buildList();
        if (listId != -1) lv.setId(listId);
        if (touchListener != null) lv.setOnTouchListener(touchListener);
        lv.setAdapter(adpWrap);
        listHolder.addView(lv);
        adpWrap.createListEmptyView(lv);

        return lv;
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
        return !(url == null ||
                  url.contentEquals("") || url.toLowerCase().contentEquals("null")
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

    public static String buildRequestPath(String mUrl, String order) {
        return buildRequestPath(mUrl, order, false);
    }

    public static String buildRequestPath(String mUrl, String order, boolean refresh) {
        String refreshAppend = "";
        if (refresh)
            refreshAppend = "&rand=" + Math.round(10000 * Math.random());

        if (order == null) {
            if (refresh) {
                return mUrl + "?rand=" + (10000 * Math.random());
            } else {
                return mUrl;
            }
        }
        return String.format(mUrl + "?order=%s", URLEncoder.encode(order) + refreshAppend);

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

    public static String makeTimeString(String durationformat, long secs) {
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


    public static void resolveParcelable(Context c, Parcelable p, long user_id) {
        if (p instanceof Track) {
            SoundCloudDB.getInstance().resolveTrack(c.getContentResolver(), (Track) p,
                    SoundCloudDB.WriteState.none, user_id);
        } else if (p instanceof Event) {
            if (((Event) p).getTrack() != null)
                SoundCloudDB.getInstance().resolveTrack(c.getContentResolver(),
                        ((Event) p).getTrack(), SoundCloudDB.WriteState.none, user_id);
        } else if (p instanceof User) {
            SoundCloudDB.getInstance().resolveUser(c.getContentResolver(), (User) p,
                    SoundCloudDB.WriteState.none, user_id);
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
        comment.user = SoundCloudDB.getInstance().resolveUserById(context.getContentResolver(), comment.user_id);
        comment.timestamp = timestamp;
        comment.body = commentBody;
        comment.reply_to_id = replyToId;
        comment.reply_to_username = replyToUsername;
        return comment;
    }

    public static String generateRecordingSharingNote(CharSequence what, CharSequence where, long created_at) {
        String note;
        if (!TextUtils.isEmpty(what)) {
            Log.i(TAG,"Not empty what");
            if (!TextUtils.isEmpty(where)) {
                note = String.format("%s at %s", what, where);
            } else {
                note = what.toString();
            }
        } else {
            if (!TextUtils.isEmpty(where)) {
                Log.i(TAG,"Not empty where");
                note = String.format("Sounds from %s", where);
            } else {
                Log.i(TAG,"Empty both");
                note = String.format("Sounds from %s", recordingDateString(created_at));
            }
        }
        return note;
    }

    public static String recordingDateString(long modified) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(modified);

        String day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
        String dayTime;

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


    public static String getTimeElapsed(Context c, long eventTimestamp){
        long elapsed = (System.currentTimeMillis() - eventTimestamp)/1000;

        if (elapsed < 60) {
            return c.getResources().getQuantityString(R.plurals.elapsed_seconds, (int) elapsed,(int) elapsed);
        } else if (elapsed < 3600) {
            return c.getResources().getQuantityString(R.plurals.elapsed_minutes, (int) (elapsed/60),(int) (elapsed/60));
        } else if (elapsed < 86400) {
            return c.getResources().getQuantityString(R.plurals.elapsed_hours, (int) (elapsed/3600),(int) (elapsed/3600));
        } else if (elapsed < 2592000) {
            return c.getResources().getQuantityString(R.plurals.elapsed_days, (int) (elapsed/86400),(int) (elapsed/86400));
        } else if (elapsed < 31536000) {
            return c.getResources().getQuantityString(R.plurals.elapsed_months, (int) (elapsed/2592000),(int) (elapsed/2592000));
        } else {
            return c.getResources().getQuantityString(R.plurals.elapsed_years, (int) (elapsed/31536000),(int) (elapsed/31536000));
        }
    }

    /**
     * Adapted from the {@link android.text.util.Linkify} class. Changes the
     * first instance of {@code link} into a clickable link attached to the given listener
     */
    public static void clickify(TextView view, final String clickableText, final ClickSpan.OnClickListener listener) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener);

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
}

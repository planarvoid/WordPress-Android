
package com.soundcloud.android;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.android.view.ScTabView;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class CloudUtils {

    private static final String TAG = "CloudUtils";
    public static final String DURATION_FORMAT_SHORT = "%2$d.%5$02d";
    public static final String DURATION_FORMAT_LONG = "%1$d.%3$02d.%5$02d";
    public static final int GRAPHIC_DIMENSIONS_BADGE = 47;
    public static final int GRAPHIC_DIMENSIONS_SMALL = 32;

    public static final String DEPRACATED_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/Overcast";
    public static final String NEW_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/SoundCloud.db";

    public static final String EXTERNAL_CACHE_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Android/data/com.soundcloud.android/files/.cache/";

    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Soundcloud";


    public interface RequestCodes {
        public static final int GALLERY_IMAGE_PICK = 9000;
        public static final int GALLERY_IMAGE_TAKE = 9001;
    }

    public enum Model {
        track, user, comment, event
    }

    public interface Dialogs {
        int DIALOG_ERROR_LOADING = 1;
        int DIALOG_UNAUTHORIZED  = 2;
        int DIALOG_CANCEL_UPLOAD = 3;
        int DIALOG_AUTHENTICATION_CONTACTING = 4;

    }

    public interface OptionsMenu {
        public static final int SETTINGS = 200;

        public static final int VIEW_CURRENT_TRACK = 201;

        public static final int REFRESH = 202;

        public static final int CANCEL_CURRENT_UPLOAD = 203;
    }

    public interface GraphicsSizes {
        public final static String t500 = "t500x500";

        public final static String crop = "crop";

        public final static String t300 = "t300x300";

        public final static String large = "large";

        public final static String t67 = "t67";

        public final static String badge = "badge";

        public final static String small = "small";

        public final static String tiny = "tiny";

        public final static String mini = "mini";

        public final static String original = "original";
    }

    public interface ListId {
        public final static int LIST_INCOMING = 1001;
        public final static int LIST_EXCLUSIVE = 1002;
        public final static int LIST_USER_TRACKS = 1003;
        public final static int LIST_USER_FAVORITES = 1004;
        public final static int LIST_SEARCH = 1005;
        public final static int LIST_USER_FOLLOWINGS = 1006;
        public final static int LIST_USER_FOLLOWERS = 1007;
    }

    public static File getCacheDir(Context c) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return new File(EXTERNAL_CACHE_DIRECTORY);
        } else {
            return c.getCacheDir();
        }
    }

    public static String getCacheDirPath(Context c) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return EXTERNAL_CACHE_DIRECTORY;
        } else {
            return c.getCacheDir().getAbsolutePath();
        }
    }

    public static String getCacheFilePath(Context c, String name) {
        return getCacheFile(c, name).getAbsolutePath();
    }

    public static File getCacheFile(Context c, String name) {
          return new File(getCacheDir(c), name);
    }

    public static void checkState(Context c) {
        checkDirs(c);

        File f = new File(DEPRACATED_DB_ABS_PATH);
        Log.i(TAG,"!!!!!!! looking for db " + f.exists());
        if (f.exists()) {
            File newDb = new File(NEW_DB_ABS_PATH);
            if (newDb.exists()) {
                newDb.delete();
            }
            f.renameTo(newDb);
        }
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
            new File(EXTERNAL_STORAGE_DIRECTORY).mkdirs();
        }
        // do a check??
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
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
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
                    ((TextView) relativeLayout.getChildAt(j)).setText(newText);
                }
            }

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


    public static boolean isTrackPlayable(Track track) {
        return track.streamable;
    }

    public static long getCurrentUserId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            return preferences.getLong(SoundCloudApplication.USER_ID, -1);
        } catch (ClassCastException e) {
            long id = Long.parseLong(preferences.getString(SoundCloudApplication.USER_ID, "-1"));
            if (id != -1) {
                preferences.edit().putLong(SoundCloudApplication.USER_ID, id).commit();
            }
            return id;
        }
    }

    public static boolean checkIconShouldLoad(String url) {
        return !(url == null ||
                  url.contentEquals("") || url.toLowerCase().contentEquals("null")
                || url.contains("default_avatar"));
    }

    private static HashMap<Context, ServiceConnection> sConnectionMap = new HashMap<Context, ServiceConnection>();

    public static boolean bindToService(Activity context, Class<? extends Service> service, ServiceConnection callback) {
        //http://blog.tourizo.com/2009/04/binding-services-while-in-activitygroup.html
        context.startService(new Intent(context, service));
        sConnectionMap.put(context, callback);
        Log.i(TAG, "Binding service " + sConnectionMap.size());

        boolean success =  context.getApplicationContext().bindService(
                (new Intent()).setClass(context, service),
                callback,
                0);

        if (!success) Log.w(TAG, "BIND TO SERVICE " + service.getSimpleName() + " FAILED");
        return success;
    }

    public static void unbindFromService(Activity context) {
        Log.i(TAG, "Unbind From Service " + context);
        ServiceConnection sb = sConnectionMap.remove(context);
        if (sb != null) context.getApplicationContext().unbindService(sb);
        Log.i(TAG, "Connetcion map empty? " + sConnectionMap.isEmpty());
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
        return url.replace("large", targetSize);
    }

    @SuppressWarnings("unchecked")
    public static boolean isTaskFinished(AsyncTask lt) {
        return lt == null || lt.getStatus() == AsyncTask.Status.FINISHED;

    }

    public static boolean isTaskPending(AsyncTask lt) {
        return lt != null && lt.getStatus() == AsyncTask.Status.PENDING;

    }

    public static Long getPCMTime(File file, int sampleRate, int channels, int bitsPerSample) {
        return file.length() / (sampleRate * channels * bitsPerSample / 8);
    }

    public static Long getPCMTime(long bytes, int sampleRate, int channels, int bitsPerSample) {
        return bytes / (sampleRate * channels * bitsPerSample / 8);
    }

    public static BitmapFactory.Options determineResizeOptions(File imageUri, int targetWidth,
                                                               int targetHeight) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream is = new FileInputStream(imageUri);

        BitmapFactory.decodeStream(is, null, options);
        is.close();

        int height = options.outHeight;
        int width = options.outWidth;

        if (height > targetHeight || width > targetWidth) {
            if (targetHeight / height < targetWidth / width) {
                options.inSampleSize = Math.round(height / targetHeight);
            } else {
                options.inSampleSize = Math.round(width / targetWidth);
            }

        }
        return options;
    }

    public static void clearBitmap(Bitmap bmp) {
        bmp.recycle();
        System.gc();
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
    public static String getErrorFromJSONResponse(String rawString) throws JSONException {
        if (rawString.startsWith("[")) {
            return ""; // arrays do not result from errors
        } else {
            JSONObject errorChecker = new JSONObject(rawString);
            try {
                if (errorChecker.get("error") != null) {
                    return errorChecker.getString("error");
                } else {
                    return "";
                }
            } catch (Exception e) {
                return "";
            }

        }
    }

    public static boolean checkThreadAlive(Thread t) {
        return (!(t == null || !t.isAlive()));
    }

    public static String streamToString(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
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


    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }


    public static void resolveParcelable(Context c, Parcelable p) {
        if (p instanceof Track) {
            SoundCloudDB.getInstance().resolveTrack(c.getContentResolver(), (Track) p, SoundCloudDB.WriteState.none,
                    CloudUtils.getCurrentUserId(c));
        } else if (p instanceof Event) {
            if (((Event) p).getTrack() != null)
                SoundCloudDB.getInstance().resolveTrack(c.getContentResolver(), ((Event) p).getTrack(), SoundCloudDB.WriteState.none,
                        CloudUtils.getCurrentUserId(c));
        } else if (p instanceof User) {
            SoundCloudDB.getInstance().resolveUser(c.getContentResolver(), (User) p, SoundCloudDB.WriteState.none,
                    CloudUtils.getCurrentUserId(c));
        }
    }



}

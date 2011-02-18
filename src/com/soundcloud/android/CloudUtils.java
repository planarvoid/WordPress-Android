
package com.soundcloud.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.objects.BaseObj.WriteState;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;
import org.json.JSONException;
import org.json.JSONObject;

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

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;

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

    /**
     * A parcelable has just been loaded, so perform any data operations
     * necessary
     *
     * @param context
     * @param p : the parcelable that has just been loaded
     */
    public static void resolveParcelable(Context context, Parcelable p) {
        if (p instanceof Track) {
            resolveTrack(context, (Track) p, WriteState.none,
                    getCurrentUserId(context));
        } else if (p instanceof Event) {
            if (((Event) p).getTrack() != null)
                resolveTrack(context, ((Event) p).getTrack(),
                        WriteState.none, getCurrentUserId(context));
        } else if (p instanceof User) {
            resolveUser(context, (User) p, WriteState.none,
                    getCurrentUserId(context));
        }
    }

    public interface RequestCodes {
        public static final int GALLERY_IMAGE_PICK = 9000;

        public static final int REAUTHORIZE = 9001;
    }

    public enum Model {
        track, user, comment, event
    }

    public interface Dialogs {
        public static final int DIALOG_GENERAL_ERROR = 9;

        public static final int DIALOG_ERROR_LOADING = 10;

        public static final int DIALOG_UNAUTHORIZED = 11;

        public static final int DIALOG_ADD_COMMENT = 12;

        public static final int DIALOG_FOLLOWING = 13;

        public static final int DIALOG_UNFOLLOWING = 14;

        public static final int DIALOG_ALREADY_FOLLOWING = 15;

        public static final int DIALOG_FAVORITED = 16;

        public static final int DIALOG_UNFAVORITED = 17;

        public static final int DIALOG_ERROR_STREAM_NOT_SEEKABLE = 18;

        public static final int DIALOG_ERROR_NO_DOWNLOADS = 19;

        public static final int DIALOG_ERROR_TRACK_ERROR = 20;

        public static final int DIALOG_ERROR_TRACK_DOWNLOAD_ERROR = 21;

        public static final int DIALOG_ADD_COMMENT_ERROR = 22;

        public static final int DIALOG_SC_CONNECT_ERROR = 23;

        public static final int DIALOG_ERROR_CHANGE_FOLLOWING_STATUS_ERROR = 24;

        public static final int DIALOG_ERROR_CHANGE_FAVORITE_STATUS_ERROR = 25;

        public static final int DIALOG_CONFIRM_DELETE_TRACK = 26;

        public static final int DIALOG_CONFIRM_RE_DOWNLOAD_TRACK = 27;

        public static final int DIALOG_CONFIRM_REMOVE_FAVORITE = 28;

        public static final int DIALOG_CONFIRM_REMOVE_FOLLOWING = 29;

        public static final int DIALOG_CONFIRM_CLEAR_PLAYLIST = 30;

        public static final int DIALOG_PROCESSING = 31;

        public static final int DIALOG_CANCEL_UPLOAD = 32;

        public static final int DIALOG_ERROR_RECORDING = 37;

        public static final int DIALOG_ERROR_MAKING_CONNECTION = 36;

        public static final int DIALOG_AUTHENTICATION_CONTACTING = 33;

        public static final int DIALOG_AUTHENTICATION_ERROR = 34;

        public static final int DIALOG_AUTHENTICATION_RETRY = 35;
    }

    public interface Defs {
        public final static int OPEN_URL = 0;

        public final static int ADD_TO_PLAYLIST = 1;

        public final static int USE_AS_RINGTONE = 2;

        public final static int PLAYLIST_SELECTED = 3;

        public final static int NEW_PLAYLIST = 4;

        public final static int PLAY_SELECTION = 5;

        public final static int GOTO_START = 6;

        public final static int GOTO_PLAYBACK = 7;

        public final static int PARTY_SHUFFLE = 8;

        public final static int SHUFFLE_ALL = 9;

        public final static int DELETE_ITEM = 10;

        public final static int SCAN_DONE = 11;

        public final static int QUEUE = 12;

        public final static int CHILD_MENU_BASE = 13; // this should be the last
        // item
    }

    public interface OptionsMenu {
        public static final int SETTINGS = 200;

        public static final int VIEW_CURRENT_TRACK = 201;

        public static final int REFRESH = 202;

        public static final int CANCEL_CURRENT_UPLOAD = 203;
    }

    public interface ContextMenu {

        public static final int CLOSE = 100;

        // basic track functions
        public static final int VIEW_TRACK = 110;

        public static final int PLAY_TRACK = 111;

        public static final int ADD_TO_PLAYLIST = 112;

        public static final int VIEW_UPLOADER = 113;

        public static final int DELETE = 114;

        public static final int RE_DOWNLOAD = 115;

        // pending download functions
        public static final int CANCEL_DOWNLOAD = 120;

        public static final int RESTART_DOWNLOAD = 121;

        public static final int FORCE_DOWNLOAD = 132;

        // downloaded functions
        public static final int DELETE_TRACK = 130;

        public static final int REFRESH_TRACK_DATA = 131;

        // comment functions
        public static final int PLAY_FROM_COMMENT_POSITION = 140;

        public static final int REPLY_TO_COMMENT = 141;

        public static final int VIEW_COMMENTER = 142;

        // playlist functions
        public static final int REMOVE_TRACK = 151;

        public static final int REMOVE_OTHER_TRACKS = 152;

        // basic user functions
        public static final int VIEW_USER = 160;

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

    public final static String[] GraphicsSizesLib = {
            GraphicsSizes.t500, GraphicsSizes.crop, GraphicsSizes.t300, GraphicsSizes.large,
            GraphicsSizes.t67, GraphicsSizes.badge, GraphicsSizes.small, GraphicsSizes.tiny,
            GraphicsSizes.mini, GraphicsSizes.original
    };

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
            if (newDb.exists())
                newDb.delete();
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

    public static LazyList createList(Activity activity) {

        LazyList mList = new LazyList(activity);
        mList.setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
        if (activity instanceof AdapterView.OnItemClickListener) {
            mList.setOnItemClickListener((AdapterView.OnItemClickListener) activity);
        }
        mList.setFastScrollEnabled(true);
        mList.setTextFilterEnabled(true);
        mList.setDivider(activity.getResources().getDrawable(R.drawable.list_separator));
        mList.setDividerHeight(1);
        activity.registerForContextMenu(mList);

        return mList;
    }

    public static void createTabList(ScActivity activity, FrameLayout listHolder,
            LazyEndlessAdapter adpWrap, int listId) {
        createTabList(activity, listHolder, adpWrap, listId, null);
    }

    public static void createTabList(ScActivity activity, FrameLayout listHolder,
            LazyEndlessAdapter adpWrap, int listId, OnTouchListener touchListener) {

        listHolder.setLayoutParams(new LayoutParams(FILL_PARENT, FILL_PARENT));
        if (activity instanceof Dashboard) {
            LazyList lv = ((Dashboard) activity).buildList(false);
            if (listId != -1)
                lv.setId(listId);
            if (touchListener != null)
                lv.setOnTouchListener(touchListener);
            lv.setAdapter(adpWrap);
            ((Dashboard)activity).configureListMenu(lv);
            listHolder.addView(lv);
            adpWrap.createListEmptyView(lv);
        }
    }


    public static FrameLayout createTabLayout(Context context) {
        FrameLayout tabLayout = new FrameLayout(context);
        tabLayout.setLayoutParams(new LayoutParams(FILL_PARENT,
                FILL_PARENT));

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.cloudtabs, tabLayout);

        // construct the tabhost
        final TabHost tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);

        FrameLayout frameLayout = (FrameLayout) tabLayout.findViewById(android.R.id.tabcontent);
        frameLayout.setPadding(0, 0, 0, 0);

        tabHost.setup();

        return tabLayout;

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

    public static void setTabTextStyle(Context context, TabWidget tabWidget) {
        setTabTextStyle(context, tabWidget, false);
    }

    public static void setTabTextStyle(Context context, TabWidget tabWidget, Boolean textOnly) {
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

    public static Boolean isTrackPlayable(Track track) {
        return track.streamable;
    }

    public static Long getCurrentUserId(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Long.parseLong(preferences.getString("currentUserId", "-1"));
    }

    public static boolean checkIconShouldLoad(String url) {
        return !(url == null ||
                  url.contentEquals("") || url.toLowerCase().contentEquals("null")
                || url.contains("default_avatar"));
    }

    public static ICloudPlaybackService sService = null;

    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    public static boolean bindToService(Activity context, ServiceConnection callback) {
        //http://blog.tourizo.com/2009/04/binding-services-while-in-activitygroup.html
        if (context.getParent() != null)
            context = context.getParent();


        context.startService(new Intent(context, CloudPlaybackService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        sConnectionMap.put(context, sb);
        Log.i(TAG, "Binding service " + sConnectionMap.size());



        return context.bindService(
                (new Intent()).setClass(context, CloudPlaybackService.class),
                sb,
                0);
    }

    public static void unbindFromService(Context context) {
        Log.i(TAG, "Unbind From Service " + context);
        ServiceBinder sb = sConnectionMap.remove(context);
        if (sb == null) {
            return;
        }
        context.unbindService(sb);
        Log.i(TAG, "Connetcion map empty? " + sConnectionMap.isEmpty());
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this
            // point,
            // so don't hang on to the ServiceConnection
            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;

        ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }

        public void onServiceConnected(ComponentName className, android.os.IBinder service) {
            sService = ICloudPlaybackService.Stub.asInterface(service);
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }

    public static void resolveTrack(Context context, Track track,
            WriteState writeState, long currentUserId) {
        resolveTrack(context, track, writeState, currentUserId, null);
    }

    // ---Make sure the database is up to date with this track info---
    public static void resolveTrack(Context context, Track track,
            WriteState writeState, Long currentUserId, DBAdapter openAdapter) {
            DBAdapter db;
            if (openAdapter == null) {
                db = new DBAdapter(context);
                db.open();
            } else
                db = openAdapter;
    
            Cursor result = db.getTrackById(track.id, currentUserId);
            if (result.getCount() != 0) {
                // add local urls and update database
                result.moveToFirst();
    
                track.user_played = result.getInt(result.getColumnIndex("user_played")) == 1;
    
                if (writeState == WriteState.update_only || writeState == WriteState.all)
                    db.updateTrack(track);
    
            } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                db.insertTrack(track);
            }
            result.close();
    
            if (openAdapter == null) db.close();


        // write with insert only because a track will never come in with
            resolveUser(context, track.user, WriteState.insert_only, currentUserId, openAdapter);
    }

    // ---Make sure the database is up to date with this track info---
    public static Track resolveTrackById(SoundCloudApplication context, long l, long currentUserId) {
        DBAdapter db = new DBAdapter(context);
        db.open();

        Cursor result = db.getTrackById(l, currentUserId);
        if (result.getCount() != 0) {
            Track track = new Track(result);
            // track = resolvePlayUrl(track);
            // track = resolveTrackFavorite(track);

            result.close();
            result = db.getUserById(track.user_id, currentUserId);

            if (result.getCount() != 0) {
                track.user = new User(result);
                track.user_id = track.user.id;
            }

            result.close();
            db.close();

            return track;
        }

        result.close();
        db.close();

        return null;

    }

    public static void resolveUser(Context context, User user, WriteState writeState,
            Long userId) {
        resolveUser(context, user, writeState, userId, null);
    }

    // ---Make sure the database is up to date with this track info---
    public static void resolveUser(Context context, User user, WriteState writeState,
            Long currentUserId, DBAdapter openAdapter) {
        DBAdapter db;
        if (openAdapter == null) {
            db = new DBAdapter(context);
            db.open();
        } else
            db = openAdapter;

        Cursor result = db.getUserById(user.id, currentUserId);
        if (result.getCount() != 0) {

            user.update(result); // update the parcelable with values from the db

            if (writeState == WriteState.update_only || writeState == WriteState.all)
                db.updateUser(user, currentUserId.compareTo(user.id) == 0);

        } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
            db.insertUser(user, currentUserId.compareTo(user.id) == 0);
        }
        result.close();

        if (openAdapter == null) db.close();
    }

    // ---Make sure the database is up to date with this track info---
    public static User resolveUserById(Context context, long userId,
            long currentUserId) {
        DBAdapter db = new DBAdapter(context);
        db.open();

        Cursor result = db.getUserById(userId, currentUserId);

        if (result.getCount() != 0) {

            User user = new User(result);
            result.close();
            db.close();

            return user;
        }

        result.close();
        db.close();

        return null;

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

    public static String stripProtocol(String url) {
        return url.replace("http://www.", "").replace("http://", "");
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
        /*
         * Provide multiple arguments so the format can be changed easily by
         * modifying the xml.
         */
        sBuilder.setLength(0);
        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        return sFormatter.format(durationformat, timeArgs).toString();
    }

    /*
     * public static String formatContent(InputStream is) throws IOException {
     * if (is == null) return ""; StringBuilder sBuilder = new StringBuilder();
     * BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
     * String line = null; while ((line = buffer.readLine()) != null) {
     * sBuilder.append(line).append("\n"); } buffer.close(); buffer = null;
     * return sBuilder.toString().trim(); }
     */

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

    /**
     * Check if a thread is alive accounting for nulls
     * 
     * @param t
     * @return boolean : is the thread alive
     */
    public static Boolean checkThreadAlive(Thread t) {
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
    
   

    public static void cleanupList(ListView list) {
        list.setOnItemClickListener(null);
        list.setOnItemLongClickListener(null);
        list.setOnCreateContextMenuListener(null);
        list.setOnScrollListener(null);
        list.setOnItemSelectedListener(null);

    }

}

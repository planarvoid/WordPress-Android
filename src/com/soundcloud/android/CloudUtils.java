
package com.soundcloud.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.activity.LazyTabActivity;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.LazyExpandableBaseAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.objects.BaseObj.WriteState;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.ICloudPlaybackService;
import com.soundcloud.android.task.LoadTask;
import com.soundcloud.android.view.LazyList;
import com.soundcloud.android.view.ScTabView;
import com.soundcloud.android.view.UserBrowser;

public class CloudUtils {

    private static final String TAG = "CloudUtils";

    public static final String API_BASE = "http://api.soundcloud.com/";

    public static final String REQUEST_FORMAT = "json";

    public static final int GRAPHIC_DIMENSIONS_T500 = 500;

    public static final int GRAPHIC_DIMENSIONS_CROP = 400;

    public static final int GRAPHIC_DIMENSIONS_T300 = 300;

    public static final int GRAPHIC_DIMENSIONS_LARGE = 100;

    public static final int GRAPHIC_DIMENSIONS_T67 = 67;

    public static final int GRAPHIC_DIMENSIONS_BADGE = 47;

    public static final int GRAPHIC_DIMENSIONS_SMALL = 32;

    public static final int GRAPHIC_DIMENSIONS_TINY_ARTWORKS = 20;

    public static final int GRAPHIC_DIMENSIONS_TINY_AVATARS = 18;

    public static final int GRAPHIC_DIMENSIONS_MINI = 16;

    public static final String EXTRA_GROUP = "group";

    public static final String EXTRA_FILTER = "filter";

    public static final String EXTRA_TITLE = "title";

    public static final String DEPRACATED_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/Overcast";

    public static final String NEW_DB_ABS_PATH = "/data/data/com.soundcloud.android/databases/SoundCloud.db";

    public static final String MUSIC_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Soundcloud/music";

    public static final String ARTWORK_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Soundcloud/images/artwork";

    public static final String WAVEFORM_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Soundcloud/images/waveforms";

    public static final String AVATAR_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Soundcloud/images/avatars";

    public static final String EXTERNAL_CACHE_DIRECTORY = Environment.getExternalStorageDirectory()
            + "/Android/data/com.soundcloud.android/files/.cache/";

    public static final String EXTERNAL_CACHE_DIRECTORY_DEPRACATED = Environment
            .getExternalStorageDirectory()
            + "/Soundcloud";

    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment
            .getExternalStorageDirectory()
            + "/Soundcloud";

    public interface RequestCodes {
        public static final int GALLERY_IMAGE_PICK = 9000;

        public static final int REUATHORIZE = 9001;
    }

    public enum LoadType {
        incoming, exclusive, favorites
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

    public interface UserOrderBy {
        public final static int ALPHABETICAL = 0;

        public final static int TRACK_COUNT = 1;
    }

    public interface TrackOrderBy {
        public final static int ALPHABETICAL = 0;

        public final static int UPLOAD_DATE = 1;
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
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return new File(EXTERNAL_CACHE_DIRECTORY);
        } else {
            return c.getCacheDir();
        }
    }

    public static String getCacheDirPath(Context c) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return EXTERNAL_CACHE_DIRECTORY;
        } else {
            return c.getCacheDir().getAbsolutePath();
        }
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);

            }
        } catch (Exception e) {
            // TODO: handle exception
        }

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
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public static String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static LazyList createList(LazyActivity activity) {

        LazyList mList = new LazyList(activity);
        mList.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        mList.setOnItemClickListener(activity);
        mList.setFastScrollEnabled(true);
        mList.setTextFilterEnabled(true);
        mList.setDivider(activity.getResources().getDrawable(R.drawable.list_separator));
        mList.setDividerHeight(1);
        activity.registerForContextMenu(mList);

        return mList;
    }

    public static void createTabList(LazyActivity activity, FrameLayout listHolder,
            LazyEndlessAdapter adpWrap) {
        createTabList(activity, listHolder, adpWrap, -1, null);
    }

    public static void createTabList(LazyActivity activity, FrameLayout listHolder,
            LazyEndlessAdapter adpWrap, int listId) {
        createTabList(activity, listHolder, adpWrap, listId, null);
    }

    public static void createTabList(LazyActivity activity, FrameLayout listHolder,
            LazyEndlessAdapter adpWrap, int listId, OnTouchListener touchListener) {

        listHolder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        LazyList lv = ((LazyTabActivity) activity).buildList(false);
        if (listId != -1)
            lv.setId(listId);
        if (touchListener != null)
            lv.setOnTouchListener(touchListener);
        lv.setAdapter(adpWrap);
        activity.configureListMenu(lv, CloudUtils.LoadType.incoming);
        listHolder.addView(lv);
        adpWrap.createListEmptyView(lv);
    }

    public static FrameLayout createTabLayout(Context c) {
        return createTabLayout(c, false);
    }

    public static FrameLayout createTabLayout(Context context, Boolean scrolltabs) {
        FrameLayout tabLayout = new FrameLayout(context);
        tabLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (scrolltabs)
            inflater.inflate(R.layout.cloudscrolltabs, tabLayout);
        else
            inflater.inflate(R.layout.cloudtabs, tabLayout);

        // construct the tabhost
        final TabHost tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);

        FrameLayout frameLayout = (FrameLayout) tabLayout.findViewById(android.R.id.tabcontent);
        frameLayout.setPadding(0, 0, 0, 0);

        tabHost.setup();

        return tabLayout;

    }

    public static void configureTabs(Context context, TabWidget tabWidget, int height) {
        configureTabs(context, tabWidget, height, -1, false);
    }

    public static void configureTabs(Context context, TabWidget tabWidget, int height, int width) {
        configureTabs(context, tabWidget, height, -1, false);
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

    public static void createTab(Context context, TabHost tabHost, String tabId,
            String indicatorText, Drawable indicatorIcon, final ScTabView tabView,
            Boolean scrolltabs) {
        TabHost.TabSpec spec;

        spec = tabHost.newTabSpec(tabId);
        if (indicatorIcon == null)
            spec.setIndicator(indicatorText);
        else
            spec.setIndicator(indicatorText, indicatorIcon);

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
                            ((TextView) relativeLayout.getChildAt(j)).getLayoutParams().width = LayoutParams.FILL_PARENT;
                            ((TextView) relativeLayout.getChildAt(j)).getLayoutParams().height = LayoutParams.FILL_PARENT;
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

    public static long getCurrentTrackId() {
        if (sService != null) {
            try {
                return sService.getTrackId();
            } catch (RemoteException ex) {
            }
        }
        return -1;
    }

    public static String getCurrentUserPermalink() {
        if (CloudUtils.sService != null) {
            try {
                return sService.getUserPermalink();
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    public static boolean checkIconShouldLoad(String url) {
        if (url == null || url.contentEquals("") || url.toLowerCase().contentEquals("null")
                || url.contains("default_avatar"))
            return false;
        return true;
    }

    public static ICloudPlaybackService sService = null;

    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    public static boolean bindToService(Context context) {
        return bindToService(context, null);
    }

    public static boolean bindToService(Context context, ServiceConnection callback) {
        context.startService(new Intent(context, CloudPlaybackService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        sConnectionMap.put(context, sb);
        Log.i(TAG, "Bindingi service " + sConnectionMap.size());
        return context.bindService((new Intent()).setClass(context, CloudPlaybackService.class),
                sb, 0);
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

    public static String buildLocalAvatarUrl(String user_permalink) {
        return CloudUtils.AVATAR_DIRECTORY + "/" + user_permalink + ".jpg";
    }

    public static void resolveTrack(SoundCloudApplication context, Track track,
            WriteState writeState) {
        resolveTrack(context, track, writeState, null, null);
    }

    public static void resolveTrack(SoundCloudApplication context, Track track,
            WriteState writeState, long currentUserId) {
        resolveTrack(context, track, writeState, currentUserId, null);
    }

    // ---Make sure the database is up to date with this track info---
    public static void resolveTrack(SoundCloudApplication context, Track track,
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
    
                track.user_played = result.getInt(result.getColumnIndex("user_played")) == 1 ? true
                : false;
    
                if (writeState == WriteState.update_only || writeState == WriteState.all)
                    db.updateTrack(track);
    
            } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
                db.insertTrack(track);
            }
            result.close();
    
            if (openAdapter == null)
                db.close();
            else
                db = null;
    
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

    public static void resolveUser(SoundCloudApplication context, User user, WriteState writeState,
            Long userId) {
        resolveUser(context, user, writeState, userId, null);
    }

    // ---Make sure the database is up to date with this track info---
    public static void resolveUser(SoundCloudApplication context, User user, WriteState writeState,
            Long currentUserId, DBAdapter openAdapter) {
        DBAdapter db;
        if (openAdapter == null) {
            db = new DBAdapter(context);
            db.open();
        } else
            db = openAdapter;
        Cursor result = db.getUserById(user.id, currentUserId);
        if (result.getCount() != 0) {
            user.update(result); // update the parcelable with values from the
            // db

            if (writeState == WriteState.update_only || writeState == WriteState.all)
                db.updateUser(user, currentUserId.compareTo(user.id) == 0);

        } else if (writeState == WriteState.insert_only || writeState == WriteState.all) {
            db.insertUser(user, currentUserId.compareTo(user.id) == 0);
        }
        result.close();

        if (openAdapter == null)
            db.close();
        else
            db = null;

    }

    // ---Make sure the database is up to date with this track info---
    public static User resolveUserById(SoundCloudApplication context, long userId,
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

    public static boolean isLocalFile(String filename) {
        if (filename.startsWith("/"))
            return true;
        else
            return false;
    }

    static void setBackground(View v, Bitmap bm) {

        if (bm == null) {
            v.setBackgroundResource(0);
            return;
        }

        int vwidth = v.getWidth();
        int vheight = v.getHeight();
        int bwidth = bm.getWidth();
        int bheight = bm.getHeight();
        float scalex = (float) vwidth / bwidth;
        float scaley = (float) vheight / bheight;
        float scale = Math.max(scalex, scaley) * 1.3f;

        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap bg = Bitmap.createBitmap(vwidth, vheight, config);
        Canvas c = new Canvas(bg);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        ColorMatrix greymatrix = new ColorMatrix();
        greymatrix.setSaturation(0);
        ColorMatrix darkmatrix = new ColorMatrix();
        darkmatrix.setScale(.3f, .3f, .3f, 1.0f);
        greymatrix.postConcat(darkmatrix);
        ColorFilter filter = new ColorMatrixColorFilter(greymatrix);
        paint.setColorFilter(filter);
        Matrix matrix = new Matrix();
        matrix.setTranslate(-bwidth / 2, -bheight / 2); // move bitmap center to
        // origin
        matrix.postRotate(10);
        matrix.postScale(scale, scale);
        matrix.postTranslate(vwidth / 2, vheight / 2); // Move bitmap center to
        // view center
        c.drawBitmap(bm, matrix, paint);
        v.setBackgroundDrawable(new BitmapDrawable(bg));
    }

    public static String formatTimestamp(int ts) {
        int milliseconds = ts;
        int seconds = ((milliseconds / 1000) % 60);
        int minutes = ((milliseconds / 1000) / 60);

        String ts_formatted;
        if (seconds < 10)
            ts_formatted = String.format("%d.0%d ", minutes, seconds);
        else
            ts_formatted = String.format("%d.%d ", minutes, seconds);
        return ts_formatted;
    }

    public static void mapCommentsToAdapter(Comment[] comments,
            LazyExpandableBaseAdapter mExpandableAdapter, Boolean chronological) {
        /*
         * if (comments == null || comments.length == 0) return; Comment
         * threadData; ArrayList<Parcelable> commentData; for (Comment comment :
         * comments){ int ts =
         * Integer.parseInt(comment.getData(Comment.key_timestamp)); String
         * ts_formatted = formatTimestamp(ts); ArrayList<Parcelable> ls =
         * (ArrayList<Parcelable>) mExpandableAdapter.getGroupData(); int i = 0;
         * Boolean threadFound = false; if (ls != null){ Iterator<Parcelable> it
         * = ls.iterator(); while (it.hasNext()){ Parcelable threadItem =
         * it.next(); if (((Comment)
         * threadItem).getData(Comment.key_timestamp).contentEquals
         * (Integer.toString(ts))){ threadFound = true; break; } i++; } } if
         * (!threadFound){ threadData = new Comment(comment);
         * threadData.putData(Comment.key_timestamp_formatted, ts_formatted);
         * commentData = new ArrayList<Parcelable>();
         * commentData.add((Parcelable) comment); if (chronological &&
         * mExpandableAdapter.getGroupCount() > 0){ ArrayList<Parcelable>
         * groupData = (ArrayList<Parcelable>)
         * mExpandableAdapter.getGroupData(); int j = 0; while (j <
         * groupData.size() && Integer.parseInt(((Comment)
         * groupData.get(j)).getData(Comment.key_timestamp)) <
         * Integer.parseInt(comment.getData(Comment.key_timestamp))){ j++; } if
         * (j < groupData.size()){ mExpandableAdapter.getGroupData().add(j,
         * (Parcelable) threadData); mExpandableAdapter.getChildData().add(j,
         * commentData); } else {
         * mExpandableAdapter.getGroupData().add((Parcelable) threadData);
         * mExpandableAdapter.getChildData().add(commentData); } } else {
         * mExpandableAdapter.getGroupData().add((Parcelable) threadData);
         * mExpandableAdapter.getChildData().add(commentData); } } else {
         * threadData = (Comment) mExpandableAdapter.getGroupData().get(i);
         * threadData.putData(Comment.key_username,
         * comment.getData(Comment.key_username));
         * threadData.putData(Comment.key_body,
         * comment.getData(Comment.key_body));
         * threadData.putData(Comment.key_timestamp,
         * comment.getData(Comment.key_timestamp));
         * threadData.putData(Comment.key_timestamp_formatted, ts_formatted);
         * commentData = (ArrayList<Parcelable>)
         * mExpandableAdapter.getChildData().get(i);
         * commentData.add(0,(Parcelable) comment); } } //trim the first comment
         * off of each child so its not double represented for (int i = 0; i <
         * mExpandableAdapter.getChildData().size(); i++){
         * mExpandableAdapter.getChildData().get(i).remove(0); }
         * mExpandableAdapter.notifyDataSetChanged();
         */
    }

    static String getResponseBody(final HttpEntity entity) throws IOException, ParseException {

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }

        InputStream instream = entity.getContent();
        if (instream == null) {
            return "";
        }
        if (entity.getContentLength() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        String charset = getContentCharSet(entity);

        if (charset == null) {
            charset = HTTP.DEFAULT_CONTENT_CHARSET;
        }

        Reader reader = new InputStreamReader(instream, charset);
        StringBuilder buffer = new StringBuilder();
        try {
            char[] tmp = new char[1024];
            int l;
            while ((l = reader.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }

        } finally {
            reader.close();
        }
        return buffer.toString();

    }

    static String getContentCharSet(final HttpEntity entity) throws ParseException {

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        String charset = null;
        if (entity.getContentType() != null) {
            HeaderElement values[] = entity.getContentType().getElements();
            if (values.length > 0) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    charset = param.getValue();
                }
            }
        }
        return charset;

    }

    public static void gotoTrackUploader(Context context, String userPemalink) {
        Intent i = new Intent(context, UserBrowser.class);
        i.putExtra("userLoadPermalink", userPemalink);
        context.startActivity(i);
    }

    public static void gotoTrackDetails(Context context, Track track) {
        /*
         * Intent i = new Intent(context, TrackBrowser.class);
         * i.putExtra("track", track); if (track.comments != null)
         * i.putExtra("comments", track.comments); context.startActivity(i);
         */
    }

    public static String buildRequestPath(String mUrl, String order) {
        return buildRequestPath(mUrl, order, false);
    }

    public static String buildRequestPath(String mUrl, String order, boolean refresh) {

        String refreshAppend = "";
        if (refresh)
            refreshAppend = "&rand=" + Math.round(10000 * Math.random());

        if (order == null)
            if (refresh)
                return mUrl + "?rand=" + (10000 * Math.random());
            else
                return mUrl;

        return String.format(mUrl + "?order=%s", URLEncoder.encode(order) + refreshAppend);

    }

    public static String formatUsername(String username) {
        return username.replace(" ", "-");
    }

    public static String stripProtocol(String url) {
        return url.replace("http://www.", "").replace("http://", "");
    }

    public static File getCacheFile(String url) {
        return new File(EXTERNAL_CACHE_DIRECTORY + "/" + url.hashCode() + ".png");
    }

    public static String getCacheFileName(String url) {
        return url.hashCode() + ".png";
    }

    public static String toTitleCase(String str) {

        str = str.toLowerCase();
        int intTmp = str.charAt(0) - 32;

        char[] carray = str.toCharArray();
        carray[0] = (char) intTmp;

        return String.valueOf(carray);
    }

    public static String formatGraphicsUrl(String url, String targetSize) {
        // Log.i(TAG,"FOrmat Graphics URL " + url);
        // for (String size : GraphicsSizesLib){
        // url = url.replace(size, targetSize);
        // }
        return url.replace("large", targetSize);
    }

    @SuppressWarnings("unchecked")
    public static boolean isTaskFinished(AsyncTask lt) {
        if (lt == null)
            return true;

        return lt.getStatus() == AsyncTask.Status.FINISHED;

    }

    public static boolean isTaskPending(LoadTask lt) {
        if (lt == null)
            return false;

        return lt.getStatus() == AsyncTask.Status.PENDING;

    }

    static int getCardId(Context context) {
        ContentResolver res = context.getContentResolver();
        Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            id = c.getInt(0);
            c.close();
        }
        return id;
    }

    public static Long getPCMTime(File file, int sampleRate, int channels, int bitsPerSample) {
        return file.length() / (sampleRate * channels * bitsPerSample / 8);
    }

    public static Long getPCMTime(long bytes, int sampleRate, int channels, int bitsPerSample) {
        return bytes / (sampleRate * channels * bitsPerSample / 8);
    }

    public static BitmapFactory.Options determineResizeOptions(String imageUri, int targetWidth,
            int targetHeight, boolean crop) throws IOException {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream is = null;
        is = new FileInputStream(imageUri);

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
        bmp = null;
        System.gc();
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

    public static String formatString(String stringFormat, Object[] args) {
        sBuilder.setLength(0);
        return sFormatter.format(stringFormat, args).toString();
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
     * Reallocates an array with a new size, and copies the contents of the old
     * array to the new array.
     * 
     * @param oldArray the old array, to be reallocated.
     * @param newSize the new array size.
     * @return A new array with the same contents.
     */
    public static Object resizeArray(Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0)
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        return newArray;
    }

    public static String toCamelCase(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Check if a thread is alive accounting for nulls
     * 
     * @return boolean : is the thread alive
     */
    public static Boolean checkThreadAlive(Thread t) {
        return (t == null || !t.isAlive()) ? false : true;
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

    /** Show an event in the LogCat view, for debugging */
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
        emptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
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

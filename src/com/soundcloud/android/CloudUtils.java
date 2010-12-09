package com.soundcloud.android;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.TabHost.OnTabChangeListener;

import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.utils.AnimUtils;



public class CloudUtils {
	


	    private static final String TAG = "CloudUtils";
	    public static final String API_BASE = "http://api.soundcloud.com/";
		
		public static final String REQUEST_FORMAT = "json";
		
		public static final int GRAPHIC_DIMENSIONS_T500 = 500;
		public static final int GRAPHIC_DIMENSIONS_CROP = 400;
		public static final int GRAPHIC_DIMENSIONS_T300 = 300;
		public static final int GRAPHIC_DIMENSIONS_LARGE = 100;
		public static final int GRAPHIC_DIMENSIONS_T67 = 500;
		public static final int GRAPHIC_DIMENSIONS_BADGE = 500;
		public static final int GRAPHIC_DIMENSIONS_SMALL = 32;
		public static final int GRAPHIC_DIMENSIONS_TINY_ARTWORKS = 20;
		public static final int GRAPHIC_DIMENSIONS_TINY_AVATARS = 18;
		public static final int GRAPHIC_DIMENSIONS_MINI = 16;
		
		public static final String EXTRA_GROUP = "group";
		public static final String EXTRA_FILTER = "filter";
		public static final String EXTRA_TITLE = "title";

		public static final String MUSIC_DIRECTORY = Environment.getExternalStorageDirectory() + "/Soundcloud/music";
		public static final String ARTWORK_DIRECTORY = Environment.getExternalStorageDirectory() + "/Soundcloud/images/artwork";
		public static final String WAVEFORM_DIRECTORY = Environment.getExternalStorageDirectory() + "/Soundcloud/images/waveforms";
		public static final String AVATAR_DIRECTORY = Environment.getExternalStorageDirectory() + "/Soundcloud/images/avatars";
		public static final String CACHE_DIRECTORY = Environment.getExternalStorageDirectory() + "/.soundcloud-cache/";
	    
		public enum LoadType { 
			incoming, exclusive, favorites 
		}
	    
	    public enum Model {
	    	track,user,comment,event
	    }
	    
	    public interface Dialogs {
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
	        public final static int CHILD_MENU_BASE = 13; // this should be the last item
	    }
	    
	    public interface OptionsMenu {
	    	public static final int SETTINGS = 200;
	    	public static final int VIEW_CURRENT_TRACK = 201;
	    	public static final int REFRESH = 202;
	    }
	    
	    public interface ContextMenu {
	    	
	    	public static final int CLOSE = 100;
	    	
	    	//basic track functions
	    	public static final int VIEW_TRACK = 110;
	    	public static final int PLAY_TRACK = 111;
	    	public static final int ADD_TO_PLAYLIST = 112;
	    	public static final int VIEW_UPLOADER = 113;
	    	public static final int DELETE = 114;
	    	public static final int RE_DOWNLOAD = 115;
	    	
	    	
	    	
	    	//pending download functions
	    	public static final int CANCEL_DOWNLOAD = 120;
	    	public static final int RESTART_DOWNLOAD = 121;
	    	public static final int FORCE_DOWNLOAD = 132;
	    	
	    	//downloaded functions
	    	public static final int DELETE_TRACK = 130;
	    	public static final int REFRESH_TRACK_DATA = 131;
	    	
	    	//comment functions
	    	public static final int PLAY_FROM_COMMENT_POSITION = 140;
	    	public static final int REPLY_TO_COMMENT = 141;
	    	public static final int VIEW_COMMENTER = 142;
	    	
	    	//playlist functions
	    	public static final int REMOVE_TRACK = 151;
	    	public static final int REMOVE_OTHER_TRACKS = 152;
	    	
	    	//basic user functions
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
	    
	    public final static String[] GraphicsSizesLib = {GraphicsSizes.t500,
	    	GraphicsSizes.crop,
	    	GraphicsSizes.t300,
	    	GraphicsSizes.large,
	    	GraphicsSizes.t67,
	    	GraphicsSizes.badge,
	    	GraphicsSizes.small,
	    	GraphicsSizes.tiny,
	    	GraphicsSizes.mini,
	    	GraphicsSizes.original};
	    
	    
	    
	    protected static void createTabList(LazyActivity activity, FrameLayout listHolder, LazyEndlessAdapter adpWrap){
			 
			listHolder.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
			LazyList lv = activity.buildList();
		    lv.setAdapter(adpWrap);
		    listHolder.addView(lv);
		    adpWrap.createListEmptyView(lv);
		}
		
	    protected static FrameLayout createTabLayout(Context c, final ViewFlipper viewFlipper){
			return createTabLayout(c, viewFlipper, false);
		}
		
		
		protected static FrameLayout createTabLayout(Context context, final ViewFlipper viewFlipper, Boolean scrolltabs){
			FrameLayout tabLayout = new FrameLayout(context);
			tabLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (scrolltabs)
				inflater.inflate(R.layout.cloudscrolltabs, tabLayout);
			else
				inflater.inflate(R.layout.cloudtabs, tabLayout);
			
			
			
		
			//construct the tabhost
			final TabHost tabHost = (TabHost) tabLayout.findViewById(android.R.id.tabhost);
			
			FrameLayout frameLayout = (FrameLayout) tabLayout.findViewById(android.R.id.tabcontent);
		    frameLayout.setPadding(0, 0, 0, 0);
		    
		    tabHost.setOnTabChangedListener(new OnTabChangeListener() {
				
				
				   @Override
				  public void onTabChanged(String arg0) {
					   Log.i("DEBUG","SIZE IS " + tabHost.getTabContentView().getHeight());
					   
					   if (tabHost.getCurrentTab() < 0)
						   return;
					   
					   switch (viewFlipper.getDisplayedChild() - tabHost.getCurrentTab()){
/*					   		case 1 :
					   			viewFlipper.setInAnimation(AnimUtils.inFromLeftAnimation());
					   			viewFlipper.setOutAnimation(AnimUtils.outToRightAnimation());
					   			break;
					   		case -1:
					   			viewFlipper.setInAnimation(AnimUtils.inFromRightAnimation());
					   			viewFlipper.setOutAnimation(AnimUtils.outToLeftAnimation());
					   			break;*/
					   		default:
					   			viewFlipper.setInAnimation(null);
					   			viewFlipper.setOutAnimation(null);
					   			break;
					   }
					   
					   Log.i("REFRESH","ON TAB CHANGED " + viewFlipper.getCurrentView());
					   if (viewFlipper.getCurrentView() == null)
						   return;
					   
					   ((ScTabView) viewFlipper.getCurrentView()).onStop();
					   viewFlipper.setDisplayedChild(tabHost.getCurrentTab());
					   ((ScTabView) viewFlipper.getCurrentView()).onStart();
				  }     
			});  
		   
		 tabHost.setup();
		    
		    return tabLayout;
		    
		}
		
		protected static void configureTabs(Context context, TabWidget tabWidget, int height){
			   
			  // Convert the tabHeight depending on screen density
		    final float scale = context.getResources().getDisplayMetrics().density;
		    height = (int) (scale * height);
		
		    for (int i = 0; i < tabWidget.getChildCount(); i++) {
		    	tabWidget.getChildAt(i).getLayoutParams().height = height;
		    }
		}
				
		
		protected static void createTab(Context context, TabHost tabHost, String tabId, String indicatorText, Drawable indicatorIcon, final ScTabView tabContent, final ViewFlipper vf, Boolean scrolltabs)
		{
			TabHost.TabSpec spec;
		    
			Log.i("ASDF","Create New Tab with drawable " + indicatorIcon);
			
		    spec = tabHost.newTabSpec(tabId);
		    if (indicatorIcon == null)
		    	spec.setIndicator(indicatorText);
		    else
		    	spec.setIndicator(indicatorText, indicatorIcon);
		    
		    vf.addView(tabContent);
		    spec.setContent(new TabHost.TabContentFactory(){
		        public View createTabContent(String tag)
		        { 
		        	Log.i("DEBUG","RETURNING THIS THING " + tag);
		        	//tabContent.onVisible();
		       	 	return vf;
		       	 	
		        }
		    }); 
		    
		    tabHost.addTab(spec);
		}
		
		protected static void setTabTextStyle(Context context, TabWidget tabWidget){
			 // a hacky way of setting the font of the indicator texts
	        for (int i = 0; i < tabWidget.getChildCount(); i++) {
	                if (tabWidget.getChildAt(i) instanceof RelativeLayout) {
	                        RelativeLayout relativeLayout = (RelativeLayout) tabWidget
	                                        .getChildAt(i);
	                        for (int j = 0; j < relativeLayout.getChildCount(); j++) {
	                                if (relativeLayout.getChildAt(j) instanceof TextView) {
	                                        ((TextView) relativeLayout.getChildAt(j)).setTextAppearance(context,R.style.TabWidgetTextAppearance);
	                                }
	                        }

	                }
	        }
		}
	    
	    
	    
	    
		
	    

//	    public static String makeAlbumsLabel(Context context, int numalbums, int numsongs, boolean isUnknown) {
//	        // There are two formats for the albums/songs information:
//	        // "N Song(s)"  - used for unknown artist/album
//	        // "N Album(s)" - used for known albums
//	        
//	        StringBuilder songs_albums = new StringBuilder();
//
//	        Resources r = context.getResources();
//	        if (isUnknown) {
//	            if (numsongs == 1) {
//	                songs_albums.append(context.getString(R.string.onesong));
//	            } else {
//	                String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
//	                sFormatBuilder.setLength(0);
//	                sFormatter.format(f, Integer.valueOf(numsongs));
//	                songs_albums.append(sFormatBuilder);
//	            }
//	        } else {
//	            String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
//	            sFormatBuilder.setLength(0);
//	            sFormatter.format(f, Integer.valueOf(numalbums));
//	            songs_albums.append(sFormatBuilder);
//	            songs_albums.append(context.getString(R.string.albumsongseparator));
//	        }
//	        return songs_albums.toString();
//	    }

	    /**
	     * This is now only used for the query screen
	     */
//	    public static String makeAlbumsSongsLabel(Context context, int numalbums, int numsongs, boolean isUnknown) {
//	        // There are several formats for the albums/songs information:
//	        // "1 Song"   - used if there is only 1 song
//	        // "N Songs" - used for the "unknown artist" item
//	        // "1 Album"/"N Songs" 
//	        // "N Album"/"M Songs"
//	        // Depending on locale, these may need to be further subdivided
//	        
//	        StringBuilder songs_albums = new StringBuilder();
//
//	        if (numsongs == 1) {
//	            songs_albums.append(context.getString(R.string.onesong));
//	        } else {
//	            Resources r = context.getResources();
//	            if (! isUnknown) {
//	                String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
//	                sFormatBuilder.setLength(0);
//	                sFormatter.format(f, Integer.valueOf(numalbums));
//	                songs_albums.append(sFormatBuilder);
//	                songs_albums.append(context.getString(R.string.albumsongseparator));
//	            }
//	            String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
//	            sFormatBuilder.setLength(0);
//	            sFormatter.format(f, Integer.valueOf(numsongs));
//	            songs_albums.append(sFormatBuilder);
//	        }
//	        return songs_albums.toString();
//	    }
	    
	    
	    public static String getCurrentUserId(Context context){
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
     		return preferences.getString("currentUserId", "");
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

	    public static boolean deleteDir(File dir) {
	        if (dir!=null && dir.isDirectory()) {
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
	    
	    
	    
	    public static boolean checkIconShouldLoad(String url) {
	        if (url == null || url.contentEquals("") || url.toLowerCase().contentEquals("null") || url.contains("default_avatar"))
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
	        return context.bindService((new Intent()).setClass(context,
	        		CloudPlaybackService.class), sb, 0);
	    }
	    
	    public static void unbindFromService(Context context) {
	        ServiceBinder sb = (ServiceBinder) sConnectionMap.remove(context);
	        if (sb == null) {
	            return;
	        }
	        context.unbindService(sb);
	        if (sConnectionMap.isEmpty()) {
	            // presumably there is nobody interested in the service at this point,
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
	    
	    public static String getCurrentTrackId() {
	        if (sService != null) {
	            try {
	                return sService.getTrackId();
	            } catch (RemoteException ex) {
	            }
	        }
	        return null;
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

	    
	    
	   
	    
	    /*
	     * Returns true if a file is currently opened for playback (regardless
	     * of whether it's playing or paused).
	     */
	    public static boolean isMusicLoaded() {
	        if (CloudUtils.sService != null) {
	            try {
	                return sService.getPath() != null;
	            } catch (RemoteException ex) {
	            }
	        }
	        return false;
	    }

	    private final static long [] sEmptyList = new long[0];
	    
	    public static long [] getSongListForCursor(Cursor cursor) {
	        if (cursor == null) {
	            return sEmptyList;
	        }
	        int len = cursor.getCount();
	        long [] list = new long[len];
	        cursor.moveToFirst();
	        int colidx = -1;
	        try {
	            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID);
	        } catch (IllegalArgumentException ex) {
	            colidx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
	        }
	        for (int i = 0; i < len; i++) {
	            list[i] = cursor.getLong(colidx);
	            cursor.moveToNext();
	        }
	        return list;
	    }

	  
	    
	    public static Track setDownloadPaths(Track track){
	    	String userDirectory = track.getData(Track.key_user_permalink);
	    	String filename = track.getData(Track.key_id);
			track.putData(Track.key_local_play_url, CloudUtils.MUSIC_DIRECTORY + "/" + userDirectory + "/" + filename + ".mp3");
			track.putData(Track.key_local_artwork_url, CloudUtils.ARTWORK_DIRECTORY + "/" + userDirectory + "/" +  filename + ".png");
			track.putData(Track.key_local_waveform_url, CloudUtils.WAVEFORM_DIRECTORY + "/" + userDirectory + "/" +  filename + ".jpg");
			return track;
	    }
	    
	    public static String buildLocalAvatarUrl(String user_permalink){
	    	return CloudUtils.AVATAR_DIRECTORY + "/" + user_permalink + ".jpg";
	    }
	    
	   
	    
	  //---Make sure the database is up to date with this track info---
	    public static void resolveTrack(Context context, Track track, Boolean writeToDB, String currentUserId) 
	    {
	    	
	    	track = setDownloadPaths(track);
	    	
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Log.i(TAG,"Resolve Track " + track.getData(Track.key_title) + " listened: " + track.getData(Track.key_user_played));
			
			Cursor result = db.getTrackById(track.getData(Track.key_id), currentUserId);
			if (result.getCount() != 0){
				
				//add local urls and update database
				result.moveToFirst();
				if (result.getColumnIndex(Track.key_user_favorite_id) > -1)
					track.putData(Track.key_user_favorite_id, result.getString(result.getColumnIndex(Track.key_user_favorite_id)));
				
				
				track.putData(Track.key_user_played, result.getString(result.getColumnIndex(Track.key_user_played)));
				
				if (writeToDB)
				db.updateTrack(track);
				
			} else if (writeToDB){
				db.insertTrack(track);	
			}
			result.close();
			db.close();

			track = resolveTrackData(track);
			
			HashMap<String,String> userinfo = new HashMap<String,String>();
			userinfo.put(User.key_id,track.getData(Track.key_user_id));
			userinfo.put(User.key_permalink,track.getData(Track.key_user_permalink));
			userinfo.put(User.key_username,track.getData(Track.key_username));
			userinfo.put(User.key_avatar_url,track.getData(Track.key_user_avatar_url));
			
			resolveUser(context,userinfo, writeToDB, currentUserId);
	    }
	    
	    public static Track resolveTrackData(Track track){
	    	track = resolvePlayUrl(track);
			track = resolveTrackFavorite(track);
			return track;
	    }
	    
	    //---Make sure the database is up to date with this track info---
	    public static Track resolveTrackById(Context context, String trackId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getTrackById(trackId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				String[] keys = result.getColumnNames();
				Track track = new Track();
				for (String key : keys) {
					//Log.i("asdf","Resolve track put data " + key+ " " + result.getString(result.getColumnIndex(key)));
					track.putData(key, result.getString(result.getColumnIndex(key)));
				}
					
				
				track = resolvePlayUrl(track);
				track = resolveTrackFavorite(track);
				
				result.close();
				
				result = db.getUserByPermalink(track.getData(Track.key_user_permalink), currentUserId);
				
				if (result.getCount() != 0){
					result.moveToFirst();
					track.putData(Track.key_username, result.getString(result.getColumnIndex(User.key_username)));
					track.putData(Track.key_user_permalink, result.getString(result.getColumnIndex(User.key_permalink)));
				}
				
				result.close();
				db.close();
				
				return track;
			}
			
	    	result.close();
			db.close();
			
			return null;
			
	    }
	    
	    public static Track resolvePlayUrl(Track track){
	    	
	    	
	    	//figure out the ideal play URL
			if (track.getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED))
				track.putData(Track.key_play_url, track.getData(Track.key_local_play_url));
			else if (!stringNullEmptyCheck(track.getData(Track.key_stream_url)))
				track.putData(Track.key_play_url, track.getData(Track.key_stream_url));
			else if (!stringNullEmptyCheck(track.getData(Track.key_download_url))){
				track.putData(Track.key_play_url, track.getData(Track.key_download_url));
			}
			
			return track;
	    }
	    
	    public static Track resolvePreviousDownload(Track track){
	    	
	    	//figure out the ideal play URL
			if (!stringNullEmptyCheck(track.getData(Track.key_local_play_url))){
				String userDirectory = track.getData(Track.key_user_permalink);
		    	String filename = track.getData(Track.key_id);
		    	File checkFile = new File(CloudUtils.MUSIC_DIRECTORY + "/" + userDirectory + "/" + filename + ".mp3");
		    	if (checkFile.exists()){
					track.putData(Track.key_play_url, track.getData(Track.key_local_play_url));
					track.putData(Track.key_download_status, Track.DOWNLOAD_STATUS_DOWNLOADED);
				}

			}
	    				
			return track;
	    }
	    		    
	    
	    public static Track resolveTrackFavorite(Track track){
	    	if (!stringNullEmptyCheck(track.getData(Track.key_user_favorite_id)))
				track.putData(Track.key_user_favorite, "true");
	    	
	    	return track;
	    }
	    
	    //---Make sure the database is up to date with this track info---
	    public static void addTrackFavorite(Context context, String trackId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getTrackById(trackId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				if (stringNullEmptyCheck(result.getString(result.getColumnIndex(Track.key_user_favorite_id)))){
					db.insertFavorite(currentUserId, trackId);
				}
				
			}
			
	    	result.close();
			db.close();
	    }
	    
	    //---Make sure the database is up to date with this track info---
	    public static void removeTrackFavorite(Context context, String trackId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getTrackById(trackId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				if (!stringNullEmptyCheck(result.getString(result.getColumnIndex(Track.key_user_favorite_id)))){
					db.removeFavorite(currentUserId, trackId);
				}
				
			}
			
	    	result.close();
			db.close();
	    }
	 
	    
	    //---Make sure the database is up to date with this track info---
	    public static User resolveUser(Context context, User user, Boolean writeToDB, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getUserByPermalink(user.getData(User.key_permalink),currentUserId);
			if (result.getCount() != 0){
				result.moveToFirst();
				user = resolveUserFromCursor(result,user);
				user = resolveUserData(user);
				if (writeToDB) db.updateUser(user);
			} else if (writeToDB){
				db.insertUser(user);	
			}
	    	result.close();
			db.close();
			
			
			
			return user;
	    }
	    
	 
	    
	    //---Make sure the database is up to date with this track info---
	    public static HashMap<String,String> resolveUser(Context context, HashMap<String, String> userinfo, Boolean writeToDB, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			Log.i("DEBUG","Getting user from database " + userinfo.get(User.key_permalink));
			Cursor result = db.getUserByPermalink(userinfo.get(User.key_permalink), currentUserId);
			if (result.getCount() != 0){
				
				result.moveToFirst();
				userinfo = resolveUserFromCursor(result,userinfo);
				userinfo = resolveUserData(userinfo);
				
				if (writeToDB)
					db.updateUser(userinfo);
				
			} else if (writeToDB) {
				db.insertUser(userinfo);
				userinfo = resolveUserData(userinfo);
			}
	    	result.close();
			db.close();
			
		
			return userinfo;
	    }
	    
	  //---Make sure the database is up to date with this track info---
	    public static User resolveUserById(Context context, String userId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getUserById(userId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				String[] keys = result.getColumnNames();
				User user = new User();
				for (String key : keys) {
					user.putData(key, result.getString(result.getColumnIndex(key)));
				}
					
				user = resolveUserFromCursor(result,user);
				user = resolveUserData(user);
				
				result.close();
				
				return user;
			}
			
	    	result.close();
			db.close();
			
			return null;
			
	    }
	    
	    //---Make sure the database is up to date with this track info---
	    public static User resolveUserByPermalink(Context context, String userPermalink, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getUserByPermalink(userPermalink, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				String[] keys = result.getColumnNames();
				User user = new User();
				for (String key : keys) {
					user.putData(key, result.getString(result.getColumnIndex(key)));
				}
					
				user = resolveUserFromCursor(result,user);
				user = resolveUserData(user);
				
				result.close();
				db.close();
				return user;
			}
			
	    	result.close();
			db.close();
			
			return null;
			
	    }
	    
	    public static User resolveUserFromCursor(Cursor result, User user){
	    	if (result.getColumnIndex(User.key_user_following_id) > -1)
	    	user.putData(User.key_user_following_id, result.getString(result.getColumnIndex(User.key_user_following_id)));
	    	return user;
	    }
	    
	    public static User resolveUserData(User user){
	    	if (user.getData(User.key_user_following_id) != "")
				user.putData(User.key_user_following, "true");
	    	
	    	return user;
	    }
	    
	    public static HashMap<String,String> resolveUserData(HashMap<String,String> userinfo){
	    	if (userinfo.get(User.key_user_following_id) != "")
	    		userinfo.put(User.key_user_following, "true");
	    	
	    	return userinfo;
	    }
	    
	    public static HashMap<String,String> resolveUserFromCursor(Cursor result, HashMap<String,String> userinfo){
	    
	    	if (result.getColumnIndex(User.key_user_following_id) > -1)
	    	userinfo.put(User.key_user_following_id, result.getString(result.getColumnIndex(User.key_user_following_id)));
	    	
	    	if (userinfo.get(User.key_user_following_id) != "")
	    		userinfo.put(User.key_user_following, "true");
	    	
	    	return userinfo;
	    }
	    
	    
	    //---Make sure the database is up to date with this track info---
	    public static void addUserFollowing(Context context, String userId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getUserById(userId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				if (stringNullEmptyCheck(result.getString(result.getColumnIndex(User.key_user_following_id)))){
					db.insertFollowing(currentUserId, userId);
				}
			}
			
	    	result.close();
			db.close();
	    }
	    
	    //---Make sure the database is up to date with this track info---
	    public static void removeUserFollowing(Context context, String userId, String currentUserId) 
	    {
	    	DBAdapter db = new DBAdapter(context);
			db.open();
			
			Cursor result = db.getUserById(userId, currentUserId);
			
			if (result.getCount() != 0){
				result.moveToFirst();
				if (!stringNullEmptyCheck(result.getString(result.getColumnIndex(User.key_user_following_id)))){
					Log.i("asdffdsa","remove folowing " + db.removeFollowing(currentUserId, userId));
				}
				
			}
			
	    	result.close();
			db.close();
	    }
	    
	    
	    public static Boolean stringNullEmptyCheck(String s){
	    	return stringNullEmptyCheck(s,false);
	    }
	    
	    public static Boolean stringNullEmptyCheck(String s, Boolean enforceStringNull){
	    	if (s == null)
	    		return true;
	    	
	    	if (s.length() == 0)
	    		return true;
	    	
	    	if (s.trim().length() == 0)
	    		return true;
	    	
	    	if (enforceStringNull)
	    	if (s.toLowerCase().contentEquals("null"))
	    		return true;
	    	
	    	return false;
	    	
	    }
	    
	    
	    public static String getLocationString(String city, String country){
	    	
	    	
	    	 if (!stringNullEmptyCheck(city) && !stringNullEmptyCheck(country)){
				 return city + ", " + country;
			    } else if (!stringNullEmptyCheck(city)){
			    	return city;
			    }else if (!stringNullEmptyCheck(country)){
			    	return country;
			    }
	    	 
			return ""; 
			 
	    }
	    
	   
	    static protected Uri getContentURIForPath(String path) {
	        return Uri.fromFile(new File(path));
	    }

	    
	    /*  Try to use String.format() as little as possible, because it creates a
	     *  new Formatter every time you call it, which is very inefficient.
	     *  Reusing an existing Formatter more than tripled the speed of
	     *  makeTimeString().
	     *  This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
	     */
	    private static StringBuilder sFormatBuilder = new StringBuilder();
	    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
	    private static final Object[] sTimeArgs = new Object[5];

	    public static String makeTimeString(Context context, long secs) {
	        String durationformat = context.getString(
	                secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);
	        
	        /* Provide multiple arguments so the format can be changed easily
	         * by modifying the xml.
	         */
	        sFormatBuilder.setLength(0);

	        final Object[] timeArgs = sTimeArgs;
	        timeArgs[0] = secs / 3600;
	        timeArgs[1] = secs / 60;
	        timeArgs[2] = (secs / 60) % 60;
	        timeArgs[3] = secs;
	        timeArgs[4] = secs % 60;

	        return sFormatter.format(durationformat, timeArgs).toString();
	    }
	    
	    public static void shuffleAll(Context context, Cursor cursor) {
	        playAll(context, cursor, 0, true);
	    }

	    public static void playAll(Context context, Cursor cursor) {
	        playAll(context, cursor, 0, false);
	    }
	    
	    public static void playAll(Context context, Cursor cursor, int position) {
	        playAll(context, cursor, position, false);
	    }
	    
	    public static void playAll(Context context, long [] list, int position) {
	        playAll(context, list, position, false);
	    }
	    
	    private static void playAll(Context context, Cursor cursor, int position, boolean force_shuffle) {
	    
	        long [] list = getSongListForCursor(cursor);
	        playAll(context, list, position, force_shuffle);
	    }
	    
	    private static void playAll(Context context, long [] list, int position, boolean force_shuffle) {
	        if (list.length == 0 || sService == null) {
//	            Log.d("CloudUtils", "attempt to play empty song list");
//	            // Don't try to play empty playlists. Nothing good will come of it.
//	            String message = context.getString(R.string.emptyplaylist, list.length);
//	            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
//	            return;
//	        }
//	        try {
//	            if (force_shuffle) {
//	                sService.setShuffleMode(CloudPlaybackService.SHUFFLE_NORMAL);
//	            }
//	            long curid = sService.getAudioId();
//	            int curpos = sService.getQueuePosition();
//	            if (position != -1 && curpos == position && curid == list[position]) {
//	                // The selected file is the file that's currently playing;
//	                // figure out if we need to restart with a new playlist,
//	                // or just launch the playback activity.
//	                long [] playlist = sService.getQueue();
//	                if (Arrays.equals(list, playlist)) {
//	                    // we don't need to set a new list, but we should resume playback if needed
//	                    sService.play();
//	                    return; // the 'finally' block will still run
//	                }
//	            }
//	            if (position < 0) {
//	                position = 0;
//	            }
//	            sService.open(list, force_shuffle ? -1 : position);
//	            sService.play();
//	        } catch (RemoteException ex) {
//	        } finally {
////	            Intent intent = new Intent(context, MediaPlaybackActivity.class)
////	                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////	            context.startActivity(intent);
	        }
	    }
	    
	    public static void clearQueue() {
	        try {
	            sService.removeTracks(0, Integer.MAX_VALUE);
	        } catch (RemoteException ex) {
	        }
	    }
	    
	    // A really simple BitmapDrawable-like class, that doesn't do
	    // scaling, dithering or filtering.
	    private static class FastBitmapDrawable extends Drawable {
	        private Bitmap mBitmap;
	        public FastBitmapDrawable(Bitmap b) {
	            mBitmap = b;
	        }
	        @Override
	        public void draw(Canvas canvas) {
	            canvas.drawBitmap(mBitmap, 0, 0, null);
	        }
	        @Override
	        public int getOpacity() {
	            return PixelFormat.OPAQUE;
	        }
	        @Override
	        public void setAlpha(int alpha) {
	        }
	        @Override
	        public void setColorFilter(ColorFilter cf) {
	        }
	    }
	    
	   

//	   
	    
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
	        matrix.setTranslate(-bwidth/2, -bheight/2); // move bitmap center to origin
	        matrix.postRotate(10);
	        matrix.postScale(scale, scale);
	        matrix.postTranslate(vwidth/2, vheight/2);  // Move bitmap center to view center
	        c.drawBitmap(bm, matrix, paint);
	        v.setBackgroundDrawable(new BitmapDrawable(bg));
	    }
	    
	    
	    
	    
	    static HashMap<String,String> mapUserFromJSON(Context context, JSONObject userObject) throws JSONException {
			HashMap<String, String> userInfo = new HashMap<String, String>();
//			
			 String location = "";
			 if ((userObject.getString(User.key_city) != "" && userObject.getString(User.key_country) != "") && (!userObject.getString(User.key_city).equalsIgnoreCase("null") && !userObject.getString(User.key_country).equalsIgnoreCase("null"))){
				 location = userObject.getString(User.key_city) + ", " + userObject.getString(User.key_country);
			    } else if (userObject.getString(User.key_city) != ""){
			    	location = userObject.getString(User.key_city);
			    }else if (userObject.getString(User.key_country) != ""){
			    	location = userObject.getString(User.key_country);
			    } else {
			    	//removeView(_lblLocation);
			    }
			
			 
		 	Iterator<String> keys = userObject.keys();
		 	while(keys.hasNext()) {
		 		String key = (String)keys.next();
		 		if (userObject.has(key) && userObject.getString(key) != "null"){
		 			userInfo.put(key, userObject.getString(key));
		 		}	
		 	}
			 
			userInfo.put(User.key_location, location);
			userInfo.put(User.key_track_count, userObject.getString(User.key_track_count) + " tracks");
			userInfo.put(User.key_followers_count, userObject.getString(User.key_followers_count) + " followers");
			userInfo.put(User.key_avatar_url, userObject.getString(User.key_avatar_url));
			
			
			if (userObject.has(User.key_user_following))
			if (userObject.get(User.key_user_following) != null)
				userInfo.put(User.key_user_following, userObject.getString(User.key_user_following));

			return userInfo;
		}
	    
	    
		static Track mapTrackFromJSON(Context context, JSONObject songObject) throws JSONException {
			
			Boolean _isPlaylist = false;
			Boolean _isFavorite = false;
			
			if (songObject.has(Track.key_type)){
				//is it a playlist
				if (((String) songObject.get(Track.key_type)).equalsIgnoreCase("playlist")){
					_isPlaylist = true;
				}
				//is it a favorited track
				if (((String) songObject.get(Track.key_type)).equalsIgnoreCase("favorite")){
					songObject.put(Track.key_favorited_by, context.getResources().getString(R.string.favorited_by) + " " + songObject.getString(Track.key_favorited_by));
				} 
			}
			
			if (_isPlaylist){
				String durationStr = songObject.getJSONArray("tracks").length() + " " + context.getResources().getString(R.string.tracks);
				songObject.put(Track.key_duration_formatted, durationStr);
			} else {
				int duration = Integer.parseInt(songObject.getString(Track.key_duration));
				String durationStr = String.valueOf((int) Math.floor((duration/1000)/60)) + "." + String.format("%02d",(int) (duration/1000)%60);
				songObject.put(Track.key_duration_formatted, durationStr);
				
				if (songObject.getString(Track.key_streamable).toString().equalsIgnoreCase("true")){
		    		songObject.put("streamable", "true");
		    		songObject.put("play_url", songObject.getString(Track.key_stream_url));
		    		
		    	} else if (songObject.getString(Track.key_downloadable).toString().equalsIgnoreCase("true")){
					songObject.put("downloadable", "true");
					songObject.put("play_url", songObject.getString(Track.key_download_url));
		    	}
			}
			
			return new Track(songObject);
		
		}
		
		static HashMap<String, String> mapCommentFromJSON(Context context, JSONObject commentObject) throws JSONException {
			
			JSONObject userObject = commentObject.getJSONObject(Comment.key_user);
			HashMap<String,String> commentInfo = new HashMap<String,String>();
			
			commentInfo.put(Comment.key_username, userObject.getString(Comment.key_username));
			commentInfo.put(Comment.key_user_id, userObject.getString(User.key_id));
			commentInfo.put(Comment.key_user_permalink, userObject.getString(User.key_permalink));
			commentInfo.put(Comment.key_body, commentObject.getString(Comment.key_body));
			
			if ( commentObject.getString(Comment.key_timestamp) == "null" ||  commentObject.getString(Comment.key_timestamp) == null ||  commentObject.getString(Comment.key_timestamp) == "")
				commentInfo.put(Comment.key_timestamp, "-1");
			else
				commentInfo.put(Comment.key_timestamp, commentObject.getString(Comment.key_timestamp));
			
			
			return commentInfo;
		}
		
		
		
		static String formatTimestamp(int ts){
			int milliseconds = ts;
			int seconds = (int) ((milliseconds / 1000) % 60);
			int minutes = (int) ((milliseconds / 1000) / 60);
			
			String ts_formatted;
			if (seconds < 10)
				ts_formatted = String.format("%d.0%d ", minutes,seconds);
			else
				ts_formatted = String.format("%d.%d ", minutes,seconds);
			return ts_formatted;
		}
		
		
		static void mapCommentsToAdapter(Comment[] comments, LazyExpandableBaseAdapter mExpandableAdapter, Boolean chronological){
			
			if (comments == null || comments.length == 0)
				return;
			
			Comment threadData;
			ArrayList<Parcelable> commentData;
			
			for (Comment comment : comments){
				
				int ts = Integer.parseInt(comment.getData(Comment.key_timestamp));
				String ts_formatted = formatTimestamp(ts);
				
				ArrayList<Parcelable> ls = (ArrayList<Parcelable>) mExpandableAdapter.getGroupData();
				
				int i = 0;
				Boolean threadFound = false;
				if (ls != null){
					Iterator<Parcelable> it = ls.iterator();
					while (it.hasNext()){
						
						Parcelable threadItem = it.next();
						if (((Comment) threadItem).getData(Comment.key_timestamp).contentEquals(Integer.toString(ts))){
							threadFound = true;
							break;
						}
						i++;
					}
				}
				if (!threadFound){
					threadData = new Comment(comment.mapData());
					threadData.putData(Comment.key_timestamp_formatted, ts_formatted);
					
					commentData = new ArrayList<Parcelable>();
					commentData.add((Parcelable) comment);
					
					if (chronological && mExpandableAdapter.getGroupCount() > 0){
						ArrayList<Parcelable> groupData = (ArrayList<Parcelable>) mExpandableAdapter.getGroupData();
						int j = 0;
						while (j < groupData.size() && Integer.parseInt(((Comment) groupData.get(j)).getData(Comment.key_timestamp)) < Integer.parseInt(comment.getData(Comment.key_timestamp))){
							j++;
						}
						if (j < groupData.size()){
							mExpandableAdapter.getGroupData().add(j, (Parcelable) threadData);
							mExpandableAdapter.getChildData().add(j, commentData);	
						} else {
							mExpandableAdapter.getGroupData().add((Parcelable) threadData);
							mExpandableAdapter.getChildData().add(commentData);
						}
						
					} else {
						mExpandableAdapter.getGroupData().add((Parcelable) threadData);
						mExpandableAdapter.getChildData().add(commentData);
					}
					
					
					
				} else {
					threadData = (Comment) mExpandableAdapter.getGroupData().get(i);
					threadData.putData(Comment.key_username, comment.getData(Comment.key_username));
					threadData.putData(Comment.key_body, comment.getData(Comment.key_body));
					threadData.putData(Comment.key_timestamp, comment.getData(Comment.key_timestamp));
					threadData.putData(Comment.key_timestamp_formatted, ts_formatted);
					commentData = (ArrayList<Parcelable>) mExpandableAdapter.getChildData().get(i);
					commentData.add(0,(Parcelable) comment);
				}
				
			}
			
			//trim the first comment off of each child so its not double represented
			for (int i = 0; i < mExpandableAdapter.getChildData().size(); i++){
				mExpandableAdapter.getChildData().get(i).remove(0);
			}
			
			mExpandableAdapter.notifyDataSetChanged();
		}
		
		static String getResponseBody(final HttpEntity entity) throws IOException, ParseException {

			if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
			
			InputStream instream = entity.getContent();
			if (instream == null) { return ""; }
			if (entity.getContentLength() > Integer.MAX_VALUE) { throw new IllegalArgumentException("HTTP entity too large to be buffered in memory"); }
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

			if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
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
		
		static String getTrackArtworkPath(HashMap<String, String> trackinfo) {
			if (!trackinfo.get(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED) || trackinfo.get(Track.key_download_error).contentEquals("true") || trackinfo.get(Track.key_local_artwork_url).contentEquals(""))
				return trackinfo.get(Track.key_artwork_url);
			else
				return trackinfo.get(Track.key_local_artwork_url);
		}
		
		static String getTrackArtworkPath(Track trackinfo) {
			if (!trackinfo.getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED) || trackinfo.getData(Track.key_download_error).contentEquals("true") || trackinfo.getData(Track.key_local_artwork_url).contentEquals(""))
				return trackinfo.getData(Track.key_artwork_url);
			else
				return trackinfo.getData(Track.key_local_artwork_url);
		}
		
		static String getTrackWaveformPath(HashMap<String, String> trackinfo) {
			Log.i("CloudUtils","Checking waveform path " + trackinfo.get(Track.key_download_error) + " " + trackinfo.get(Track.key_local_waveform_url));
			if (!trackinfo.get(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED) || trackinfo.get(Track.key_download_error).contentEquals("true") || trackinfo.get(Track.key_local_waveform_url).contentEquals(""))
				return trackinfo.get(Track.key_waveform_url);
			else
				return trackinfo.get(Track.key_local_waveform_url);
		}
		
		static String getTrackWaveformPath(Track trackinfo) {
			Log.i("CloudUtils","Checking waveform path " + trackinfo.getData(Track.key_download_error) + " " + trackinfo.getData(Track.key_local_waveform_url));
			if (!trackinfo.getData(Track.key_download_status).contentEquals(Track.DOWNLOAD_STATUS_DOWNLOADED) || trackinfo.getData(Track.key_download_error).contentEquals("true") || trackinfo.getData(Track.key_local_waveform_url).contentEquals(""))
				return trackinfo.getData(Track.key_waveform_url);
			else
				return trackinfo.getData(Track.key_local_waveform_url);
		}
		
		static void gotoTrackUploader(Context context, String userPemalink) {
			Intent i = new Intent(context, UserBrowser.class);
			i.putExtra("userLoadPermalink", userPemalink);
			context.startActivity(i);
		}
		
		static void gotoTrackDetails(Context context, Track track) {
			/*Intent i = new Intent(context, TrackBrowser.class);
			i.putExtra("track", track);
			if (track.comments != null)
				i.putExtra("comments", track.comments);
			
			context.startActivity(i);*/
		}
		
		
		static void addTrackToPlaylist(ICloudPlaybackService service, Track track){
			if (service != null)
				try {
					service.enqueueTrack(track, CloudPlaybackService.LAST);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		
		public static void buildCacheDirs(){
			
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				new File(CACHE_DIRECTORY).mkdirs();
			}
			
			File f = new File(CACHE_DIRECTORY);
			
			if (!f.exists())
				 f.mkdirs();
		}

		public static String buildRequestPath(String mUrl, String filter, String order){
			return buildRequestPath(mUrl,filter,order,false);
		}
		
		public static String buildRequestPath(String mUrl, String filter, String order, boolean refresh){
			
			String refreshAppend = "";
			if (refresh)
				refreshAppend = "&rand=" + Math.round((int)10000*Math.random());
			
			if (filter == null && order == null)
				if (refresh)
					return mUrl + "?rand=" + ((int)10000*Math.random());
				else
					return mUrl;
			
			
			
			if (filter == null)
				return String.format(mUrl + "?order=%s", URLEncoder.encode(order) + refreshAppend);
			else if (order == null)
				return String.format(mUrl + "?filter=%s", URLEncoder.encode(filter) + refreshAppend);
			else
				return String.format(mUrl + "?filter=%s&order=%s", URLEncoder.encode(filter),URLEncoder.encode(order) + refreshAppend);
		}
		
		public static String formatUsername(String username){
			return username.replace(" ", "-");
		}
		
		public static String stripProtocol(String url){
			return url.replace("http://www.", "").replace("http://", "");
		}

		


		public static String getCacheFileName(String url) {
//			File f = new File(CACHE_DIRECTORY);
//			if (!f.exists())
//				f.mkdirs();
//			
//			builder.setLength(0);
//			builder.append(CACHE_DIRECTORY);
//			builder.append(url.hashCode()).append(".jpg");
			return url.hashCode() + ".jpg";
		}
		
		public static String getMP3FileName(String title) {
			StringBuilder builder = new StringBuilder();
			builder.setLength(0);
			builder.append(title.hashCode()).append(".mp3");
			return builder.toString();
		}
		
		
		public static String getCacheMP3FileName(String url) {
			StringBuilder builder = new StringBuilder();
			builder.setLength(0);
			builder.append(CACHE_DIRECTORY);
			builder.append(url.hashCode()).append(".mp3");
			return builder.toString();
		}
		

		
		public static String formatGraphicsUrl (String url, String targetSize){
			for (String size : GraphicsSizesLib){
				Log.i("DEBUG","Replace " + size + " with " +targetSize);
				url = url.replace(size, targetSize);
				Log.i("DEBUG","Now its " + url);
			}
			
			Log.i("DEBUG","Returning" + url);
			return url;
		}
		
		
		@SuppressWarnings("unchecked")
		public static boolean isTaskFinished(AsyncTask lt){
			if (lt == null)
				return true;
				
			return lt.getStatus() == AsyncTask.Status.FINISHED;
			
		}
		
		public static boolean isTaskPending(LoadTask lt){
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

		
		
//		public static void configureTabs(TabWidget tabWidget, Drawable tabBackground, Drawable tabBackground2){
//			 //Field mBottomLeftStrip; 
//		     //Field mBottomRightStrip;
//			
//			for (int i =0; i < tabWidget.getChildCount(); i++) {
//	            Log.i("TABS","tabWidget " + tabWidget.getChildAt(i));
//				View vvv = tabWidget.getChildAt(i);
//				
//				//tabWidget.getChildAt(i).getLayoutParams().height = LayoutParams.MATCH_PARENT;
//	            //tabWidget.getChildAt(i).getLayoutParams().width = LayoutParams.WRAP_CONTENT;
//				if (!(tabWidget.getChildAt(i) instanceof OcTab))
//					vvv.setBackgroundDrawable(tabBackground2);
//			}
//			
//			tabWidget.setStripEnabled(false);
//		}

	}


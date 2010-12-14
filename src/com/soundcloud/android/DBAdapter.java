 package com.soundcloud.android;


import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class DBAdapter 
{

    
    private static final String TAG = "SoundcloudDBAdapter";
    
    private static final String[] userkeys = {User.key_id,
    	User.key_permalink,
    	User.key_username,
    	User.key_avatar_url,
    	User.key_city,
    	User.key_country,
    	User.key_discogs_name,
    	User.key_followers_count,
    	User.key_followings_count,
    	User.key_full_name,
    	User.key_myspace_name,
    	User.key_track_count,
    	User.key_website,
    	User.key_website_title,
    	User.key_description};
    
    private static final String[] followingKeys = {"id","user_id","following_id"};
    	
    private static final String[] trackkeys = {Track.key_id,
    	Track.key_permalink,
    	Track.key_created_at,
    	Track.key_duration,
    	Track.key_description,
    	Track.key_bpm,
    	Track.key_genre,
    	Track.key_release,
    	Track.key_release_year,
    	Track.key_release_month,
    	Track.key_release_day,
    	Track.key_isrc,
    	Track.key_purchase_url,
    	Track.key_tag_list,
    	Track.key_label_id,
    	Track.key_label_name,
    	Track.key_video_url,
    	Track.key_track_type,
    	Track.key_key_signature,
    	Track.key_tag_list,
    	Track.key_sharing,
    	Track.key_state,
    	Track.key_title,
    	Track.key_original_format,
    	Track.key_license,
    	Track.key_download_count,
    	Track.key_favoritings_count,
    	Track.key_uri,
		Track.key_permalink_url,
		Track.key_artwork_url,
		Track.key_waveform_url,
		Track.key_downloadable,
		Track.key_download_url,
		Track.key_stream_url,
		Track.key_streamable,
		Track.key_user_id,
		Track.key_local_play_url,
		Track.key_local_artwork_url,
		Track.key_local_waveform_url,
		Track.key_user_played,
		Track.key_download_status,
		Track.key_download_error};
    
    private static final String[] favoriteKeys = {"id","user_id","favorite_id"};
    
    
    private static final String DATABASE_NAME = "Overcast";
    private static final String DATABASE_USER_TABLE = "Users";
    private static final String DATABASE_TRACK_TABLE = "Tracks";
    private static final String DATABASE_FOLLOWING_TABLE = "Followings";
    private static final String DATABASE_FAVORITE_TABLE = "Favorites";
    private static final int DATABASE_VERSION = 2;

    private static final String DATABASE_CREATE_1 =
        "create table Tracks (id string primary key, "
        + "permalink string null, "
        + "created_at string null, " 
        + "duration string null, "
        + "description text null, " 
        + "bpm string null, "
        + "genre string null, " 
        + "release string null, "
        + "release_year string null, " 
        + "release_month string null, "
        + "release_day string null, " 
        + "isrc string null, "
        + "purchase_url string null, " 
        + "tag_list string null, "
        + "label_id string null, " 
        + "label_name string null, "
        + "video_url string null, " 
        + "track_type string null, "
        + "key_signature string null, " 
        + "sharing string null, "
        + "state string null, "
        + "title string null, " 
        + "original_format string null, "
        + "license string null, " 
        + "download_count string null, " 
        + "favoritings_count string null, " 
        + "uri string null, "
        + "permalink_url string null, " 
        + "artwork_url string null, "
        + "waveform_url string null, " 
        + "downloadable string null, "
        + "download_url string null, " 
        + "stream_url string null, "
        + "streamable string null, "
        + "user_id string null, "
        + "local_play_url string null, " 
        + "local_artwork_url string null, " 
        + "local_waveform_url string null, "
        + "user_played boolean false, "
        + "download_status string null, "
        + "download_error boolean false);";

    
    private static final String DATABASE_CREATE_2 =
       "create table Users (id string primary key, "
    	+ "permalink string null, " 
        + "username string null, " 
        + "avatar_url string null, " 
        + "city string null, "
        + "country string null, "
        + "discogs_name string null, " 
        + "followers_count string null, " 
        + "followings_count string null, " 
        + "full_name string null, " 
        + "myspace_name string null, " 
        + "track_count string null, " 
        + "website string null, " 
        + "website_title string null, " 
        + "description text null);";
    
    private static final String DATABASE_CREATE_3 =
        "create table Followings (id INTEGER PRIMARY KEY, "
         + "user_id string null, " 
         + "following_id);";
    
    private static final String DATABASE_CREATE_4 =
        "create table Favorites (id INTEGER PRIMARY KEY, "
         + "user_id string null, " 
         + "favorite_id);";
    
    private final Context context;   
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	
            db.execSQL(DATABASE_CREATE_1);
            db.execSQL(DATABASE_CREATE_2);
            db.execSQL(DATABASE_CREATE_3);
            db.execSQL(DATABASE_CREATE_4);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS Tracks");
            db.execSQL("DROP TABLE IF EXISTS Users");
            db.execSQL("DROP TABLE IF EXISTS Followings");
            db.execSQL("DROP TABLE IF EXISTS Favorites");
            onCreate(db);
        }
    }    
    
    
    public void createFavorites() 
    {
    	
        db.execSQL(DATABASE_CREATE_3);
        db.execSQL(DATABASE_CREATE_4);
    }
    
    //---opens the database---
    public DBAdapter open() throws SQLException 
    {
    	
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---    
    public void close() 
    {
    	db.close();
        DBHelper.close();
    }
    
    public void wipeDB() 
    {
    	db.execSQL("DROP TABLE "+ DATABASE_USER_TABLE);
    	db.execSQL("DROP TABLE "+ DATABASE_TRACK_TABLE);
    	
    	db.execSQL(DATABASE_CREATE_1);
        db.execSQL(DATABASE_CREATE_2);
        
        Log.i("WIPE", "Wiping Database");
    }
    
    
    
    
    
    //---insert a title into the database---
    public void insertTrack(Track track) 
    {

    	ContentValues args = new ContentValues();
    	for (String key : trackkeys){
		 if (track.hasKey(key)){
			 Log.i(TAG,"Inserting track key:val => "+ key + ":" + track.getData(key));
			 args.put(key, track.getData(key));
		 }
			 
    	}
    	
    	long id = db.insert(DATABASE_TRACK_TABLE, null, args);
    }
    
    public void insertTrack(HashMap<String,String> track) 
    {

    	ContentValues args = new ContentValues();
    	for (String key : trackkeys){
		 if (track.containsKey(key)){
			 Log.i(TAG,"Inserting track key:val => "+ key + ":" + track.get(key));
			 args.put(key, track.get(key));
		 }
    	}
    	
    	long id = db.insert(DATABASE_TRACK_TABLE, null, args);
    }
    
    public void insertUser(User user) 
    {

    	ContentValues args = new ContentValues();
    	for (String key : userkeys){
		 if (user.hasKey(key)){
			 Log.i(TAG,"Inserting user key:val => "+ key + ":" + user.getData(key));
			 args.put(key, user.getData(key));
		 }
    	}
    	
    	long id = db.insert(DATABASE_USER_TABLE, null, args);
    }
    
    public void insertFollowing(String user_id, String following_id) 
    {
    	ContentValues args = new ContentValues();
    	args.put("user_id", user_id);
    	args.put("following_id", following_id);
    	
    	//Log.i("DB INSERT FOLLOWING","insert following |" + following_id + "|" + user_id);
    	
    	long id = db.insert(DATABASE_FOLLOWING_TABLE, null, args);
    	
    	//Log.i("DB INSERT FOLLOWING","insert following response|" + id);
    }
    
    public int removeFollowing(String user_id, String following_id) 
    {
    	 return db.delete(DATABASE_FOLLOWING_TABLE, "user_id = '" + user_id + "' and following_id = '" + following_id + "'",null);
    	
    }
    
    public void insertFavorite(String user_id, String favorite_id) 
    {
    	ContentValues args = new ContentValues();
    	args.put("user_id", user_id);
    	args.put("favorite_id", favorite_id);
    	
    	long id = db.insert(DATABASE_FAVORITE_TABLE, null, args);
    }
    
    public int removeFavorite(String user_id, String favorite_id) 
    {
    	 return db.delete(DATABASE_FAVORITE_TABLE, "user_id = '" + user_id + "' and favorite_id = '" + favorite_id + "'",null);
    	
    }
    
    
    public void insertUser(HashMap<String, String> userinfo) 
    {

    	ContentValues args = new ContentValues();
    	for (String key : userkeys){
		 if (userinfo.containsKey(key))
			 args.put(key, userinfo.get(key));
		 	
    	}
    	
    	long id = db.insert(DATABASE_USER_TABLE, null, args);
  
    }

    
    //---deletes a particular title---
//    public boolean deleteTitle(long rowId) 
//    {
//        return db.delete(DATABASE_TABLE, KEY_ROWID + 
//        		"=" + rowId, null) > 0;
//    }
    
  public int updateTrack(Track track) 
  {
    	 ContentValues args = new ContentValues();
    	 for (String key : trackkeys){
    			 if (track.hasKey(key)){
    				 Log.i(TAG,"Updating track key:val => "+ key + ":" + track.getData(key));
    				 args.put(key, track.getData(key));
    			 }
    	 }
    	
    	int result = db.update(DATABASE_TRACK_TABLE, args, Track.key_id + "='" + track.getData(Track.key_id) + "'", null); 
    	Log.i(TAG,"Update Result is " + result);
    	return result; 
    	
  }
  
  public int updateUser(User user) 
  {
    	 ContentValues args = new ContentValues();
    	 for (String key : userkeys){
    		 if (user.hasKey(key)){
    			 args.put(key, user.getData(key));
    			 Log.i("updating","updating value " + key + " " + user.getData(key));
    		 }
    	 }
    	
    	return db.update(DATABASE_USER_TABLE, args, User.key_id + "='" + user.getData(User.key_id) + "'", null);
    	
  }
  
  public int updateUser(HashMap<String,String> userinfo) 
  {
    	 ContentValues args = new ContentValues();
    	 for (String key : userkeys){
    		 if (userinfo.containsKey(key))
    			 args.put(key, userinfo.get(key));
    	 }
    	
    	return db.update(DATABASE_USER_TABLE, args, User.key_id + "='" + userinfo.get(User.key_id) + "'", null);
    	
  }
  
  public int clearFollowings(String user_id) 
  {
    	 return db.delete(DATABASE_FOLLOWING_TABLE, "user_id = '" + user_id + "'",null);
  }
  
  public int clearFavorites(String user_id) 
  {
	  return db.delete(DATABASE_FAVORITE_TABLE, "user_id = '" + user_id + "'",null);
    	
  }
  
  public int markTrackPlayed(String id) 
  {
    	 ContentValues args = new ContentValues();
    	 args.put(Track.key_user_played, true);
    	
    	return db.update(DATABASE_TRACK_TABLE, args, Track.key_id + "='" + id + "'", null);
    	
  }
  
  public int markTrackDownloaded(String id) 
  {
    	 ContentValues args = new ContentValues();
    	 args.put(Track.key_download_status, Track.DOWNLOAD_STATUS_DOWNLOADED);
    	
    	return db.update(DATABASE_TRACK_TABLE, args, Track.key_id + "='" + id + "'", null);
    	
  }
  
  public int markTrackDownloadError(String id, String status) 
  {
    	 ContentValues args = new ContentValues();
    	 args.put(Track.key_download_error, status);
    	
    	return db.update(DATABASE_TRACK_TABLE, args, Track.key_id + "='" + id + "'", null);
    	
  }
  
  public int markTrackCancelDownload(String id) 
  {
    	 ContentValues args = new ContentValues();
    	 args.put(Track.key_download_error, "");
    	 args.put(Track.key_download_status, Track.DOWNLOAD_STATUS_NONE);
    	
    	return db.update(DATABASE_TRACK_TABLE, args, Track.key_id + "='" + id + "'", null);
    	
  }

  
  
  //---retrieves all the titles---
    public Cursor getTrackById(String id, String current_user_id) 
    {
    	//return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM (Tracks INNER JOIN Users ON Tracks.user_permalink = Users.permalink) INNER JOIN Favorites ON Tracks.id = Favorites.favorite_id WHERE Favorites.user_id = '" + user_id + "' ORDER BY Favorites.id asc", null);
    	if (current_user_id != "")
    		return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id FROM Tracks LEFT OUTER JOIN Favorites ON (Tracks.id = Favorites.favorite_id AND Favorites.user_id = '" + current_user_id + "') WHERE Tracks.id = '" + id + "'", null);
    	else
    		return db.query(DATABASE_TRACK_TABLE, trackkeys, 
                Track.key_id + "='" + id + "'", 
                null, 
                null, 
                null, 
                null);
    }
    
    public Cursor getTrackPlayedById(String id, String current_user_id) 
    {
    	//return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM (Tracks INNER JOIN Users ON Tracks.user_permalink = Users.permalink) INNER JOIN Favorites ON Tracks.id = Favorites.favorite_id WHERE Favorites.user_id = '" + user_id + "' ORDER BY Favorites.id asc", null);
    	Log.i("asdf","raw query " + "SELECT Tracks.user_played from Tracks where Tracks.id = '" + id + "'");
    	Log.i("asdf","current user id " + current_user_id);
    	return db.rawQuery("SELECT Tracks.user_played as user_played from Tracks where Tracks.id = '" + id + "'", null);
    }
    
   
    public Cursor getUserByPermalink(String permalink, String current_user_id) 
    {
    	Log.i("asdffdsa","select user by perma " + "SELECT Users.*, Followings.id as user_following_id FROM Users LEFT OUTER JOIN Followings ON (Users.id = Followings.following_id AND  Followings.user_id = '" + current_user_id + "') WHERE Users.permalink = '" + permalink + "'");
    	
    	if (current_user_id != "")
    		return db.rawQuery("SELECT Users.*, Followings.id as user_following_id FROM Users LEFT OUTER JOIN Followings ON (Users.id = Followings.following_id AND  Followings.user_id = '" + current_user_id + "') WHERE Users.permalink = '" + permalink + "'", null);
    	else
    		return db.query(DATABASE_USER_TABLE, userkeys, 
                User.key_permalink + "='" + permalink + "'", 
                null, 
                null, 
                null, 
                null);
    }
    
    public Cursor getUserById(String id, String current_user_id) 
    {
    	if (current_user_id != "")
    		return db.rawQuery("SELECT Users.*, Followings.id as user_following_id FROM Users LEFT OUTER JOIN Followings ON (Users.id = Followings.following_id AND Followings.user_id = '" + current_user_id + "') WHERE Users.id = '" + id + "'", null);
    	else
    		return db.query(DATABASE_USER_TABLE, userkeys, 
                User.key_id + "='" + id + "'", 
                null, 
                null, 
                null, 
                null);
    }
    
//    //---retrieves all the titles---
//    public Cursor getAllDownloadedTracks() 
//    {
//        return db.query(DATABASE_TRACK_TABLE, trackkeys, 
//                Track.key_downloaded + "='true'", 
//                null, 
//                null, 
//                null, 
//                null);
//    }
//    
//    //---retrieves all the titles---
//    public Cursor getAllPendingDownloads() 
//    {
//        return db.query(DATABASE_TRACK_TABLE, trackkeys, 
//        		Track.key_downloaded + "='false'", 
//                null, 
//                null, 
//                null, 
//                null);
//    }
   
    public Cursor getFavoriteTracks(String user_id) 
    {
    	return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM (Tracks INNER JOIN Users ON Tracks.user_permalink = Users.permalink) INNER JOIN Favorites ON Tracks.id = Favorites.favorite_id WHERE Favorites.user_id = '" + user_id + "' ORDER BY Favorites.id asc", null);
    }
    
    public Cursor getFollowings(String user_id)
    {
    	return db.rawQuery("SELECT Users.*, Followings.id as user_following_id FROM Users INNER JOIN Followings ON Users.id = Followings.following_id WHERE Followings.user_id = '" + user_id + "' ORDER BY Followings.id asc", null);
    }
    
    public Cursor getAllDownloadedTracks() 
    {
    	return db.rawQuery("SELECT Tracks.*, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM Tracks INNER JOIN Users ON Tracks.user_id = Users.id WHERE " + Track.key_download_status + " = '"+Track.DOWNLOAD_STATUS_DOWNLOADED+"' ORDER BY Users.username COLLATE NOCASE ASC", null);
    }
    
    public Cursor getAllPendingDownloads() 
    {
    	return db.rawQuery("SELECT Tracks.*, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM Tracks INNER JOIN Users ON Tracks.user_id = Users.id WHERE " + Track.key_download_status + " = '"+Track.DOWNLOAD_STATUS_PENDING+"' ORDER BY Users.username COLLATE NOCASE ASC", null);
    }

//    //---retrieves a particular title---
//    public Cursor getTitle(long rowId) throws SQLException 
//    {
//        Cursor mCursor =
//                db.query(true, DATABASE_TABLE, new String[] {
//                		KEY_ROWID,
//                		KEY_ISBN, 
//                		KEY_TITLE,
//                		KEY_PUBLISHER
//                		}, 
//                		KEY_ROWID + "=" + rowId, 
//                		null,
//                		null, 
//                		null, 
//                		null, 
//                		null);
//        if (mCursor != null) {
//            mCursor.moveToFirst();
//        }
//        return mCursor;
//    }
//
//    //---updates a title---
//    public boolean updateTitle(long rowId, String isbn, 
//    String title, String publisher) 
//    {
//        ContentValues args = new ContentValues();
//        args.put(KEY_ISBN, isbn);
//        args.put(KEY_TITLE, title);
//        args.put(KEY_PUBLISHER, publisher);
//        return db.update(DATABASE_TABLE, args, 
//                         KEY_ROWID + "=" + rowId, null) > 0;
//    }
//    */
}



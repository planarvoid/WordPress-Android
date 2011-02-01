 package com.soundcloud.android;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;

public class DBAdapter 
{

    
    private static final String TAG = "SoundcloudDBAdapter";
    private static final String DATABASE_USER_TABLE = "Users";
    private static final String DATABASE_TRACK_TABLE = "Tracks";
    private static final String DATABASE_FOLLOWING_TABLE = "Followings";
    private static final String DATABASE_FAVORITE_TABLE = "Favorites";
    
    private static final String DATABASE_NAME = "SoundCloud";
    private static final int DATABASE_VERSION = 3;
    
    private static final String DATABASE_CREATE_TRACKS =
        "create table Tracks (id string primary key, "
        + "permalink string null, "
        + "duration string null, "
        + "tag_list string null, "
        + "track_type string null, "
        + "title string null, " 
        + "permalink_url string null, " 
        + "artwork_url string null, "
        + "waveform_url string null, " 
        + "downloadable string null, "
        + "download_url string null, " 
        + "stream_url string null, "
        + "streamable string null, "
        + "user_id string null, "
        + "user_favorite boolean false, "
        + "user_played boolean false, "
        + "filelength integer null);";

    
    private static final String DATABASE_CREATE_USERS =
       "create table Users (id string primary key, "
        + "username string null, " 
        + "avatar_url string null, " 
        + "permalink string null, " 
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
    
    private static final String DATABASE_CREATE_FOLLOWINGS =
        "create table Followings (id INTEGER PRIMARY KEY, "
         + "user_id string null, " 
         + "following_id);";
    
    private static final String DATABASE_CREATE_FOLLOWERS =
        "create table Favorites (id INTEGER PRIMARY KEY, "
         + "user_id string null, " 
         + "favorite_id);";
    
    private final SoundCloudApplication scApp;
    
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(SoundCloudApplication scApp) 
    {
        this.scApp = scApp;
        DBHelper = new DatabaseHelper(scApp);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
    	private Context mContext;
    	
        DatabaseHelper(Context scApp) 
        {
            super(scApp, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	try {
        		db.execSQL(DATABASE_CREATE_TRACKS);
        		db.execSQL(DATABASE_CREATE_USERS);
        	} catch (SQLiteException e){
        		e.printStackTrace();
        	}
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
        	   //Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        	
        	  if (newVersion > oldVersion) {
        	    db.beginTransaction();
        	 
        	    boolean success = true;
        	    for (int i = oldVersion ; i < newVersion ; ++i) {
        	      int nextVersion = i + 1;
        	      switch (nextVersion) {
        	       
        	        case 3:
        	        	upgradeTo3(db);
        	          break;
        	        // etc. for later versions.
        	      }
        	 
        	      if (!success) {
        	        break;
        	      }
        	    }
        	 
        	    if (success) {
        	      db.setTransactionSuccessful();
        	    }
        	    db.endTransaction();
        	  }
        	  else {
        		  db.execSQL("DROP TABLE IF EXISTS Tracks");
                  db.execSQL("DROP TABLE IF EXISTS Users");
                  db.execSQL("DROP TABLE IF EXISTS Followings");
                  db.execSQL("DROP TABLE IF EXISTS Favorites");
                  onCreate(db);
        	  }
        	
         
           
        }
        
        private void upgradeTo3(SQLiteDatabase db){
        	// reformat table to remove unnecessary cols
        	db.execSQL("DROP TABLE IF EXISTS TmpTracks");
        	db.execSQL(DATABASE_CREATE_TRACKS.replace("create table Tracks", "create table TmpTracks"));
        	List<String> columns = GetColumns(db, "TmpTracks");
        	columns.retainAll(GetColumns(db, "Tracks"));
        	String cols = join(columns, ","); 
        	db.execSQL(String.format( "INSERT INTO Tmp%s (%s) SELECT %s from %s", DATABASE_TRACK_TABLE, cols, cols, DATABASE_TRACK_TABLE));
        	db.execSQL("DROP table  '" + DATABASE_TRACK_TABLE+"'");
        	db.execSQL(DATABASE_CREATE_TRACKS);
        	db.execSQL(String.format( "INSERT INTO %s (%s) SELECT %s from Tmp%s", DATABASE_TRACK_TABLE, cols, cols, DATABASE_TRACK_TABLE));
        	db.execSQL("DROP table Tmp" + DATABASE_TRACK_TABLE);
        	
        	//make sure booleans are formatted properly, as some were strings before
        	db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = '' ");
        	db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = '0' ");
        	db.execSQL("UPDATE Tracks  set user_favorite = 0 where user_favorite = 'false' ");
        	db.execSQL("UPDATE Tracks  set user_favorite = 1 where user_favorite = 'true' ");
        	db.execSQL("UPDATE Tracks  set user_favorite = 1 where user_favorite = '1' ");
        	db.execSQL("UPDATE Tracks  set user_played = 0 where user_favorite = '' ");
        	db.execSQL("UPDATE Tracks  set user_played = 0 where user_favorite = '0' ");
        	db.execSQL("UPDATE Tracks  set user_played = 0 where user_favorite = 'false' ");
        	db.execSQL("UPDATE Tracks  set user_played = 1 where user_favorite = 'true' ");
        	db.execSQL("UPDATE Tracks  set user_played = 1 where user_favorite = '1' ");
        }
    }    
    
    //---opens the database---
    public DBAdapter open() throws SQLException 
    {
    	
        db = DBHelper.getWritableDatabase();
    	/*for (String st : GetColumns(db,DATABASE_TRACK_TABLE)){
    		Log.i(TAG,"Col " + st);
    	}*/
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
        db.execSQL(DATABASE_CREATE_USERS);
    	db.execSQL(DATABASE_CREATE_TRACKS);

        
        //Log.i("WIPE", "Wiping Database");
    }
    
    private String[] getDBCols(String tablename){
    	if (this.scApp.getDBColumns().get(tablename) == null)
    		this.scApp.getDBColumns().put(tablename,GetColumnsArray(db, tablename));
    	return this.scApp.getDBColumns().get(tablename);
    }
    
    private ContentValues buildTrackArgs(Track track){
    	ContentValues args = new ContentValues();
    	Method m;
    	for (String key : getDBCols(DATABASE_TRACK_TABLE)){
    		try {
    			// I was going to search through annotaitons but it was too expensive, so this is way cheaper
				m =Track.class.getMethod("get"+CloudUtils.toCamelCase(key));
				if (m != null){
	    			try {
						if (m.getReturnType() == String.class)
							args.put(key, (String) m.invoke(track));
						else  if (m.getReturnType() == Integer.class)
							args.put(key, (Integer) m.invoke(track));
						else  if (m.getReturnType() == Long.class)
							args.put(key, (Long) m.invoke(track));
						else if (m.getReturnType() == Boolean.class)
							args.put(key, ((Boolean) m.invoke(track)) ? 1 : 0);
						
					} catch (Exception e) {e.printStackTrace();} 
	    		}
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				//e1.printStackTrace();
			}
    		
    	}
    	return args;
    }
    
    private ContentValues buildUserArgs(User  user, Boolean isCurrentUser){
    	ContentValues args = new ContentValues();
    	Method m;
    	for (String key : getDBCols(DATABASE_USER_TABLE)){
    		if (!isCurrentUser && key.equalsIgnoreCase("description"))
    			continue;
    		
    		try {
    			// I was going to search through annotaitons but it was too expensive, so this is way cheaper
				m =User.class.getMethod("get"+CloudUtils.toCamelCase(key));
				
				if (m != null){
	    			 
	    			try {
						if (m.getReturnType() == String.class)
							args.put(key, (String) m.invoke(user));
						else  if (m.getReturnType() == Integer.class)
							args.put(key, (Integer) m.invoke(user));
						else  if (m.getReturnType() == Long.class)
							args.put(key, (Long) m.invoke(user));
						else if (m.getReturnType() == Boolean.class)
							args.put(key, ((Boolean) m.invoke(user)) ? 1 : 0);
					} catch (Exception e) {e.printStackTrace();} 
	    		}
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				//e1.printStackTrace();
			}
    	}
    	return args;
    }
    
    
    //---insert a title into the database---
    public void insertTrack(Track track) 
    {
    	long id = db.insert(DATABASE_TRACK_TABLE, null, buildTrackArgs(track));
    }
    
    public void insertTrack(HashMap<String,String> track) 
    {

    	ContentValues args = new ContentValues();
    	for (String key : getDBCols(DATABASE_TRACK_TABLE)){
		 if (track.containsKey(key)){
			 //Log.i(TAG,"Inserting track key:val => "+ key + ":" + track.get(key));
			 args.put(key, track.get(key));
		 }
    	}
    	
    	long id = db.insert(DATABASE_TRACK_TABLE, null, args);
    }
    
    public void insertUser(User user, Boolean isCurrentUser) 
    {
    	long id = db.insert(DATABASE_USER_TABLE, null, buildUserArgs(user, isCurrentUser));
    }
    
    public void insertFollowing(String user_id, String following_id) 
    {
    	ContentValues args = new ContentValues();
    	args.put("user_id", user_id);
    	args.put("following_id", following_id);
    	
    	////Log.i("DB INSERT FOLLOWING","insert following |" + following_id + "|" + user_id);
    	
    	long id = db.insert(DATABASE_FOLLOWING_TABLE, null, args);
    	
    	////Log.i("DB INSERT FOLLOWING","insert following response|" + id);
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
    
    public int trimTracks(long[] currentPlaylist){
    	String[] whereArgs = new String[2];
    	whereArgs[0] = whereArgs[1] = Boolean.toString(false);
    	return db.delete(DATABASE_TRACK_TABLE, "(user_favorite = 0 AND user_played = 0) AND id NOT IN (" + joinArray(currentPlaylist,",") + ")", null);
    }
    
    public int test(){
    	SQLiteStatement dbJournalCountQuery;
        dbJournalCountQuery = db.compileStatement("select count(*) from " + DATABASE_TRACK_TABLE);
        return (int) dbJournalCountQuery.simpleQueryForLong();
    }
    
   /* 
    public void insertTracks(Track[] tracks){
   	 // Create a single InsertHelper to handle this set of insertions.
   	    InsertHelper ih = new InsertHelper(db, "columnTable");
   	
   	    // Get the numeric indexes for each of the columns that we're updating
   	    final int idCol = ih.getColumnIndex("id");
   	    final int permalinkCol = ih.getColumnIndex("permalink");
   	    final int durationCol = ih.getColumnIndex("duration");
   		final int tagCol = ih.getColumnIndex("tag_list");
   	    final int trackTypeCol = ih.getColumnIndex("track_type");
   	    final int title = ih.getColumnIndex("title");
   	    final int permalinkUrlCol = ih.getColumnIndex("permalink_url");
   	    final int artworkUrlCol = ih.getColumnIndex("artwork_url");
   	    final int waveformUrlCol = ih.getColumnIndex("waveform_url");
   	    final int downloadable = ih.getColumnIndex("downloadable");
   	    final int downloadUrlCol = ih.getColumnIndex("download_url");
   	    final int streamUrlCol = ih.getColumnIndex("stream_url");
   	    final int streamableCol = ih.getColumnIndex("streamable");
   	    final int userIdCol = ih.getColumnIndex("userId");
   	    final int userPlayedCol = ih.getColumnIndex("user_played");
   	    final int filelengthCol = ih.getColumnIndex("filelength");
   	
   	    Boolean moreRowsToInsert = true;
   	    for (int i = 0; i < users.length; i++){
   	        // ... Create the data for this row (not shown) ...
   	
   	        // Get the InsertHelper ready to insert a single row
   	        ih.prepareForInsert();
   	
   	        // Add the data for each column
   	        ih.bind(idCol, users[i].getId());
   	
   	        // Insert the row into the database.
   	        ih.execute();
   	    }
       }
    
    public void insertUsers(User[] users){
	 // Create a single InsertHelper to handle this set of insertions.
	    InsertHelper ih = new InsertHelper(db, "columnTable");
	
	    // Get the numeric indexes for each of the columns that we're updating
	    final int idCol = ih.getColumnIndex("id");
	    final int permalinkCol = ih.getColumnIndex("permalink");
	    final int usernameCol = ih.getColumnIndex("username");
		final int avatarCol = ih.getColumnIndex("avatar_url");
	    final int cityCol = ih.getColumnIndex("city");
	    final int countryCol = ih.getColumnIndex("country");
	    final int discogsNameCol = ih.getColumnIndex("country");
	    final int followersCountCol = ih.getColumnIndex("country");
	    final int followingsCountCol = ih.getColumnIndex("country");
	    final int fullNameCol = ih.getColumnIndex("country");
	    final int myspaceNameCol = ih.getColumnIndex("country");
	    final int trackCountCol = ih.getColumnIndex("country");
	    final int websiteCol = ih.getColumnIndex("country");
	    final int websiteTitleCol = ih.getColumnIndex("country");
	    final int descriptionCol = ih.getColumnIndex("country");
	
	    Boolean moreRowsToInsert = true;
	    for (int i = 0; i < users.length; i++){
	        // ... Create the data for this row (not shown) ...
	
	        // Get the InsertHelper ready to insert a single row
	        ih.prepareForInsert();
	
	        // Add the data for each column
	        ih.bind(idCol, users[i].getId());
	
	        // Insert the row into the database.
	        ih.execute();
	    }
    }
    */
    
    //---deletes a particular title---
//    public boolean deleteTitle(long rowId) 
//    {
//        return db.delete(DATABASE_TABLE, KEY_ROWID + 
//        		"=" + rowId, null) > 0;
//    }
    


public int updateTrack(Track track) 
  {
    	return db.update(DATABASE_TRACK_TABLE, buildTrackArgs(track), Track.key_id + "='" + track.getId() + "'", null); 
  }
  
  public int updateUser(User user, Boolean isCurrentUser) 
  {
    	return db.update(DATABASE_USER_TABLE, buildUserArgs(user,isCurrentUser), User.key_id + "='" + user.getId() + "'", null);
    	
  }
  
  public int updateUser(HashMap<String,String> userinfo) 
  {
    	 ContentValues args = new ContentValues();
    	 for (String key : getDBCols(DATABASE_USER_TABLE)){
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
    public Cursor getTrackById(long l, long currentUserId) 
    {
    	//return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM (Tracks INNER JOIN Users ON Tracks.user_permalink = Users.permalink) INNER JOIN Favorites ON Tracks.id = Favorites.favorite_id WHERE Favorites.user_id = '" + user_id + "' ORDER BY Favorites.id asc", null);
    	if (currentUserId != 0)
    		return db.rawQuery("SELECT Tracks.* FROM Tracks WHERE Tracks.id = '" + l + "'", null);
    	else
    		return db.query(DATABASE_TRACK_TABLE, GetColumnsArray(db, DATABASE_TRACK_TABLE), 
                Track.key_id + "='" + l + "'", 
                null, 
                null, 
                null, 
                null);
    }
    
    public Cursor getTrackPlayedById(String id, String current_user_id) 
    {
    	//return db.rawQuery("SELECT Tracks.*, Favorites.id as user_favorite_id, Users.id as user_id, Users.permalink as user_permalink, Users.username, Users.avatar_url, Users.city, Users.country FROM (Tracks INNER JOIN Users ON Tracks.user_permalink = Users.permalink) INNER JOIN Favorites ON Tracks.id = Favorites.favorite_id WHERE Favorites.user_id = '" + user_id + "' ORDER BY Favorites.id asc", null);
    	//Log.i("asdf","raw query " + "SELECT Tracks.user_played from Tracks where Tracks.id = '" + id + "'");
    	//Log.i("asdf","current user id " + current_user_id);
    	return db.rawQuery("SELECT Tracks.user_played as user_played from Tracks where Tracks.id = '" + id + "'", null);
    }
    
    public Cursor getUserById(Long userId, Long currentUserId) 
    {
    	if (currentUserId != 0)
    		return db.rawQuery("SELECT Users.* FROM Users WHERE Users.id = '" + userId + "'", null);
    	else
    		return db.query(DATABASE_USER_TABLE, GetColumnsArray(db, DATABASE_USER_TABLE), 
                User.key_id + "='" + userId + "'", 
                null, 
                null, 
                null, 
                null);
    }
    
//    //---retrieves all the titles---
//    public Cursor getAllDownloadedTracks() 
//    {
//        return db.query(DATABASE_TRACK_TABLE, GetColumns(db, DATABASE_TRACK_TABLE), 
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
//        return db.query(DATABASE_TRACK_TABLE, GetColumns(db, DATABASE_TRACK_TABLE), 
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
    
    
    
    public static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }
    
    public static String[] GetColumnsArray(SQLiteDatabase db, String tableName) {
        String[] ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = c.getColumnNames();
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    public static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list.get(i));
        }
        return buf.toString();
    }
    
    public static String joinArray(String[] list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.length;
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list[i]);
        }
        return buf.toString();
    }
    
    private String joinArray(long[] list, String delim) {
    	 StringBuilder buf = new StringBuilder();
         int num = list.length;
         for (int i = 0; i < num; i++) {
             if (i != 0)
                 buf.append(delim);
             buf.append((String) Long.toString(list[i]));
         }
         return buf.toString();
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



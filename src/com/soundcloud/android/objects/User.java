package com.soundcloud.android.objects;

import java.lang.reflect.Field;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;

@JsonIgnoreProperties(ignoreUnknown=true)
public class User extends BaseObj implements Parcelable  {

	public static final String key_id = "id";
	public static final String key_username = "username";
	public static final String key_track_count = "track_count";
	public static final String key_discogs_name = "discogs_name";
	public static final String key_city = "city";
	public static final String key_uri = "uri";
	public static final String key_avatar_url = "avatar_url";
	public static final String key_local_avatar_url = "local_avatar_url";
	public static final String key_website_title = "website_title";
	public static final String key_website = "website";
	public static final String key_description = "description";
	public static final String key_online = "online";
	public static final String key_permalink = "permalink";
	public static final String key_permalink_url = "permalink_url";
	public static final String key_full_name = "full_name";
	public static final String key_followers_count = "followers_count";
	public static final String key_followings_count = "followings_count";
	public static final String key_public_favorites_count = "public_favorites_count";
	public static final String key_myspace_name = "myspace_name";
	public static final String key_country = "country";
	public static final String key_location = "location";
	public static final String key_user_following = "user_following";
	public static final String key_user_following_id = "user_following_id";
	
	private Long id;
	private String username;
	private String track_count;
	private String discogs_name;
	private String city;
	private String uri;
	private String avatar_url;
	private String local_avatar_url;
	private String website_title;
	private String website;
	private String description;
	private String online;
	private String permalink;
	private String permalink_url;
	private String full_name;
	private String followers_count;
	private String followings_count;
	private String public_favorites_count;
	private String private_tracks_count;
	private String myspace_name;
	private String country;
	private String location;
	private String plan;
	private String user_following;
	private String user_following_id;
	
	
	@JsonProperty("id")
	public Long getId() {
		return id;
	}
	@JsonProperty("id")
	public void setId(Long id) {
		Log.i("User","Setting user id " + id);
		this.id = id;
	}
	@JsonProperty("username")
	public String getUsername() {
		return username;
	}
	@JsonProperty("username")
	public void setUsername(String username) {
		this.username = username;
	}
	@JsonProperty("track_count")
	public String getTrackCount() {
		return track_count;
	}
	@JsonProperty("track_count")
	public void setTrackCount(String track_count) {
		this.track_count = track_count;
	}
	@JsonProperty("discogs_name")
	public String getDiscogsName() {
		return discogs_name;
	}
	@JsonProperty("discogs_name")
	public void setDiscogsName(String discogs_name) {
		this.discogs_name = discogs_name;
	}
	@JsonProperty("uri")
	public String getUri() {
		return uri;
	}
	@JsonProperty("uri")
	public void setUri(String uri) {
		this.uri = uri;
	}
	@JsonProperty("avatar_url")
	public String getAvatarUrl() {
		return avatar_url;
	}
	@JsonProperty("avatar_url")
	public void setAvatarUrl(String avatar_url) {
		this.avatar_url = avatar_url;
	}
	@JsonProperty("local_avatar_url")
	public String getLocalAvatarUrl() {
		return local_avatar_url;
	}
	@JsonProperty("local_avatar_url")
	public void setLocalAvatarUrl(String local_avatar_url) {
		this.local_avatar_url = local_avatar_url;
	}
	@JsonProperty("website_title")
	public String getWebsiteTitle() {
		return website_title;
	}
	@JsonProperty("website_title")
	public void setWebsiteTitle(String website_title) {
		this.website_title = website_title;
	}
	@JsonProperty("website")
	public String getWebsite() {
		return website;
	}
	@JsonProperty("website")
	public void setWebsite(String website) {
		this.website = website;
	}
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}
	@JsonProperty("description")
	public void setDescription(String description) {
		this.description = description;
	}
	@JsonProperty("online")
	public String getOnline() {
		return online;
	}
	@JsonProperty("online")
	public void setOnline(String online) {
		this.online = online;
	}
	@JsonProperty("permalink")
	public String getPermalink() {
		return permalink;
	}
	@JsonProperty("permalink")
	public void setPermalink(String permalink) {
		this.permalink = permalink;
	}
	@JsonProperty("permalink_url")
	public String getPermalinkUrl() {
		return permalink_url;
	}
	@JsonProperty("permalink_url")
	public void setPermalinkUrl(String permalink_url) {
		this.permalink_url = permalink_url;
	}
	@JsonProperty("full_name")
	public String getFullName() {
		return full_name;
	}
	@JsonProperty("full_name")
	public void setFullName(String full_name) {
		this.full_name = full_name;
	}
	@JsonProperty("followers_count")
	public String getFollowersCount() {
		return followers_count;
	}
	@JsonProperty("followers_count")
	public void setFollowersCount(String followers_count) {
		this.followers_count = followers_count;
	}
	@JsonProperty("followings_count")
	public String getFollowingsCount() {
		return followings_count;
	}
	@JsonProperty("followings_count")
	public void setFollowingsCount(String followings_count) {
		this.followings_count = followings_count;
	}
	@JsonProperty("public_favorites_count")
	public String getPublicFavoritesCount() {
		return public_favorites_count;
	}
	@JsonProperty("public_favorites_count")
	public void setPublicFavoritesCount(String public_favorites_count) {
		this.public_favorites_count = public_favorites_count;
	}
	@JsonProperty("private_tracks_count")
	public String getPrivateTracksCount() {
		return private_tracks_count;
	}
	@JsonProperty("private_tracks_count")
	public void setPrivateTracksCount(String private_tracks_count) {
		this.private_tracks_count = private_tracks_count;
	}

	@JsonProperty("myspace_name")
	public String getMyspaceName() {
		return myspace_name;
	}
	@JsonProperty("myspace_name")
	public void setMyspaceName(String myspace_name) {
		this.myspace_name = myspace_name;
	}
	@JsonProperty("country")
	public String getCountry() {
		return country;
	}
	@JsonProperty("country")
	public void setCountry(String country) {
		this.country = country;
		resolveLocation();
	}
	@JsonProperty("city")
	public String getCity() {
		return city;
	}
	@JsonProperty("city")
	public void setCity(String city) {
		this.city = city;
		resolveLocation();
	}
	@JsonProperty("location")
	public String getLocation() {
		return location;
	}
	@JsonProperty("location")
	public void setLocation(String location) {
		this.location = location;
	}
	@JsonProperty("plan")
	public String getPlan() {
		return plan;
	}
	@JsonProperty("plan")
	public void setPlan(String plan) {
		this.plan = plan;
	}
	@JsonProperty("user_following")
	public String getUserFollowing() {
		return user_following;
	}
	@JsonProperty("user_following")
	public void setUserFollowing(String user_following) {
		this.user_following = user_following;
	}
	@JsonProperty("user_following_id")
	public String getUserFollowingId() {
		return user_following_id;
	}
	@JsonProperty("user_following_id")
	public void setUserFollowingId(String user_following_id) {
		this.user_following_id = user_following_id;
	}

	public void resolveLocation() {
		setLocation(CloudUtils.getLocationString(getCity() == null ? "" : getCity(), getCountry() == null ? "" : getCountry()));
	}
	
	public User(){
	}
	
	public User(Parcel in){
		readFromParcel(in);
	}
	
	public User(Cursor cursor){
		if (cursor.getCount() != 0){
			cursor.moveToFirst();
			
			String[] keys = cursor.getColumnNames();
			for (String key : keys) {
				try {
					Field f = this.getPrivateField(key);
					if (f != null){
						if (f.getType() == String.class){
								f.set(this, cursor.getString(cursor.getColumnIndex(key)));
						}else if (f.getType() == Integer.class){
							f.set(this, cursor.getInt(cursor.getColumnIndex(key)));	
						}else if (f.getType() == Long.class){
							f.set(this, cursor.getLong(cursor.getColumnIndex(key)));	
						}else if (f.getType() == Boolean.class){
							f.set(this, cursor.getInt(cursor.getColumnIndex(key)));	
						}
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}	
			}
		}
	}
	
	public void update(Cursor cursor){
		if (cursor.getCount() != 0){
			cursor.moveToFirst();
			
			String[] keys = cursor.getColumnNames();
			for (String key : keys) {
				try {
					Field f = this.getPrivateField(key);
					if (f != null){
						if (f.getType() == String.class){
							if (f.get(this) == null) f.set(this, cursor.getString(cursor.getColumnIndex(key)));
						}else if (f.getType() == Integer.class){
							if (f.get(this) == null) f.set(this, cursor.getInt(cursor.getColumnIndex(key)));	
						}else if (f.getType() == Long.class){
							f.set(this, cursor.getLong(cursor.getColumnIndex(key)));	
						}else if (f.getType() == Boolean.class){
							if (f.get(this) == null) f.set(this, cursor.getInt(cursor.getColumnIndex(key)));	
						}
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}	
			}
		}
	}
	
	public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
          return new User[size];
        }
    };

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

}

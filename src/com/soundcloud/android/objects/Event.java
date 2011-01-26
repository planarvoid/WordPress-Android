package com.soundcloud.android.objects;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Event extends BaseObj implements Parcelable  {

	private static String TAG = "Event";
	
	public static final String MODEL = "overcast.event";
	
	public static final String key_id = "id";
	public static final String key_type = "type";
	public static final String key_origin = "origin";
	public static final String key_tags = "tags";
	public static final String key_label = "label";
	public static final String key_track = "track";
	public static final String key_next_href = "next_href";
	public static final String key_user = "user";
	public static final String key_user_id = "user_id";
	public static final String key_username = "username";
	public static final String key_user_permalink = "user_permalink";
	public static final String key_user_avatar_url = "user_avatar_url";
	
	private Bundle data;
	
	
	
	private int id;
	private String type;
	private String tags;
	private String label;
	private Origin origin;
	
	public static class Origin extends Track{
		private Track sharedTrack;
		
		@JsonProperty("track")
		public Track getSharedTrack() {
			return sharedTrack;
		}

		@JsonProperty("track")
		public void setSharedTrack(@JsonProperty("track") Track sharedTrack) {
			Log.i(TAG,"SEtting Shared Track ");
			this.sharedTrack = sharedTrack;
		}
	}

	@JsonProperty("id")
	public int getId() {
		return id;
	}
	@JsonProperty("id")
	public void setId(int id) {
		this.id = id;
	}
	@JsonProperty("type")
	public String getType() {
		return type;
	}
	@JsonProperty("type")
	public void setType(String type) {
		this.type = type;
	}
	@JsonProperty("tags")
	public String getTags() {
		return tags;
	}
	@JsonProperty("tags")
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	@JsonProperty("origin")
	public void setOrigin(Origin origin) {
		this.origin = origin;
	}
	@JsonProperty("origin")
	public Origin getOrigin() {
		return origin;
	}
	@JsonProperty("label")
	public void setLabel(String label) {
		this.label = label;
	}
	@JsonProperty("label")
	public String getLabel() {
		return label;
	}


	public enum Parcelables { track, user, comment }
	public enum ActivityTypes { track, track_sharing, comment }
	
	public Event(){
		data = new Bundle();
	}
	
	public Event(Parcel in){
		readFromParcel(in);
	}
	
	public Track getTrack(){
		if (getType().equalsIgnoreCase("track"))
			return origin;
		else if (getType().equalsIgnoreCase("track-sharing"))
			return origin.getSharedTrack();
		
		return null;
	}
	

	
	public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
          return new Event[size];
        }
    };


	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}

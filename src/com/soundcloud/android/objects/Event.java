package com.soundcloud.android.objects;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class Event extends BaseObj implements Parcelable  {

	public static final String MODEL = "overcast.event";
	
	public static final String key_id = "id";
	public static final String key_type = "type";
	public static final String key_origin = "origin";
	public static final String key_tags = "tags";
	
	public static final String key_track = "track";
	
	public static final String key_next_href = "next_href";
	
	
	public static final String key_user = "user";
	public static final String key_user_id = "user_id";
	public static final String key_username = "username";
	public static final String key_user_permalink = "user_permalink";
	public static final String key_user_avatar_url = "user_avatar_url";
	
	private Bundle data;
	
	public enum Parcelables { track, user, comment }
	public enum ActivityTypes { track, track_sharing, comment }
	
	public Event(){
		data = new Bundle();
	}
	
	public Event(JSONObject eventObject){

		data = new Bundle();
		//Log.i("EVENT","Event obj " + eventObject.toString());
		
		try {
			
			data.putString(key_type, eventObject.get(key_type).toString());
			data.putString(key_tags, eventObject.get(key_type).toString());
			
			switch (ActivityTypes.valueOf(eventObject.get(key_type).toString().replace("-","_"))){
				case track:
					data.putParcelable(key_track, new Track(eventObject.getJSONObject(key_origin)));
					break;
				case track_sharing:
					data.putParcelable(key_track, new Track(eventObject.getJSONObject(key_origin).getJSONObject(key_track)));
					break;
				default:
					break;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		resolveData();
	}
	
	
	
	public void resolveData() {
	
	}
	
	
	
	public Event(Parcel in){
		readFromParcel(in);
	}
	
	public Boolean hasKey(String key){
		return data.containsKey(key);
	}
	
	@Override
	public String getData(String key){
		if (data.get(key) != null)
			return data.getString(key);
		else
			return "";
	}
	
	@Override
	public HashMap<String,String> mapData(){
		HashMap<String,String> dataMap = new HashMap<String,String>();
		
		for (String key : data.keySet()){
			if (data.get(key) instanceof String)
				dataMap.put(key,data.getString(key));
		}
		
		return dataMap;
	}
	
	
	
	@Override
	public void putData(String key,String value){
		data.putString(key,value);
	}
	
	@Override
	public Parcelable getDataParcelable(String key){
		if (data.get(key) != null)
			return data.getParcelable(key);
		else
			return null;
	}
	
	@Override
	public void putDataParcelable(String key, Parcelable value){
		data.putParcelable(key, value);
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
	public void readFromParcel(Parcel in) {
    	data = in.readBundle();
    }

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		// TODO Auto-generated method stub
		out.writeBundle(data);
	}

	
}

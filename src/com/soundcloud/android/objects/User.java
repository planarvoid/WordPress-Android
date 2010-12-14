package com.soundcloud.android.objects;

import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.soundcloud.android.CloudUtils;


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
	
	private Bundle data;
	
	public User(){
		data = new Bundle();
	}
	
	public User(JSONObject userObject){
		
		data = new Bundle();
		
		Iterator keys = userObject.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			
			if (!key.contentEquals("tracks"))
			try{
				switch (Parcelables.valueOf(key.toLowerCase())){
				case track:
					data.putParcelable(key, new Track(userObject.getJSONObject(key)));
					break;
				case user:
					data.putParcelable(key, new User(userObject.getJSONObject(key)));
					break;
				default:
					
					break;
			}
			
			} catch (IllegalArgumentException e ){
				try {
					if (userObject.has(key) && userObject.getString(key) != "null")
						data.putString(key, userObject.getString(key));
					else
						data.putString(key, "");
				} catch (JSONException e1) {
					data.putString(key, "");
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		resolveData();
	}
	
	public User(Node datanode){
		data = new Bundle();
		for(int i=0; i < datanode.getChildNodes().getLength() ; i++){
			Node dataItemNode = datanode.getChildNodes().item(i);
			if(dataItemNode.getNodeType() == Node.ELEMENT_NODE){
				
				try{
					switch (Parcelables.valueOf(dataItemNode.getNodeName().toLowerCase())){
						case track:
							data.putParcelable(dataItemNode.getNodeName().toString(), new Track(dataItemNode));
							break;
						case user:
							data.putParcelable(dataItemNode.getNodeName().toString(), new User(dataItemNode));
							break;
						case comment:
							data.putParcelable(dataItemNode.getNodeName().toString(), new Comment(dataItemNode));
							break;
						default:
							break;
						
					}
				} catch (IllegalArgumentException e ){
					if (dataItemNode.getChildNodes().getLength() > 0){						
						data.putString(dataItemNode.getNodeName().toString().replace("-","_"), dataItemNode.getFirstChild().getNodeValue());
					} else {
						data.putString(dataItemNode.getNodeName().toString().replace("-","_"), "");
					}
				}
			}
		}
		
		resolveData();
	}
	
	public User(HashMap<String,String> userData){
		data = new Bundle();
		
		for (String key : userData.keySet())
		{
			data.putString(key, userData.get(key));
		}
		
		resolveData();
	}
	
	
	public void resolveData() {
		data.putString(User.key_location, CloudUtils.getLocationString(data.getString(User.key_city), data.getString(User.key_country)));
	}
	
	
	public User(Parcel in){
		readFromParcel(in);
	}
	
	@Override
	public String getData(String key){
		if (data.get(key) != null)
			return data.getString(key);
		else
			return "";
	}
	
	@Override
	public void putData(String key,String value){
		data.putString(key,value);
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

	public Boolean hasKey(String key){
		return data.containsKey(key);
	}

	
}

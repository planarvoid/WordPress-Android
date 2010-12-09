package com.soundcloud.android.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


public class Comment extends BaseObj implements Parcelable  {

	public static final String MODEL = "overcast.comment";
	
	public static final String key_id = "id";
	public static final String key_uri = "uri";
	public static final String key_body = "body";
	public static final String key_timestamp = "timestamp";
	public static final String key_timestamp_formatted = "timestamp_formatted";
	public static final String key_track_id = "track_id";
	public static final String key_created_at = "created_at";
	public static final String key_reply_to = "reply_to";
	
	public static final String key_user = "user";
	public static final String key_user_id = "user_id";
	public static final String key_username = "username";
	public static final String key_user_permalink = "user_permalink";
	public static final String key_user_avatar_url = "user_avatar_url";
	
	private Bundle data;
	
	public enum Parcelables { track, user, comment }
	
	public Comment(){
		data = new Bundle();
	}
	
	public Comment(JSONObject commentObject){

		data = new Bundle();
		
		Iterator keys = commentObject.keys();
		while(keys.hasNext()) {
			
			String key = (String)keys.next();
			if (!key.contentEquals("tracks"))
				try{
					switch (Parcelables.valueOf(key.toLowerCase())){
						case track:
							data.putParcelable(key, new Track(commentObject.getJSONObject(key)));
							break;
						case user:
							//User usr = new User(commentObject.getJSONObject(key_user));
							//data.putParcelable(key, usr);
							data.putString(key_username, commentObject.getJSONObject(key_user).getString(User.key_username));
							data.putString(key_user_permalink, commentObject.getJSONObject(key_user).getString(User.key_permalink));
							data.putString(key_user_avatar_url, commentObject.getJSONObject(key_user).getString(User.key_avatar_url));
							break;
						default:
							break;
				}
			
			} catch (IllegalArgumentException e ){
				
				try {
					if (commentObject.has(key) && commentObject.getString(key) != "null")
						data.putString(key, commentObject.getString(key));
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
	
	public Comment(Node datanode){
		
		data = new Bundle();
		for(int i=0; i < datanode.getChildNodes().getLength() ; i++){
			Node dataItemNode = datanode.getChildNodes().item(i);
			if(dataItemNode.getNodeType() == Node.ELEMENT_NODE){
				
				try{
					switch (Parcelables.valueOf(dataItemNode.getNodeName().toLowerCase())){
						case track:
							//data.putParcelable(key, new Track(commentObject.getJSONObject(key)));
							//data.putParcelable(dataItemNode.getNodeName().toString(), new Track(dataItemNode));
							break;
						case user:
							data.putParcelable(dataItemNode.getNodeName().toString(), new User(dataItemNode));
							for(int j=0; i < dataItemNode.getChildNodes().getLength() ; j++){
								Node dataItemChildNode = dataItemNode.getChildNodes().item(j);
								if(dataItemChildNode.getNodeType() == Node.ELEMENT_NODE){
									if (dataItemChildNode.getNodeName().toLowerCase().contentEquals(User.key_username)){
										data.putString(key_username, dataItemChildNode.getFirstChild().getNodeValue());
									} else if (dataItemChildNode.getNodeName().toLowerCase().contentEquals(User.key_permalink)){
										data.putString(key_user_permalink, dataItemChildNode.getFirstChild().getNodeValue());
									} else if (dataItemChildNode.getNodeName().toLowerCase().contentEquals(User.key_avatar_url)){
										data.putString(key_user_avatar_url, dataItemChildNode.getFirstChild().getNodeValue());
									}
								}
							}
							break;
						case comment:
							data.putParcelable(dataItemNode.getNodeName().toString(), new Comment(dataItemNode));
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
	
	public Comment(HashMap<String,String> commentData){
		data = new Bundle();
		for (String key : commentData.keySet())
		{
			data.putString(key, commentData.get(key));
		}
		resolveData();
	}
	
	
	public void resolveData() {
		if ( data.getString(Comment.key_timestamp) == "null" ||  data.getString(Comment.key_timestamp) == null ||  data.getString(Comment.key_timestamp) == "")
			data.putString(Comment.key_timestamp, "-1");
		else
			data.putString(Comment.key_timestamp, data.getString(Comment.key_timestamp));
	}
	
	
	
	public Comment(Parcel in){
		readFromParcel(in);
	}
	
	public Boolean hasKey(String key){
		return data.containsKey(key);
	}
	
	public String getData(String key){
		if (data.get(key) != null)
			return data.getString(key);
		else
			return "";
	}
	
	public HashMap<String,String> mapData(){
		HashMap<String,String> dataMap = new HashMap<String,String>();
		
		for (String key : data.keySet()){
			if (data.get(key) instanceof String)
				dataMap.put(key,data.getString(key));
		}
		
		return dataMap;
	}
	
	public List<NameValuePair> mapDataToParams(){
		List<NameValuePair> params = new java.util.ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("comment[body]", data.getString(key_body)));
		params.add(new BasicNameValuePair("comment[timestamp]", data.getString(key_timestamp)));
		params.add(new BasicNameValuePair("comment[reply_to]", data.getString(key_reply_to)));
		return params;
	}
	
	
	
	public void putData(String key,String value){
		data.putString(key,value);
	}
	
	public Parcelable getDataParcelable(String key){
		if (data.get(key) != null)
			return data.getParcelable(key);
		else
			return null;
	}
	
	public void putDataParcelable(String key, Parcelable value){
		data.putParcelable(key, value);
	}
	
	public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        public Comment[] newArray(int size) {
          return new Comment[size];
        }
    };

	
	

    public void readFromParcel(Parcel in) {
    	data = in.readBundle();
    }

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel out, int arg1) {
		// TODO Auto-generated method stub
		out.writeBundle(data);
	}

	
}

package com.soundcloud.android.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.soundcloud.android.CloudUtils;


public class Track extends BaseObj implements Parcelable  {

	public static final String MODEL = "overcast.song";
	
	public static final String DOWNLOAD_STATUS_NONE = "none";
	public static final String DOWNLOAD_STATUS_PENDING = "pending";
	public static final String DOWNLOAD_STATUS_DOWNLOADED = "downloaded";

	public static final String key_id = "id";
	public static final String key_uri = "uri";
	public static final String key_title = "title";
	
	//public static final String key_comments = "comments";

	public static final String key_favorited_by = "favorited_by";
	
	public static final String key_type = "type";
	public static final String key_tracklist = "tracklist";
	
	public static final String key_label_id = "label_id";
	public static final String key_release_day = "release_day";
	public static final String key_release_year = "release_year";
	
	public static final String key_created_at = "created_at";
	public static final String key_license = "license";
	public static final String key_label_name = "label_name";
	public static final String key_original_format = "original_format";
	public static final String key_track_type = "track_type";
	public static final String key_comment_count = "comment_count";
	public static final String key_favoritings_count = "favoritings_count";
	public static final String key_user_playback_count = "user_playback_count";
	public static final String key_purchase_url = "purchase_url";
	public static final String key_bpm = "bpm";
	public static final String key_artwork_url = "artwork_url";
	public static final String key_release_month = "release_month";
	public static final String key_release = "release";
	public static final String key_duration = "duration";
	public static final String key_duration_formatted = "duration_formatted";
	
	public static final String key_user_id = "user_id";
	public static final String key_user = "user";
	public static final String key_username = "username";
	public static final String key_user_permalink = "user_permalink";
	public static final String key_user_avatar_url = "avatar_url";
	public static final String key_user_favorite = "user_favorite";
	public static final String key_user_favorite_id = "user_favorite_id";
	
	public static final String key_waveform_url = "waveform_url";
		
	public static final String key_download_count = "download_count";
	public static final String key_downloadable = "downloadable";
	public static final String key_download_url = "download_url";
	public static final String key_download_status = "download_status";
	public static final String key_download_error = "download_error";
	
	public static final String key_user_played = "user_played";
	
	public static final String key_sharing = "sharing";
	public static final String key_state = "state";
	
	public static final String key_streamable = "streamable";
	public static final String key_stream_url = "stream_url";
	
	public static final String key_play_url = "play_url";
	
	public static final String key_local_play_url = "local_play_url";
	public static final String key_local_artwork_url = "local_artwork_url";
	public static final String key_local_waveform_url = "local_waveform_url";
	public static final String key_local_avatar_url = "local_avatar_url";
	
	public static final String key_permalink = "permalink";
	public static final String key_permalink_url = "permalink_url";
	public static final String key_video_url = "video_url";
	
	public static final String key_tag_list = "tag_list";
	public static final String key_isrc = "isrc";
	public static final String key_genre = "genre";
	
	public static final String key_key_signature = "key_signature";
	public static final String key_description = "description";
	
	public static final String key_load_progress = "progress";
	public static final String key_kb_loaded = "kb_loaded";
	public static final String key_kb_total = "kb_total";
	
	public static final String key_playback_count = "playback_count";
	
	public Comment[] comments;
	
	private boolean mIsPlaylist = false;
	
	private Bundle data;
	
	public enum Parcelables { track, user, comment }
	
	public Track(){
		data = new Bundle();
	}
	
	public Track(JSONObject songObject){

		data = new Bundle();
		
		Iterator keys = songObject.keys();
		while(keys.hasNext()) {
			String key = (String)keys.next();
			if (!key.contentEquals("tracks"))
				try{
					switch (Parcelables.valueOf(key.toLowerCase())){
						case track:
							data.putParcelable(key, new Track(songObject.getJSONObject(key)));
							break;
						case user:
							//User usr = new User(songObject.getJSONObject(key));
							//data.putParcelable(key, usr);
							data.putString(key_user_id, songObject.getJSONObject(key_user).getString(User.key_id));							
							data.putString(key_username, songObject.getJSONObject(key_user).getString(User.key_username));
							data.putString(key_user_avatar_url, songObject.getJSONObject(key_user).getString(User.key_avatar_url));
							break;
						default:
							break;
				}
			
			} catch (IllegalArgumentException e ){
				
				try {
								
					if (songObject.has(key) && songObject.getString(key) != "null"){
						data.putString(key, songObject.getString(key));
					}else
						data.putString(key, "");
				} catch (JSONException e1) {
					data.putString(key, "");
				}
			
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			if (songObject.has("tracks")){
				
				JSONArray collection;
				collection = songObject.getJSONArray("tracks");
				
				Track[] trackArray = new Track[collection.length()];
				data.putString(Track.key_duration, Integer.toString(collection.length()));
				
				for (int i = 0; i < collection.length(); i++) {
					trackArray[i] = new Track(collection.getJSONObject(i));
				}
				data.putParcelableArray(key_tracklist, trackArray);
				
				mIsPlaylist = true;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		resolveData();
	}
	
	public Track(Node datanode){
		
		data = new Bundle();
		ArrayList<Track> tracks = new ArrayList<Track>();
		
		for(int i=0; i < datanode.getChildNodes().getLength() ; i++){
			Node dataItemNode = datanode.getChildNodes().item(i);
			if(dataItemNode.getNodeType() == Node.ELEMENT_NODE){
				
				try{
					switch (Parcelables.valueOf(dataItemNode.getNodeName().toLowerCase())){
						case track:
							tracks.add(new Track(dataItemNode));
							break;
							
						case user:
							for(int j=0; i < datanode.getChildNodes().getLength() ; j++){
								Node dataItemChildNode = dataItemNode.getChildNodes().item(i);
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
		
		if (tracks.size() > 0){ //turn it into an array for adding to parcelable
			data.putString(Track.key_duration, Integer.toString(tracks.size()));
			Track[] trackArray = new Track[tracks.size()];
			int i = 0;
			for (Iterator<Track> trackIterator = tracks.iterator(); trackIterator.hasNext();) {  
			    trackArray[i] = trackIterator.next(); 
			    i++;
			}  
			
			data.putParcelableArray(key_tracklist, trackArray);
			
			mIsPlaylist = true;
		}
		tracks = null;
		
		resolveData();
		
	}
	
	public Track(HashMap<String,String> songData){
		data = new Bundle();
		
		for (String key : songData.keySet())
		{
			data.putString(key, songData.get(key));
		}
		
		resolveData();
	}
	
	public void resolveData() {
		
		if (!mIsPlaylist){
			if (CloudUtils.stringNullEmptyCheck(data.getString(Track.key_duration),true)){
				data.putString(Track.key_duration_formatted, "0");
				return;
			}
			
			int duration = Integer.parseInt(data.getString(Track.key_duration));
			String durationStr = "";
			if (Math.floor(Math.floor((duration/1000)/60)/60) > 0)
				durationStr = String.valueOf((int) Math.floor(Math.floor((duration/1000)/60)/60) + "." + (int) Math.floor((duration/1000)/60)%60) + "." + String.format("%02d",(duration/1000)%60);
			else
				durationStr = String.valueOf((int) Math.floor((duration/1000)/60)%60) + "." + String.format("%02d",(duration/1000)%60);
			data.putString(Track.key_duration_formatted, durationStr);
		}
		
		
	}
	
	
	
	
	public Track(Parcel in){
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
			if (data.getString(key) != null)
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

	public Parcelable[] getDataParcelableArray(String key){
		if (data.get(key) != null)
			return data.getParcelableArray(key);
		else
			return null;
	}
	
	@Override
	public void putDataParcelable(String key, Parcelable value){
		data.putParcelable(key, value);
	}
	
	public void putDataParcelableArray(String key, Parcelable[] value){
		data.putParcelableArray(key, value);
	}
	
	public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
          return new Track[size];
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

	public boolean isPlaylist(){
		return mIsPlaylist;
	}
	
}

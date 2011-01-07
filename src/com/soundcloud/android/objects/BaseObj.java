package com.soundcloud.android.objects;

import java.util.HashMap;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


public class BaseObj implements Parcelable  {
	
	private Bundle data;
	
	public enum Parcelables { track, user, comment }
	
	public BaseObj(){
		data = new Bundle();
	}
	
	
	public BaseObj(Parcel in){
		readFromParcel(in);
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
			if (data.getString(key) != null)
				dataMap.put(key,data.getString(key));
		}
		
		return dataMap;
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
	
	public static final Parcelable.Creator<BaseObj> CREATOR = new Parcelable.Creator<BaseObj>() {
        public BaseObj createFromParcel(Parcel in) {
            return new BaseObj(in);
        }

        public BaseObj[] newArray(int size) {
          return new BaseObj[size];
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

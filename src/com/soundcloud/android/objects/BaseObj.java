package com.soundcloud.android.objects;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.annotate.JsonProperty;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.soundcloud.android.CloudUtils;


public class BaseObj implements Parcelable  {
	
	protected Bundle data;
	
	public enum Parcelables { track, user, comment }
	
	public BaseObj(){
		data = new Bundle();
	}
	
	public BaseObj(Parcel in){
		readFromParcel(in);
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
    	Method m;
    	data = in.readBundle(this.getClass().getClassLoader());
    	for (String key : data.keySet()){
    		
    		try {
    			// I was going to search through annotaitons but it was too expensive, so this is way cheaper
				m =this.getClass().getMethod("set"+CloudUtils.toCamelCase(key),data.get(key).getClass());
				if (m != null){
					m.invoke(this, data.get(key));
	    		}
			} catch (SecurityException e1) {
				e1.printStackTrace();
			} catch (NoSuchMethodException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
    	}
    }

	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void writeToParcel(Parcel out, int arg1) {
		// data = in.readBundle();
		data = new Bundle();
    		for (Method m : this.getClass().getMethods()) {
    			//make sure it is a getter with the right annotation
    			if (!m.getReturnType().equals(Void.TYPE)) //do this first because finding an annotation is expensive
    			if (m.isAnnotationPresent(JsonProperty.class)){
    					try {
    						if (m.invoke(this) != null)
    						if (m.getReturnType() == String.class)
	    						data.putString(((String) m.getAnnotation(JsonProperty.class).value()),(String) m.invoke(this));
	    					else  if (m.getReturnType() == Integer.class)
    							data.putInt(((String) m.getAnnotation(JsonProperty.class).value()),(Integer) m.invoke(this));
	    					else if (m.getReturnType() == Boolean.class)
	    						data.putBoolean(((String) m.getAnnotation(JsonProperty.class).value()),(Boolean) m.invoke(this));
	    					else if (Parcelable.class.isAssignableFrom(m.getReturnType())) {
	    						
	    						Parcelable p = (Parcelable) m.invoke(this);
	    						data.putParcelable(((String) m.getAnnotation(JsonProperty.class).value()),p);
	    					} else {
	    						Log.i("BaseObj","Ignoring "+ m.getAnnotation(JsonProperty.class));
	    					}
						} catch (Exception e) {
							//catch false parcelable mapping or any other false mapping
						} 
    				continue;
    			}
    		}
		out.writeBundle(data);
	}
	
	public Field getPrivateField(CharSequence name) {
	  for (Field field : getClass().getDeclaredFields()) {
		  if (field.getName().contentEquals(name)){
			  return field;
		  }
	  }
	  return null;
	 }

	
}

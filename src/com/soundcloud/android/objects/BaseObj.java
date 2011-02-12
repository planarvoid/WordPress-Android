
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

public class BaseObj implements Parcelable {

    public enum Parcelables {
        track, user, comment
    }

    public enum WriteState {
        none, insert_only, update_only, all
    }

    public BaseObj() {
        
    }

    public BaseObj(Parcel in) {
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
        Field f;
        Bundle data = in.readBundle(this.getClass().getClassLoader());
        for (String key : data.keySet()) {
            try {
                f = this.getClass().getField(key);
                if (f != null) {
                    f.set(this, data.get(key));
                }
            } catch (SecurityException e1) {
                e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void buildParcel(Parcel out, int flags) {
        // data = in.readBundle();
        Bundle data = new Bundle();
        try {
            for (Field f : this.getClass().getDeclaredFields()) {
                    if (f.getType() == String.class)
                        
                            data.putString(f.getName(),
                                    (String) f.get(this));
                       
                    else if (f.getType() == Integer.class)
                        data.putInt(f.getName(),
                                (Integer) f.get(this));
                    else if (f.getType() == Long.class)
                        data.putLong(f.getName(),
                                (Long) f.get(this));
                    else if (f.getType() == Boolean.class)
                        data.putBoolean(f.getName(),
                                (Boolean) f.get(this));
                    else if (Parcelable.class.isAssignableFrom(f.getType())) {
    
                        Parcelable p = (Parcelable) f.get(this);
                        data
                                .putParcelable(f.getName(), p);
                    } else {
                        Log.i("BaseObj", "Ignoring " + f.getName() + " of type " + f.getType());
                    }            
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        out.writeBundle(data);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        buildParcel(dest,flags);        
    }


}

package com.soundcloud.android.streaming;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.utils.CloudUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class ScStreamItem implements Parcelable {

    public static String SCStreamItemDidResetNotification = "SCStreamItemDidResetNotification";

    public String URL;
    public String redirectedURL;
    public boolean enabled;

    private String mURLHash;
    private int mContentLength;


    public void setContentLength(int value){
        if (mContentLength == value) return;

        boolean reset = false;
        if (mContentLength != 0){
            reset = true;
        }
        mContentLength = value;

        /*
        notify(new Intent("SCStreamItemDidResetNotification"));
        */
    }

    public int getContentLength(){
        return mContentLength;
    }

    public String getURLHash(){
        if (TextUtils.isEmpty(mURLHash) && !TextUtils.isEmpty(URL)){
            mURLHash = CloudUtils.md5(URL);
        }
        return mURLHash;
    }

    @Override
    public String toString() {
            return "ScStreamItem{url: " + URL +
                    ", redirectedURL:" + redirectedURL +
                    ", URLHash:" + mURLHash +
                    ", contentLength:" + mContentLength +
                    ", enabled:" + enabled +
                    "}";
        }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putString("URL", URL);
        data.putString("redirectedURL", redirectedURL);
        data.putString("URLHash", mURLHash);
        data.putBoolean("enabled",enabled);
        data.putInt("contentLength",mContentLength);
        dest.writeBundle(data);
    }

    public ScStreamItem(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        URL = data.getString("URL");
        redirectedURL = data.getString("redirectedURL");
        mURLHash = data.getString("URLHash");
        enabled = data.getBoolean("enabled");
        mContentLength = data.getInt("contentLength");
    }

    public static final Parcelable.Creator<ScStreamItem> CREATOR = new Parcelable.Creator<ScStreamItem>() {
        public ScStreamItem createFromParcel(Parcel in) {
            return new ScStreamItem(in);
        }

        public ScStreamItem[] newArray(int size) {
            return new ScStreamItem[size];
        }
    };

}

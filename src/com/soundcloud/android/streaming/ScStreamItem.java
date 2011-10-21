package com.soundcloud.android.streaming;

import android.content.Context;
import android.content.Intent;
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

    public static String SCStreamItemDidResetNotification = "com.soundcloud.android.SCStreamItemDidResetNotification";

    public final String URL;
    public String redirectedURL;
    public boolean enabled;

    private Context mContext;
    private String mURLHash;
    private long mContentLength;

    public ScStreamItem(Context context, String URL) {
        if (TextUtils.isEmpty(URL)) throw new IllegalArgumentException();
        mContext = context;
        this.URL = URL;
    }

    public ScStreamItem(Context context, String URL, long length) {
        this(context, URL);
        setContentLength(length);
    }

    public void setContentLength(long value){
        if (mContentLength == value) return;

        boolean reset = false;
        if (mContentLength != 0){
            reset = true;
        }
        mContentLength = value;

        if (reset){
            Intent i = new Intent(SCStreamItemDidResetNotification);
            i.getExtras().putParcelable("item",this);
            mContext.sendBroadcast(i);
        }

        /*
        TODO add reset listener to player
        */
    }

    public long getContentLength(){
        return mContentLength;
    }

    public String getURLHash(){
        if (mURLHash == null) {
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
        data.putLong("contentLength",mContentLength);
        dest.writeBundle(data);
    }

    public ScStreamItem(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        URL = data.getString("URL");
        redirectedURL = data.getString("redirectedURL");
        mURLHash = data.getString("URLHash");
        enabled = data.getBoolean("enabled");
        mContentLength = data.getLong("contentLength");
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

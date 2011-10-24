package com.soundcloud.android.streaming;

import com.soundcloud.android.utils.CloudUtils;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class StreamItem implements Parcelable {
    public static String SCStreamItemDidResetNotification = "com.soundcloud.android.SCStreamItemDidResetNotification";

    public final String url;
    public String redirectedURL;
    public boolean available = true;  // http status 402,404,410

    private String mURLHash;
    private long mContentLength;
    private String mEtag;

    public StreamItem(String url) {
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException();
        this.url = url;
    }

    public StreamItem(String url, long length) {
        this(url);
        setContentLength(length);
    }

    public boolean setContentLength(long value) {
        if (mContentLength != value) {
            final long oldLength = mContentLength;
            mContentLength = value;


            /*
            TODO: move this out of this class

            if (oldLength != 0) {
                Intent i = new Intent(SCStreamItemDidResetNotification);
                i.getExtras().putParcelable("item", this);
                mContext.sendBroadcast(i);
            }
            TODO add reset listener to player


            */

            return oldLength != 0;
        } else {
            return false;
        }
    }

    public long getContentLength() {
        return mContentLength;
    }

    public Range getRange() {
        return Range.from(0, getContentLength());
    }

    public String getURLHash() {
        if (mURLHash == null) {
            mURLHash = CloudUtils.md5(url);
        }
        return mURLHash;
    }

    @Override
    public String toString() {
        return "ScStreamItem{url: " + url +
                ", redirectedURL:" + redirectedURL +
                ", URLHash:" + mURLHash +
                ", contentLength:" + mContentLength +
                ", enabled:" + available +
                "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putString("URL", url);
        data.putString("redirectedURL", redirectedURL);
        data.putString("URLHash", mURLHash);
        data.putBoolean("enabled", available);
        data.putLong("contentLength", mContentLength);
        dest.writeBundle(data);
    }

    public StreamItem(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        url = data.getString("URL");
        redirectedURL = data.getString("redirectedURL");
        mURLHash = data.getString("URLHash");
        available = data.getBoolean("enabled");
        mContentLength = data.getLong("contentLength");
    }

    public static final Parcelable.Creator<StreamItem> CREATOR = new Parcelable.Creator<StreamItem>() {
        public StreamItem createFromParcel(Parcel in) {
            return new StreamItem(in);
        }

        public StreamItem[] newArray(int size) {
            return new StreamItem[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamItem that = (StreamItem) o;
        return !(url != null ? !url.equals(that.url) : that.url != null);
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}

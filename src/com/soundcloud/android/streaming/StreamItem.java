package com.soundcloud.android.streaming;

import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Stream;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class StreamItem implements Parcelable {
    public final Index index = new Index();

    public final String url;
    public String redirectedURL;
    public boolean unavailable;  // http status 402,404,410
    private String mURLHash;
    private long mContentLength;
    private String mEtag;

    public StreamItem(String url) {
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException();
        this.url = url;
    }

    /* package */ StreamItem(String url, long length, String etag) {
        this(url);
        mContentLength = length;
        mEtag = etag;
    }

    public StreamItem initializeFrom(Stream s) {
        setContentLength(s.contentLength);
        redirectedURL = s.streamUrl;
        mEtag = s.eTag;
        return this;
    }

    public String getETag() {
        return mEtag;
    }

    public boolean setContentLength(long value) {
        if (mContentLength != value) {
            final long oldLength = mContentLength;
            mContentLength = value;


            /*
            TODO: move this out of this class

            public static String SCStreamItemDidResetNotification = "com.soundcloud.android.SCStreamItemDidResetNotification";

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

    public Range byteRange() {
        return Range.from(0, getContentLength());
    }

    public Range chunkRange(int chunkSize) {
        return byteRange().chunkRange(chunkSize);
    }

    public String getURLHash() {
        if (mURLHash == null) {
            mURLHash = urlHash(url);
        }
        return mURLHash;
    }

    public static String urlHash(String url) {
        return CloudUtils.md5(url);
    }

    @Override
    public String toString() {
        return "ScStreamItem{url: " + url +
                ", redirectedURL:" + redirectedURL +
                ", URLHash:" + mURLHash +
                ", contentLength:" + mContentLength +
                ", unavailable:" + unavailable +
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
        data.putBoolean("unavailable", unavailable);
        data.putLong("contentLength", mContentLength);
        dest.writeBundle(data);
    }

    public StreamItem(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        url = data.getString("URL");
        redirectedURL = data.getString("redirectedURL");
        mURLHash = data.getString("URLHash");
        unavailable = data.getBoolean("unavailable");
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


    public StreamStorage.Metadata getMetadata() {
        StreamStorage.Metadata md = new StreamStorage.Metadata();
        md.eTag = mEtag;
        md.contentLength = mContentLength;
        return md;
    }
}

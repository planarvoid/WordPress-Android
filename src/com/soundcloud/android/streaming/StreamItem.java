package com.soundcloud.android.streaming;

import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Stream;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamItem implements Parcelable {
    public final Index chunksToDownload = new Index();
    public final List<Integer> downloadedChunks = new ArrayList<Integer>();

    public final String url;
    public final String urlHash;

    private boolean mUnavailable;  // http status 402, 404, 410
    private long mContentLength;
    private String mRedirectedUrl;
    private String mEtag;  // audio content ETag
    private long mExpires; // expiration time of the redirect link

    public StreamItem(String url) {
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException();
        this.url = url;
        this.urlHash = urlHash(url);
    }

    /* package */ StreamItem(String url, long length, String etag) {
        this(url);
        mContentLength = length;
        mEtag = etag;
    }

    public StreamItem initializeFromStream(Stream s) {
        mRedirectedUrl = s.streamUrl;
        mContentLength = s.contentLength;
        mEtag = s.eTag;
        mExpires = s.expires;
        return this;
    }

    public int numberOfChunks(int chunkSize) {
        return (int) Math.ceil(((double ) getContentLength()) / ((double ) chunkSize));
    }

    public String etag() {
        return mEtag;
    }

    public String redirectUrl() {
        return mRedirectedUrl;
    }

    public void invalidateRedirectUrl() {
        mRedirectedUrl = null;
    }

    public boolean isRedirectValid() {
        return mContentLength > 0 && mRedirectedUrl != null && !isRedirectExpired();
    }

    public void markUnavailable() {
        mUnavailable = true;
    }

    public boolean isRedirectExpired() {
        return System.currentTimeMillis() > mExpires;
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

    public boolean isAvailable() {
        return !mUnavailable;
    }

    public static String urlHash(String url) {
        return CloudUtils.md5(url);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("StreamItem");
        sb.append("{url='").append(url).append('\'');
        sb.append(", urlHash='").append(urlHash).append('\'');
        sb.append(", unavailable=").append(mUnavailable);
        sb.append(", mContentLength=").append(mContentLength);
        sb.append(", mRedirectedUrl='").append(mRedirectedUrl).append('\'');
        sb.append(", mEtag='").append(mEtag).append('\'');
        sb.append(", mExpires=").append(mExpires);
        sb.append(", chunksToDownload=").append(chunksToDownload);
        sb.append(", downloadedChunks=").append(downloadedChunks);
        sb.append('}');
        return sb.toString();
    }

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

    // serialization support
    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(url);
        dos.writeLong(mContentLength);
        dos.writeUTF(mEtag == null ? "" : mEtag);
        dos.writeInt(downloadedChunks.size());
        for (Integer index : downloadedChunks) {
            dos.writeInt(index);
        }
    }

    public static StreamItem read(DataInputStream dis) throws IOException {
        String url = dis.readUTF();
        StreamItem item = new StreamItem(url);
        item.mContentLength = dis.readLong();
        item.mEtag = dis.readUTF();
        int n = dis.readInt();
        for (int i = 0; i < n; i++) {
            item.downloadedChunks.add(dis.readInt());
        }
        return item;
    }

    public static StreamItem fromIndexFile(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            return read(dis);
        } finally {
            dis.close();
        }
    }

    public static StreamItem fromCompleteFile(String url, File file) {
        StreamItem item = new StreamItem(url);
        item.mContentLength = file.length();
        //item.mEtag = CloudUtils.md5(file); // XXX overhead?
        return item;
    }


    // parcelable support
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putString("url", url);
        data.putString("redirectedUrl", mRedirectedUrl);
        data.putString("etag", mEtag);
        data.putBoolean("unavailable", mUnavailable);
        data.putLong("contentLength", mContentLength);
        data.putLong("expires", mExpires);
        // TODO index + downloaded chunks
        dest.writeBundle(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public StreamItem(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        url = data.getString("url");
        urlHash = urlHash(url);
        mRedirectedUrl = data.getString("redirectedUrl");
        mEtag = data.getString("etag");
        mUnavailable = data.getBoolean("unavailable");
        mContentLength = data.getLong("contentLength");
        mExpires = data.getLong("expires");
    }

    public static final Parcelable.Creator<StreamItem> CREATOR = new Parcelable.Creator<StreamItem>() {
        public StreamItem createFromParcel(Parcel in) {
            return new StreamItem(in);
        }

        public StreamItem[] newArray(int size) {
            return new StreamItem[size];
        }
    };

}

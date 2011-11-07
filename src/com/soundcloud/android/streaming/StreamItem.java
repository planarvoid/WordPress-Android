package com.soundcloud.android.streaming;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.api.Stream;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StreamItem implements Parcelable {
    public final Index missingChunks = new Index();
    public final List<Integer> downloadedChunks =
            Collections.synchronizedList(new ArrayList<Integer>());

    public final URL url;
    public final String urlHash;

    private boolean mUnavailable;  // http status 402, 404, 410
    private long mContentLength;
    private URL mRedirectedUrl;
    private String mEtag;  // audio content ETag
    private long mExpires; // expiration time of the redirect link

    private File mCachedFile;

    public StreamItem(String url) {
        if (TextUtils.isEmpty(url)) throw new IllegalArgumentException();
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url",e);
        }

        this.urlHash = urlHash(url);
    }

    public StreamItem(String url, File f) {
        this(url);
        mContentLength = f.length();
        mCachedFile = f;
    }

    /* package */ StreamItem(String url, long length, String etag) {
        this(url);
        mContentLength = length;
        mEtag = etag;
    }

    public StreamItem initializeFromStream(Stream s) {
        try {
            mRedirectedUrl = new URL(s.streamUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        mContentLength = s.contentLength;
        mEtag = s.eTag;
        mExpires = s.expires;
        return this;
    }

    public int numberOfChunks(int chunkSize) {
        return (int) Math.ceil(((double ) getContentLength()) / ((double ) chunkSize));
    }

    public String etag() {
        if (mEtag == null && mCachedFile != null && mCachedFile.exists()) {
            mEtag = '"'+CloudUtils.md5(mCachedFile)+'"';
        }
        return mEtag;
    }

    public URL redirectUrl() {
        return mRedirectedUrl;
    }

    public void invalidateRedirectUrl() {
        mRedirectedUrl = null;
    }

    public boolean isRedirectValid() {
        return mContentLength > 0
                && mRedirectedUrl != null;
                /* && !isRedirectExpired();  */ // unreliable, don't use
    }

    public void markUnavailable() {
        mUnavailable = true;
    }

    /**
     * Checks is the redirect is expired.
     * Note: this assumes that the client clock is correct and is therefore not reliable.
     * @return true if the redirect is no longer valid.
     */
    public boolean isRedirectExpired() {
        return System.currentTimeMillis() > mExpires;
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
        sb.append(", mExpires=").append(mExpires == 0 ? "" : new Date(mExpires));
        sb.append(", chunksToDownload=").append(missingChunks);
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

    public void toIndexFile(File f) throws IOException {
        mkdirs(f.getParentFile());
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            write(dos);
        } finally {
            if (dos != null) dos.close();
        }
    }

    public static StreamItem fromIndexFile(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            return read(dis);
        } finally {
            dis.close();
        }
    }

    /* package */ void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(url.toString());
        dos.writeLong(mContentLength);
        dos.writeUTF(mEtag == null ? "" : mEtag);
        dos.writeInt(downloadedChunks.size());
        for (Integer index : downloadedChunks) {
            dos.writeInt(index);
        }
    }

    /* package */ static StreamItem read(DataInputStream dis) throws IOException {
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


    // parcelable support
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putString("url", url.toString());
        data.putString("redirectedUrl", mRedirectedUrl.toString());
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

    public StreamItem(Parcel in) throws MalformedURLException {
        Bundle data = in.readBundle(getClass().getClassLoader());
        url = new URL(data.getString("url"));
        urlHash = urlHash(url.toString());
        mRedirectedUrl = new URL(data.getString("redirectedUrl"));
        mEtag = data.getString("etag");
        mUnavailable = data.getBoolean("unavailable");
        mContentLength = data.getLong("contentLength");
        mExpires = data.getLong("expires");
    }

    public static final Parcelable.Creator<StreamItem> CREATOR = new Parcelable.Creator<StreamItem>() {
        public StreamItem createFromParcel(Parcel in) {
            try {
                return new StreamItem(in);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public StreamItem[] newArray(int size) {
            return new StreamItem[size];
        }
    };
}

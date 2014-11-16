package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.utils.IOUtils.mkdirs;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Stream;

import android.net.Uri;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamItem implements Parcelable {
    public static final String TRACK_ID_KEY = "track_id";
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
    private static final Pattern STREAM_PATTERN = Pattern.compile("/(\\d+)/stream(\\?secret_token=s-\\w+)?$");
    public final Index missingChunks = new Index();
    public final List<Integer> downloadedChunks =
            Collections.synchronizedList(new ArrayList<Integer>());
    public final String urlHash;
    public final long trackId;
    private final URL url;
    private boolean unavailable;  // http status 402, 404, 410
    private int httpErrorStatus;
    private long contentLength;
    private URL redirectedUrl;
    private String etag;  // audio content ETag
    private long expires; // expiration time of the redirect link
    private int bitrate;
    private File cachedFile;

    public StreamItem(String url) {
        if (TextUtils.isEmpty(url)) {
            throw new IllegalArgumentException();
        }
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url", e);
        }
        trackId = getTrackId(url);
        if (trackId == -1) {
            throw new IllegalArgumentException("could not get track id from " + url);
        }
        this.urlHash = urlHash(url);
    }

    public StreamItem(String url, File f) {
        this(url);
        contentLength = f.length();
        cachedFile = f;
    }

    /* package */ StreamItem(String url, long length, String etag) {
        this(url);
        contentLength = length;
        this.etag = etag;
    }

    public StreamItem(Parcel in) throws MalformedURLException {
        Bundle data = in.readBundle(getClass().getClassLoader());
        url = new URL(data.getString("url"));
        trackId = getTrackId(url.toString());
        urlHash = urlHash(url.toString());
        redirectedUrl = new URL(data.getString("redirectedUrl"));
        etag = data.getString("etag");
        unavailable = data.getBoolean("unavailable");
        contentLength = data.getLong("contentLength");
        expires = data.getLong("expires");
    }

    public StreamItem initializeFromStream(Stream s) {
        try {
            redirectedUrl = new URL(s.streamUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        contentLength = s.contentLength;
        etag = s.eTag;
        expires = s.expires;
        bitrate = s.bitRate;
        httpErrorStatus = 0;
        return this;
    }

    public int numberOfChunks(int chunkSize) {
        return (int) Math.ceil(((double) getContentLength()) / ((double) chunkSize));
    }

    public String etag() {
        if (etag == null && cachedFile != null && cachedFile.exists()) {
            etag = '"' + IOUtils.md5(cachedFile) + '"';
        }
        return etag;
    }

    public URL redirectUrl() {
        return redirectedUrl;
    }

    public boolean isRedirectValid() {
        return contentLength > 0
                && redirectedUrl != null;
                /* && !isRedirectExpired();  */ // unreliable, don't use
    }

    public boolean markUnavailable(int statusCode) {
        httpErrorStatus = statusCode;
        unavailable = PublicApiWrapper.isStatusCodeClientError(statusCode);
        return unavailable;
    }

    public boolean invalidateRedirectUrl(int statusCode) {
        httpErrorStatus = statusCode;
        if (PublicApiWrapper.isStatusCodeClientError(statusCode)) {
            redirectedUrl = null;
        }
        return redirectedUrl == null;
    }

    public int getHttpError() {
        return httpErrorStatus;
    }

    /**
     * Checks is the redirect is expired.
     * Note: this assumes that the client clock is correct and is therefore not reliable.
     *
     * @return true if the redirect is no longer valid.
     */
    public boolean isRedirectExpired() {
        return System.currentTimeMillis() > expires;
    }

    public long getContentLength() {
        return contentLength;
    }

    public int getBitrate() {
        return bitrate;
    }

    public Range byteRange() {
        return Range.from(0, getContentLength());
    }

    public Range chunkRange(int chunkSize) {
        return byteRange().chunkRange(chunkSize);
    }

    public boolean isAvailable() {
        return !unavailable;
    }

    public static String urlHash(String url) {
        return IOUtils.md5(url);
    }

    public static long getTrackId(String url) {
        final Uri uri = Uri.parse(url);
        String id = uri.getQueryParameter(TRACK_ID_KEY);
        if (id == null) {
            Matcher m = STREAM_PATTERN.matcher(url);
            if (m.find()) {
                id = m.group(1);
            } else {
                return -1;
            }
        }

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(500)
                .append("StreamItem{url='").append(url).append('\'')
                .append(", urlHash='").append(urlHash).append('\'')
                .append(", unavailable=").append(unavailable)
                .append(", contentLength=").append(contentLength)
                .append(", redirectedUrl='").append(redirectedUrl).append('\'')
                .append(", etag='").append(etag).append('\'')
                .append(", expires=").append(expires == 0 ? "" : new Date(expires))
                .append(", chunksToDownload=").append(missingChunks)
                .append(", httpStatus=").append(httpErrorStatus)
                .append(", downloadedChunks=").append(downloadedChunks)
                .append('}');
        return sb.toString();
    }

    // serialization support

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StreamItem that = (StreamItem) o;
        return !(url != null ? !url.toString().equals(that.url.toString()) : that.url != null);
    }

    @Override
    public int hashCode() {
        return url != null ? url.toString().hashCode() : 0;
    }

    public void toIndexFile(File f) throws IOException {
        mkdirs(f.getParentFile());
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            write(dos);
        } finally {
            if (dos != null) {
                dos.close();
            }
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

    public String streamItemUrl() {
        return url.toString();
    }

    public URL getUrl() {
        return url;
    }

    // parcelable support
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle data = new Bundle();
        data.putString("url", url.toString());
        data.putString("redirectedUrl", redirectedUrl.toString());
        data.putString("etag", etag);
        data.putBoolean("unavailable", unavailable);
        data.putLong("contentLength", contentLength);
        data.putLong("expires", expires);
        // TODO index + downloaded chunks
        dest.writeBundle(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /* package */ void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(url.toString());
        dos.writeLong(contentLength);
        dos.writeUTF(etag == null ? "" : etag);
        dos.writeInt(downloadedChunks.size());
        for (Integer index : downloadedChunks) {
            dos.writeInt(index);
        }
    }

    /* package */
    static StreamItem read(DataInputStream dis) throws IOException {
        final String url = dis.readUTF();
        if (TextUtils.isEmpty(url)) {
            throw new IOException("no url stored");
        }
        StreamItem item = new StreamItem(url);
        item.contentLength = dis.readLong();
        item.etag = dis.readUTF();
        int n = dis.readInt();
        for (int i = 0; i < n; i++) {
            item.downloadedChunks.add(dis.readInt());
        }
        return item;
    }
}

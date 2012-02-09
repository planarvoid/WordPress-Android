package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.*;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.IOUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StatFs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@JsonAutoDetect(JsonMethod.NONE)
public class Beta implements Comparable<Beta>, Parcelable {
    public static final String EXTRA_BETA_VERSION = "beta-version";
    @JsonProperty String key;
    @JsonProperty long lastmodified;
    @JsonProperty String etag;
    @JsonProperty long size;
    @JsonProperty String storageClass;
    @JsonProperty Map<String,String> metadata = new HashMap<String, String>();

    private static final Pattern VERSION = Pattern.compile(".*-(\\d+).apk$");
    public static final String META_DATA_EXT = ".json";

    // metadata fields
    public static final String VERSION_NAME = "android-versionname";
    public static final String VERSION_CODE = "android-versioncode";
    public static final String GIT_SHA1     = "git-sha1";


    public Uri getURI() {
        return BetaService.BETA_BUCKET.buildUpon().appendPath(key).build();
    }

    public File getLocalFile() {
        return new File(BetaService.APK_PATH, key);
    }

    public File getMetaDataFile() {
        return new File(BetaService.APK_PATH, key+META_DATA_EXT);
    }

    public boolean isDownloaded() {
        // could check MD5 here, but too expensive
        return getMetaDataFile().exists() &&
               getLocalFile().exists() &&
               getLocalFile().length() == size;
              /*  disabled: http://code.google.com/p/android/issues/detail?id=18624
               && getLocalFile().lastModified() == lastmodified;
              */
    }

    public boolean isEnoughStorageLeft() {
        if (SoundCloudApplication.DALVIK)  { // XXX
            StatFs fs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            final long free = (long) fs.getAvailableBlocks() * (long) fs.getBlockSize();
            return (size * 3l) < free;
        } else {
            return true;
        }
    }

    public int getVersionCode() {
        if (metadata.containsKey(VERSION_CODE)) {
            try {
                return Integer.parseInt(metadata.get(VERSION_CODE));
            } catch (NumberFormatException e) {
                return -1;
            }
        } else if (key == null) {
            return -1;
        } else {
            Matcher m = VERSION.matcher(key);
            return (m.matches()) ? Integer.parseInt(m.group(1)) : -1;
        }
    }

    public String getVersionName() {
        return metadata.containsKey(VERSION_NAME) ? metadata.get(VERSION_NAME) : "unknown";
    }

    public String getGitSha1() {
        return metadata.containsKey(GIT_SHA1) ? metadata.get(GIT_SHA1) : "unknown";
    }

    public long downloadTime() {
        if (getMetaDataFile().exists()) {
            return getMetaDataFile().lastModified();
        } else {
            return -1;
        }
    }
    public void touch() throws IOException {
        if (!getLocalFile().setLastModified(lastmodified)) {

            if (Build.VERSION.SDK_INT < 11) {
                // XXX honeycomb+ devices don't support setLastModified
                // http://code.google.com/p/android/issues/detail?id=18624
                throw new IOException("could not set last modified");
            }
        }
    }

    public boolean isInstalled(Context context) {
        return isInstalled(context, getVersionCode(), getVersionName());
    }

    public static boolean isInstalled(Context context, int newVersionCode, String newVersionName) {
        return isUptodate(
                getAppVersionCode(context, -1),
                getAppVersion(context, ""),
                newVersionCode,
                newVersionName);
    }

    public static boolean isUptodate(int currentVersionCode,
                                     String currentVersionName,
                                     int newVersionCode,
                                     String newVersionName) {

        return currentVersionCode > newVersionCode ||
               currentVersionCode == newVersionCode && currentVersionName.equals(newVersionName);
    }

    @Override
    public String toString() {
        return "Content{" +
                "key='" + key + '\'' +
                ", lastmodified=" + lastmodified +
                ", etag='" + etag + '\'' +
                ", size=" + size +
                ", storageClass='" + storageClass + '\'' +
                ", metadata=" + metadata +
                ", file=" + (isDownloaded() ? getLocalFile() : null) +
                ", uri="+ getURI() +
                '}';
    }

    @Override
    public int compareTo(Beta content) {
        return getVersionCode() == content.getVersionCode() ? 0 :
               getVersionCode()  > content.getVersionCode() ? -1 : 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeLong(lastmodified);
        dest.writeString(etag);
        dest.writeLong(size);
        dest.writeString(storageClass);
        dest.writeMap(metadata);
    }

    /** @noinspection unchecked*/
    public static Creator<Beta> CREATOR = new Parcelable.Creator<Beta>() {
        @Override
        public Beta createFromParcel(final Parcel source) {
            return new Beta() {
                {
                    key = source.readString();
                    lastmodified = source.readLong();
                    etag = source.readString();
                    size = source.readLong();
                    storageClass = source.readString();
                    metadata = source.readHashMap(Beta.class.getClassLoader());
                }
            };
        }

        @Override
        public Beta[] newArray(int size) {
            return new Beta[size];
        }
    };

    public Intent getInstallIntent() {
        return new Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.fromFile(getLocalFile()), "application/vnd.android.package-archive");
    }

    public void deleteFiles() {
        IOUtils.deleteFile(getLocalFile());
        IOUtils.deleteFile(getMetaDataFile());
    }

    // JSON bindings
    static final ObjectMapper mapper = new ObjectMapper();

    public String toJSON() throws IOException {
        return mapper.writeValueAsString(this);
    }

    public static Beta fromJSON(String json) throws IOException {
        return mapper.readValue(json, Beta.class);
    }

    public static Beta fromJSON(File json) throws IOException {
        return mapper.readValue(json, Beta.class);
    }

    public void persist() throws IOException {
        FileOutputStream fos = new FileOutputStream(getMetaDataFile());
        mapper.writeValue(fos, this);
        fos.close();
    }
}

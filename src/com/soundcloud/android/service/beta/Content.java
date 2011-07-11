package com.soundcloud.android.service.beta;

import static com.soundcloud.android.utils.CloudUtils.deleteFile;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@JsonAutoDetect(JsonMethod.NONE)
public class Content implements Comparable<Content>, Parcelable {
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
               getLocalFile().length() == size &&
               getLocalFile().lastModified() == lastmodified;
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
    public int compareTo(Content content) {
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
    public static Creator<Content> CREATOR = new Parcelable.Creator<Content>() {
        @Override
        public Content createFromParcel(final Parcel source) {
            return new Content() {
                {
                    key = source.readString();
                    lastmodified = source.readLong();
                    etag = source.readString();
                    size = source.readLong();
                    storageClass = source.readString();
                    metadata = source.readHashMap(Content.class.getClassLoader());
                }
            };
        }

        @Override
        public Content[] newArray(int size) {
            return new Content[size];
        }
    };

    public Intent getInstallIntent() {
        return new Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.fromFile(getLocalFile()), "application/vnd.android.package-archive");
    }

    public void deleteFiles() {
        deleteFile(getLocalFile());
        deleteFile(getMetaDataFile());
    }

    // JSON bindings
    static final ObjectMapper mapper = new ObjectMapper();

    public String toJSON() throws IOException {
        return mapper.writeValueAsString(this);
    }

    public static Content fromJSON(String json) throws IOException {
        return mapper.readValue(json, Content.class);
    }

    public static Content fromJSON(File json) throws IOException {
        return mapper.readValue(json, Content.class);
    }

    public void persist() throws IOException {
        FileOutputStream fos = new FileOutputStream(getMetaDataFile());
        mapper.writeValue(fos, this);
        fos.close();
    }
}

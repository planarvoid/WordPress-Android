
package com.soundcloud.android.objects;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.utils.record.CloudRecorder.Profile;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Recording extends BaseObj implements Parcelable {
    public long id;
    public long user_id;
    public long timestamp;
    public double longitude;
    public double latitude;
    public String what_text;
    public String where_text;
    public String audio_path;
    public String artwork_path;
    public String four_square_venue_id;
    public String shared_emails;
    public String service_ids;
    public boolean is_private;
    public boolean external_upload;
    public int audio_profile;
    public boolean uploaded;
    public boolean upload_error;

    public Map<String,Object> upload_data;

    public static final class Recordings implements BaseColumns {
        private Recordings() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Recordings");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.recordings";
        public static final String ITEM_TYPE = "vnd.android.cursor.item/soundcloud.recordings";

        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String WHAT_TEXT = "what_text";
        public static final String WHERE_TEXT = "where_text";
        public static final String AUDIO_PATH = "audio_path";
        public static final String ARTWORK_PATH = "artwork_path";
        public static final String FOUR_SQUARE_VENUE_ID = "four_square_venue_id";
        public static final String SHARED_EMAILS = "shared_emails";
        public static final String SERVICE_IDS = "service_ids";
        public static final String IS_PRIVATE = "is_private";
        public static final String EXTERNAL_UPLOAD = "external_upload";
        public static final String AUDIO_PROFILE = "audio_profile";
        public static final String UPLOADED = "uploaded";
        public static final String UPLOAD_ERROR = "upload_error";
    }

    public Recording() {
    }

    public Recording(Parcel in) {
        readFromParcel(in);
    }

    public Recording(Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();

            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                if (key.contentEquals("_id")) id = cursor.getLong(cursor.getColumnIndex(key));
                else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class){
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) != 0);
                        } else if (f.getType() == Double.TYPE) {
                            f.set(this, cursor.getDouble(cursor.getColumnIndex(key)));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (IllegalAccessException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (SecurityException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                }
            }
        }
    }

    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void prepareUploadData(){
        upload_data = new HashMap<String, Object>();
        upload_data.put(CloudAPI.Params.SHARING, is_private ? CloudAPI.Params.PRIVATE : CloudAPI.Params.PUBLIC);
        upload_data.put(CloudAPI.Params.DOWNLOADABLE, false);
        upload_data.put(CloudAPI.Params.STREAMABLE, true);


        if (!is_private) {

            List<Integer> serviceIds = new ArrayList<Integer>();
            if (!TextUtils.isEmpty(service_ids))
            for (String serviceId : service_ids.split(",")){
                if (!TextUtils.isEmpty(serviceId)) serviceIds.add(Integer.valueOf(serviceId));
            }

             if (!serviceIds.isEmpty()) {
                upload_data.put(
                        CloudAPI.Params.SHARING_NOTE,
                        CloudUtils.generateRecordingSharingNote(what_text,
                                where_text, timestamp));
                upload_data.put(CloudAPI.Params.POST_TO, serviceIds);
             } else {
                upload_data.put(CloudAPI.Params.POST_TO_EMPTY, "");
             }
        } else {

             if (!TextUtils.isEmpty(shared_emails)) {
                 upload_data.put(CloudAPI.Params.SHARED_EMAILS, Arrays.asList(shared_emails.split(",")));
             }
        }

        upload_data.put(UploadTask.Params.SOURCE_PATH, audio_path);

        final String title = CloudUtils.generateRecordingSharingNote(what_text,
                where_text, timestamp);

        upload_data.put(CloudAPI.Params.TITLE, title);
        upload_data.put(CloudAPI.Params.TYPE, "recording");

        // add machine tags
        List<String> tags = new ArrayList<String>();

        if (external_upload) {
            tags.add("soundcloud:source=android-3rdparty-upload");
        } else {
            tags.add("soundcloud:source=android-record");
        }

        if (TextUtils.isEmpty(artwork_path)) upload_data.put(UploadTask.Params.ARTWORK_PATH, artwork_path);
        if (TextUtils.isEmpty(four_square_venue_id)) tags.add("foursquare:venue="+four_square_venue_id);
        if (latitude  != 0) tags.add("geo:lat="+latitude);
        if (longitude != 0) tags.add("geo:lon="+longitude);
        upload_data.put(CloudAPI.Params.TAG_LIST, TextUtils.join(" ", tags));

        File audio_file = new File(audio_path);

        if (audio_profile == Profile.RAW && !external_upload) {
            upload_data.put(UploadTask.Params.OGG_FILENAME,new File(audio_file.getParentFile(), generateFilename(title,"ogg")).getAbsolutePath());
            upload_data.put(UploadTask.Params.ENCODE, true);
        } else {
            if (!external_upload){
                File newRecFile = new File(audio_file.getParentFile(), generateFilename(title, "mp4"));

                if (!audio_file.equals(newRecFile) || audio_file.renameTo(newRecFile)) {
                    audio_file = newRecFile;
                    this.audio_path = audio_file.getAbsolutePath();
                }
            }
        }

        upload_data.put(UploadTask.Params.SOURCE_PATH, audio_file.getAbsolutePath());
    }

    private String generateFilename(String title, String extension) {
        return String.format("%s_%s.%s", title.replace(" ","_"),
               DateFormat.format("yyyy-MM-dd-hh-mm-ss", timestamp), extension);
    }



}

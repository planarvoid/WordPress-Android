
package com.soundcloud.android.objects;

import com.soundcloud.android.SoundCloudDB.Recordings;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;
import com.soundcloud.api.CloudAPI;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
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
    public long duration;
    public String artwork_path;
    public String four_square_venue_id;
    public String shared_emails;
    public String service_ids;
    public boolean is_private;
    public boolean external_upload;
    public int audio_profile;
    public int upload_status;
    public boolean upload_error;

    public Map<String,Object> upload_data;

    public static interface UploadStatus {
        public static final int NOT_YET_UPLOADED    = 0;
        public static final int UPLOADING           = 1;
        public static final int UPLOADED            = 2;
    }

    public Recording() {
    }

    public Recording(File f) {
        audio_path = f.getAbsolutePath();
        audio_profile = Profile.ENCODED_LOW;
        timestamp = f.lastModified();
    }

    public Recording(Parcel in) {
        readFromParcel(in);
    }

    public Recording(Cursor cursor) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (key.contentEquals("_id"))
                id = cursor.getLong(cursor.getColumnIndex(key));
            else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class) {
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

    public ContentValues buildContentValues(){
        ContentValues cv = new ContentValues();
        cv.put(Recordings.ID, id);
        cv.put(Recordings.USER_ID, user_id);
        cv.put(Recordings.TIMESTAMP, timestamp);
        cv.put(Recordings.LONGITUDE, longitude);
        cv.put(Recordings.LATITUDE, latitude);
        cv.put(Recordings.WHAT_TEXT, what_text);
        cv.put(Recordings.WHERE_TEXT, where_text);
        cv.put(Recordings.AUDIO_PATH, audio_path);
        cv.put(Recordings.DURATION, duration);
        cv.put(Recordings.ARTWORK_PATH, artwork_path);
        cv.put(Recordings.FOUR_SQUARE_VENUE_ID, four_square_venue_id);
        cv.put(Recordings.SHARED_EMAILS, shared_emails);
        cv.put(Recordings.SERVICE_IDS, service_ids);
        cv.put(Recordings.IS_PRIVATE, is_private);
        cv.put(Recordings.EXTERNAL_UPLOAD, external_upload);
        cv.put(Recordings.AUDIO_PROFILE, audio_profile);
        cv.put(Recordings.UPLOAD_STATUS, upload_status);
        cv.put(Recordings.UPLOAD_ERROR, upload_error);
        return cv;
    }

    public void prepareForUpload(){
        upload_data = new HashMap<String, Object>();
        upload_data.put(CloudAPI.TrackParams.SHARING, is_private ? CloudAPI.TrackParams.PRIVATE : CloudAPI.TrackParams.PUBLIC);
        upload_data.put(CloudAPI.TrackParams.DOWNLOADABLE, false);
        upload_data.put(CloudAPI.TrackParams.STREAMABLE, true);

        if (!is_private) {

            List<Integer> serviceIds = new ArrayList<Integer>();
            if (!TextUtils.isEmpty(service_ids))
            for (String serviceId : service_ids.split(",")){
                if (!TextUtils.isEmpty(serviceId)) serviceIds.add(Integer.valueOf(serviceId));
            }

             if (!serviceIds.isEmpty()) {
                upload_data.put(
                        CloudAPI.TrackParams.SHARING_NOTE,
                        CloudUtils.generateRecordingSharingNote(what_text,
                                where_text, timestamp));
                upload_data.put(CloudAPI.TrackParams.POST_TO, serviceIds);
             } else {
                upload_data.put(CloudAPI.TrackParams.POST_TO_EMPTY, "");
             }
        } else {

             if (!TextUtils.isEmpty(shared_emails)) {
                 upload_data.put(CloudAPI.TrackParams.SHARED_EMAILS, Arrays.asList(shared_emails.split(",")));
             }
        }

        upload_data.put(UploadTask.Params.SOURCE_PATH, audio_path);

        final String title = CloudUtils.generateRecordingSharingNote(what_text,
                where_text, timestamp);

        upload_data.put(CloudAPI.TrackParams.TITLE, title);
        upload_data.put(CloudAPI.TrackParams.TYPE, "recording");

        // add machine tags
        List<String> tags = new ArrayList<String>();

        if (external_upload) {
            tags.add("soundcloud:source=android-3rdparty-upload");
        } else {
            tags.add("soundcloud:source=android-record");
        }


        if (!TextUtils.isEmpty(artwork_path)) upload_data.put(UploadTask.Params.ARTWORK_PATH, artwork_path);
        if (!TextUtils.isEmpty(four_square_venue_id)) tags.add("foursquare:venue="+four_square_venue_id);
        if (latitude  != 0) tags.add("geo:lat="+latitude);
        if (longitude != 0) tags.add("geo:lon="+longitude);
        upload_data.put(CloudAPI.TrackParams.TAG_LIST, TextUtils.join(" ", tags));

        File audio_file = new File(audio_path);
        if (audio_profile == Profile.RAW && !external_upload) {
            upload_data.put(UploadTask.Params.OGG_FILENAME,new File(audio_file.getParentFile(), generateFilename(title,"ogg")).getAbsolutePath());
            upload_data.put(UploadTask.Params.ENCODE, true);
        } else {
            if (!external_upload){
                File newRecFile = new File(audio_file.getParentFile(), generateFilename(
                        title,
                        audio_file.getName().contains(".") ? audio_file.getName().substring(
                                audio_file.getName().lastIndexOf(".") + 1) : "mp4"));

                if (!audio_file.equals(newRecFile) && audio_file.renameTo(newRecFile)) {
                    audio_file = newRecFile;
                    audio_path = audio_file.getAbsolutePath();
                }
            }
        }

        upload_data.put(UploadTask.Params.SOURCE_PATH, audio_file.getAbsolutePath());
        upload_data.put(UploadTask.Params.LOCAL_RECORDING_ID, id);

        upload_status = UploadStatus.UPLOADING;
    }

    private String generateFilename(String title, String extension) {
        return String.format("%s_%s.%s", title.replace(" ","_"),
               DateFormat.format("yyyy-MM-dd-hh-mm-ss", timestamp), extension);
    }
}

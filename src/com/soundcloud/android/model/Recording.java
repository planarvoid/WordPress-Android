
package com.soundcloud.android.model;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import android.provider.SyncStateContract;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.service.CloudCreateService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedDeclaration"})
public class Recording extends ModelBase implements PageTrackable {
    public long user_id;
    public long timestamp;
    public double longitude;
    public double latitude;
    public String what_text;
    public String where_text;
    public File audio_path;
    /** in msecs */
    public long duration;
    public File artwork_path;
    public String four_square_venue_id; /* this is actually a hex id */
    public String shared_emails;
    public String shared_ids;
    public long private_user_id;
    public String service_ids;
    public boolean is_private;
    public boolean external_upload;
    public int audio_profile;
    public int upload_status;
    public boolean upload_error;

    public String[] tags;
    public String description, genre;

    private Map<String,Object> mUpload_data;

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");
    private static final Pattern COMPRESSED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    public String private_username;

    public File generateImageFile(File imageDir) {
        if (audio_path == null) {
            return null;
        } else {
            if (audio_path.getName().contains(".")) {
                return new File(imageDir, audio_path.getName().substring(0, audio_path.getName().lastIndexOf(".")) + ".bmp");
            } else {
                return new File(imageDir, audio_path.getName()+".bmp");
            }
        }
    }

    public boolean exists() {
        return audio_path.exists();
    }

    public Recording(File f) {
        if (f == null) throw new IllegalArgumentException("file is null");
        audio_path = f;
        audio_profile = Profile.ENCODED_LOW;
        timestamp = f.lastModified();
    }

    public Recording(Parcel in) {
        readFromParcel(in);
    }

    public Recording(Cursor cursor) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (key.contentEquals("_id")) {
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
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
                        }  else if (f.getType() == File.class) {
                            if (!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(key)))){
                                f.set(this, new File(cursor.getString(cursor.getColumnIndex(key))));
                            }
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

        // enforce proper construction
        if (audio_path == null) {
            throw new IllegalArgumentException("audio_path is null");
        }
    }

    public static Recording fromUri(Uri uri, ContentResolver resolver) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        try {
            return cursor != null && cursor.moveToFirst() ? new Recording(cursor) : null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static Recording pendingFromPrivateUserId(long id, ContentResolver resolver) {
        Cursor cursor = resolver.query(DatabaseHelper.Content.RECORDINGS, null,
                Recordings.PRIVATE_USER_ID + " = ? AND " + Recordings.UPLOAD_STATUS + " = ?",
                new String[]{Long.toString(id), String.valueOf(Upload.UploadStatus.NOT_YET_UPLOADED)}, null);

        try {
            return cursor != null && cursor.moveToFirst() ? new Recording(cursor) : null;
        } finally {
            if (cursor != null) cursor.close();
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

    public ContentValues buildContentValues(){
        ContentValues cv = new ContentValues();
        cv.put(Recordings.USER_ID, user_id);
        cv.put(Recordings.TIMESTAMP, timestamp);
        cv.put(Recordings.LONGITUDE, longitude);
        cv.put(Recordings.LATITUDE, latitude);
        cv.put(Recordings.WHAT_TEXT, what_text);
        cv.put(Recordings.WHERE_TEXT, where_text);
        cv.put(Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
        cv.put(Recordings.DURATION, duration);
        if (artwork_path != null) cv.put(Recordings.ARTWORK_PATH, artwork_path.getAbsolutePath());
        cv.put(Recordings.FOUR_SQUARE_VENUE_ID, four_square_venue_id);
        cv.put(Recordings.SHARED_EMAILS, shared_emails);
        cv.put(Recordings.SHARED_IDS, shared_ids);
        cv.put(Recordings.PRIVATE_USER_ID, private_user_id);
        cv.put(Recordings.SERVICE_IDS, service_ids);
        cv.put(Recordings.IS_PRIVATE, is_private);
        cv.put(Recordings.EXTERNAL_UPLOAD, external_upload);
        cv.put(Recordings.AUDIO_PROFILE, audio_profile);
        cv.put(Recordings.UPLOAD_STATUS, upload_status);
        cv.put(Recordings.UPLOAD_ERROR, upload_error);
        return cv;
    }

    public static boolean isRawFilename(String filename) {
        return RAW_PATTERN.matcher(filename).matches();
    }

    public static boolean isCompressedFilename(String filename){
        return COMPRESSED_PATTERN.matcher(filename).matches();
    }

    public String sharingNote(Resources res) {
        return CloudUtils.generateRecordingSharingNote(
                res,
                what_text,
                where_text,
                timestamp);
    }


    public Uri toUri() {
        return DatabaseHelper.Content.RECORDINGS
                .buildUpon()
                .appendEncodedPath(String.valueOf(id))
                .build();
    }

    public String getStatus(Resources resources) {
        if (upload_status == 1) {
            return resources.getString(R.string.cloud_upload_currently_uploading);
        } else {
            return CloudUtils.getTimeElapsed(resources, timestamp)
                    + ", "
                    + formattedDuration()
                    + ", "
                    + (upload_error ?
                    resources.getString(R.string.cloud_upload_upload_failed) :
                    resources.getString(R.string.cloud_upload_not_yet_uploaded));
        }
    }

    public String formattedDuration() {
        return  CloudUtils.formatTimestamp(duration);
    }

    public boolean delete(ContentResolver resolver) {
        boolean deleted = false;
        if (!external_upload && audio_path.exists()) {
            deleted = audio_path.delete();
        }
        if (resolver != null) resolver.delete(toUri(), null, null);
        return deleted;
    }

    @Override
    public String toString() {
        return "Recording{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", timestamp=" + timestamp +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", what_text='" + what_text + '\'' +
                ", where_text='" + where_text + '\'' +
                ", audio_path=" + audio_path +
                ", duration=" + duration +
                ", artwork_path=" + artwork_path +
                ", four_square_venue_id='" + four_square_venue_id + '\'' +
                ", shared_emails='" + shared_emails + '\'' +
                ", shared_ids='" + shared_ids + '\'' +
                ", service_ids='" + service_ids + '\'' +
                ", is_private=" + is_private +
                ", external_upload=" + external_upload +
                ", audio_profile=" + audio_profile +
                ", upload_status=" + upload_status +
                ", upload_error=" + upload_error +
                ", tags=" + (tags == null ? null : Arrays.asList(tags)) +
                ", description='" + description + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }

    @Override
    public String pageTrack(String... params) {
         return is_private ? Consts.Tracking.SHARE_PRIVATE : Consts.Tracking.SHARE_PUBLIC;
    }
}

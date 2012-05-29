
package com.soundcloud.android.model;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedDeclaration"})
public class Recording extends ScModel {
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

    public Recording(Cursor c) {
        this.id = c.getLong(c.getColumnIndex(Recordings._ID));
        this.user_id = c.getLong(c.getColumnIndex(Recordings.USER_ID));
        this.timestamp = c.getLong(c.getColumnIndex(Recordings.TIMESTAMP));
        this.longitude = c.getDouble(c.getColumnIndex(Recordings.LONGITUDE));
        this.latitude = c.getDouble(c.getColumnIndex(Recordings.LATITUDE));
        this.what_text = c.getString(c.getColumnIndex(Recordings.WHAT_TEXT));
        this.where_text = c.getString(c.getColumnIndex(Recordings.WHERE_TEXT));
        this.audio_path = new File(c.getString(c.getColumnIndex(Recordings.AUDIO_PATH)));
        final String artwork = c.getString(c.getColumnIndex(Recordings.ARTWORK_PATH));
        this.artwork_path = artwork == null ? null : new File(artwork);
        final String audio = c.getString(c.getColumnIndex(Recordings.AUDIO_PATH));
        this.audio_path = audio == null ? null : new File(audio);
        this.duration = c.getLong(c.getColumnIndex(Recordings.DURATION));
        this.four_square_venue_id = c.getString(c.getColumnIndex(Recordings.FOUR_SQUARE_VENUE_ID));
        this.shared_emails = c.getString(c.getColumnIndex(Recordings.SHARED_EMAILS));
        this.shared_ids = c.getString(c.getColumnIndex(Recordings.SHARED_IDS));
        this.private_user_id = c.getLong(c.getColumnIndex(Recordings.PRIVATE_USER_ID));
        int usernameIdx = c.getColumnIndex(DBHelper.Users.USERNAME);
        if (usernameIdx != -1) { // gets joined in
            this.private_username = c.getString(usernameIdx);
        }
        this.service_ids = c.getString(c.getColumnIndex(Recordings.SERVICE_IDS));
        this.is_private = c.getInt(c.getColumnIndex(Recordings.IS_PRIVATE)) == 1;
        this.external_upload = c.getInt(c.getColumnIndex(Recordings.EXTERNAL_UPLOAD)) == 1;
        this.audio_profile = c.getInt(c.getColumnIndex(Recordings.AUDIO_PROFILE));
        this.upload_status = c.getInt(c.getColumnIndex(Recordings.UPLOAD_STATUS));
        this.upload_error = c.getInt(c.getColumnIndex(Recordings.UPLOAD_ERROR)) == 1;

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
        Cursor cursor = resolver.query(Content.RECORDINGS.uri, null,
                Recordings.PRIVATE_USER_ID + " = ? AND " + Recordings.UPLOAD_STATUS + " = ?",
                new String[]{ Long.toString(id), String.valueOf(Upload.UploadStatus.NOT_YET_UPLOADED)}, null);

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
        ContentValues cv = super.buildContentValues();
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
        return Content.RECORDINGS.forId(id);
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
}

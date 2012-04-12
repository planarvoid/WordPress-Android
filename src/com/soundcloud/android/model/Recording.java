
package com.soundcloud.android.model;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.Time;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Recording extends ScModel implements Comparable<Recording> {
    // basic properties
    public long user_id;

    public String title;

    public String what_text;
    public String where_text;

    public long duration; // in msecs

    public boolean is_private;
    public String[] tags;
    public String description, genre;

    public double longitude;
    public double latitude;

    // assets
    public File audio_path;
    public File encoded_audio_path;
    public File artwork_path;
    public File resized_artwork_path;

    // sharing
    public String four_square_venue_id; /* hex */
    public String shared_emails;
    public String shared_ids;
    public String service_ids;
    public String private_username;
    public long private_user_id;

    // status
    public boolean external_upload;
    public int upload_status;

    // upload state
    public int status;
    private boolean mSuccess;
    private Exception mUploadException;

    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm)$");
    private static final Pattern ENCODED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    public static final String TAG_SOURCE_ANDROID_RECORD    = "soundcloud:source=android-record";
    public static final String TAG_RECORDING_TYPE_DEDICATED = "soundcloud:recording-type=dedicated";
    public static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";


    public static interface Status {
        int NOT_YET_UPLOADED    = 0;
        int UPLOADING           = 1;
        int UPLOADED            = 2;
        int ERROR               = 3;
    }

    public long lastModified() {
        return audio_path.lastModified();
    }

    public String getAbsolutePath() {
        return audio_path.getAbsolutePath();
    }

    public boolean setLastModified(long l) {
        return audio_path.setLastModified(l);
    }

    public Recording(File f) {
        if (f == null) throw new IllegalArgumentException("file is null");
        audio_path = f;
    }

    public Recording(Cursor c) {
        this.id = c.getLong(c.getColumnIndex(Recordings._ID));
        this.user_id = c.getLong(c.getColumnIndex(Recordings.USER_ID));
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
        this.upload_status = c.getInt(c.getColumnIndex(Recordings.UPLOAD_STATUS));

        // enforce proper construction
        if (audio_path == null) {
            throw new IllegalArgumentException("audio_path is null");
        }
    }

    public Recording(Parcel in) {
        readFromParcel(in);

        // enforce proper construction
        if (audio_path == null) {
            throw new IllegalArgumentException("audio_path is null");
        }

    }

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

    public List<String> getTags() {
        // add machine tags
        List<String> tags = new ArrayList<String>();
        if (this.tags != null) {
            for (String t : this.tags) {
                tags.add(t.contains(" ") ? "\""+t+"\"" : t);
            }
        }
        if (!TextUtils.isEmpty(four_square_venue_id)) tags.add("foursquare:venue=" + four_square_venue_id);
        if (latitude != 0) tags.add("geo:lat=" + latitude);
        if (longitude != 0) tags.add("geo:lon=" + longitude);
        if (external_upload) {
            tags.add(TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD);
        } else {
            tags.add(TAG_SOURCE_ANDROID_RECORD);
            if (private_user_id > 0) {
                tags.add(TAG_RECORDING_TYPE_DEDICATED);
            }
        }
        return tags;
    }

    public boolean exists() {
        return audio_path.exists();
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
        Cursor cursor = resolver.query(Content.RECORDINGS.uri,
                null,
                Recordings.PRIVATE_USER_ID + " = ? AND " + Recordings.UPLOAD_STATUS + " = ?",
                new String[] { Long.toString(id), String.valueOf(Recording.Status.NOT_YET_UPLOADED) },
                null);

        try {
            return cursor != null && cursor.moveToFirst() ? new Recording(cursor) : null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public ContentValues buildContentValues(){
        ContentValues cv = super.buildContentValues();
        cv.put(Recordings.USER_ID, user_id);
        cv.put(Recordings.TIMESTAMP, lastModified());
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
        cv.put(Recordings.UPLOAD_STATUS, upload_status);
        return cv;
    }

    public static boolean isRawFilename(String filename) {
        return RAW_PATTERN.matcher(filename).matches();
    }

    public static boolean isEncodedFilename(String filename){
        return ENCODED_PATTERN.matcher(filename).matches();
    }

    public String sharingNote(Resources res) {
        String note;
        if (!TextUtils.isEmpty(what_text)) {
            if (!TextUtils.isEmpty(where_text)) {
                note = res.getString(R.string.recorded_at, what_text, where_text);
            } else {
                note = what_text;
            }
        } else {
            note = res.getString(R.string.sounds_from, !TextUtils.isEmpty(where_text) ? where_text :
                    recordingDateString(res));
        }
        return note;
    }

    public Uri toUri() {
        return Content.RECORDINGS.forId(id);
    }

    public String getStatus(Resources resources) {
        if (upload_status == Status.UPLOADING) {
            return resources.getString(R.string.cloud_upload_currently_uploading);
        } else {
            return CloudUtils.getTimeElapsed(resources, lastModified())
                    + ", "
                    + formattedDuration()
                    + ", "
                    + (upload_status == Status.ERROR ?
                    resources.getString(R.string.cloud_upload_upload_failed) :
                    resources.getString(R.string.cloud_upload_not_yet_uploaded));
        }
    }

    public String tagString() {
        return TextUtils.join(" ", getTags());
    }

    public String formattedDuration() {
        return CloudUtils.formatTimestamp(duration);
    }

    public boolean delete(ContentResolver resolver) {
        boolean deleted = false;
        if (!external_upload && audio_path.exists()) {
            deleted = audio_path.delete();
        }
        if (resolver != null) resolver.delete(toUri(), null, null);
        return deleted;
    }

    public static Recording fromIntent(Intent intent, ContentResolver resolver, long userId) {
        final String action = intent.getAction();

        if (intent.hasExtra(Intent.EXTRA_STREAM) &&
                (Intent.ACTION_SEND.equals(action) ||
                Actions.SHARE.equals(action) ||
                Actions.EDIT.equals(action))) {

            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            File file = IOUtils.getFromMediaUri(resolver, stream);
            if (file != null && file.exists()) {
                Recording r = new Recording(file);
                r.external_upload = true;
                r.user_id = userId;

                r.what_text = intent.getStringExtra(Actions.EXTRA_TITLE);
                r.where_text = intent.getStringExtra(Actions.EXTRA_WHERE);
                r.is_private = !intent.getBooleanExtra(Actions.EXTRA_PUBLIC, true);
                Location loc = intent.getParcelableExtra(Actions.EXTRA_LOCATION);
                if (loc != null) {
                    r.latitude = loc.getLatitude();
                    r.longitude = loc.getLongitude();
                }
                r.tags = intent.getStringArrayExtra(Actions.EXTRA_TAGS);
                r.description = intent.getStringExtra(Actions.EXTRA_DESCRIPTION);
                r.genre = intent.getStringExtra(Actions.EXTRA_GENRE);

                Uri artwork = intent.getParcelableExtra(Actions.EXTRA_ARTWORK);

                if (artwork != null && "file".equals(artwork.getScheme())) {
                    r.artwork_path = new File(artwork.getPath());
                }

                return r;
            }
        } else if (intent.getData() != null) {
            return Recording.fromUri(intent.getData(), resolver);
        }
        return null;
    }

    public boolean updateStatus(ContentResolver resolver) {
        ContentValues cv = new ContentValues();
        cv.put(Recordings.UPLOAD_STATUS, status);
        cv.put(Recordings.UPLOAD_ERROR, status == Recording.Status.NOT_YET_UPLOADED);
        if (audio_path != null) {
            cv.put(Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
        }
        return resolver.update(toUri(), cv, null, null) > 0;
    }

    private String recordingDateString(Resources res) {
        Time time = new Time();
        time.set(lastModified());
        final int id;
        if (time.hour <= 12) {
            id = R.string.recorded_morning;
        } else if (time.hour <= 17) {
            id = R.string.recorded_afternoon;
        } else if (time.hour <= 21) {
            id = R.string.recorded_evening;
        } else {
            id = R.string.recorded_night;
        }
        return res.getString(id, time.format("%A"));
    }

    public void upload(Context context) {
        context.startService(new Intent(Actions.UPLOAD).putExtra(UploadService.EXTRA_RECORDING, this));
    }


    public Map<String, ?> toParamsMap(Context context) {
        Map<String, Object> data = new HashMap<String, Object>();
        title = sharingNote(context.getResources());

        data.put(Params.Track.TITLE, title);
        data.put(Params.Track.TYPE, "recording");
        data.put(Params.Track.SHARING, is_private ? Params.Track.PRIVATE : Params.Track.PUBLIC);
        data.put(Params.Track.DOWNLOADABLE, false);
        data.put(Params.Track.STREAMABLE, true);

        if (!TextUtils.isEmpty(tagString())) data.put(Params.Track.TAG_LIST, tagString());
        if (!TextUtils.isEmpty(description)) data.put(Params.Track.DESCRIPTION, description);
        if (!TextUtils.isEmpty(genre))       data.put(Params.Track.GENRE, genre);

        if (!TextUtils.isEmpty(service_ids)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, service_ids.split(","));
            data.put(Params.Track.POST_TO, ids);
            data.put(Params.Track.SHARING_NOTE, title);
        } else {
            data.put(Params.Track.POST_TO_EMPTY, "");
        }

        if (!TextUtils.isEmpty(shared_emails)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, shared_emails.split(","));
            data.put(Params.Track.SHARED_EMAILS, ids);
        }

        if (private_user_id > 0) {
            data.put(Params.Track.SHARED_IDS, private_user_id);
        } else if (!TextUtils.isEmpty(shared_ids)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, shared_ids.split(","));
            data.put(Params.Track.SHARED_IDS, ids);
        }
        return data;
    }


    public Request getRequest(Context context, File file, Request.TransferProgressListener listener) {
        final Request request = new Request(Endpoints.TRACKS);
        final Map<String, ?> map = toParamsMap(context);
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Iterable) {
                for (Object o : (Iterable)entry.getValue()) {
                    request.add(entry.getKey(), o.toString());
                }
            } else {
                request.add(entry.getKey(), entry.getValue().toString());
            }
        }
        final String fileName;
        if (!external_upload) {
            String title = map.get(Params.Track.TITLE).toString();
            final String newTitle = title == null ? "unknown" : title;
            fileName = String.format("%s.%s", URLEncoder.encode(newTitle.replace(" ", "_")), "ogg");
        } else {
            fileName = file.getName();
        }
        return request.withFile(com.soundcloud.api.Params.Track.ASSET_DATA, file, fileName)
                .withFile(com.soundcloud.api.Params.Track.ARTWORK_DATA, artwork_path)
                .setProgressListener(listener);
    }

    public boolean isError() {
        return mUploadException != null;
    }

    /**
     * Gets called after successful upload. Clean any tmp files here.
     */
    public void onUploaded() {
        mSuccess = true;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public Recording setUploadException(Exception e) {
        mUploadException = e;
        mSuccess = false;
        return this;
    }

    public Exception getUploadException() {
        return mUploadException;
    }



    public boolean hasArtwork() {
        return artwork_path != null && artwork_path.exists();
    }

    @Override
    public String toString() {
        return "Recording{" +
                "id=" + id +
                ", user_id=" + user_id +
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
                ", upload_status=" + upload_status +
                ", tags=" + (tags == null ? null : Arrays.asList(tags)) +
                ", description='" + description + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }

    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };

    @Override public int compareTo(Recording recording) {
        return Long.valueOf(lastModified()).compareTo(recording.lastModified());
    }

    public Intent getIntent() {
        Intent intent;
        if (private_user_id > 0) {
            intent = new Intent(Actions.MESSAGE).putExtra("recipient", private_user_id);
        } else {
            intent = new Intent(Actions.RECORD);
        }
        return intent;
    }
}


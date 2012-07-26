
package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.audio.AudioReader;
import com.soundcloud.android.audio.PlaybackStream;
import com.soundcloud.android.audio.reader.EmptyReader;
import com.soundcloud.android.audio.reader.VorbisReader;
import com.soundcloud.android.audio.reader.WavReader;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.upload.UploadService;
import com.soundcloud.android.service.upload.UserCanceledException;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Recording extends ScModel implements Comparable<Recording> {

    public static final File IMAGE_DIR = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings/images");
    public static final String EXTRA = "recording";

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
    @NotNull private File audio_path;
    @Nullable public File artwork_path;
    @Nullable public File resized_artwork_path;

    // sharing
    public String four_square_venue_id; /* hex */
    public String shared_emails;
    public String shared_ids;
    public String service_ids;

    // private message to another user
    private User   recipient;
    /* package */ String recipient_username;
    /* package */ long   recipient_user_id;

    // status
    public boolean external_upload;
    public int upload_status;
    public long track_id = NOT_SET;

    private PlaybackStream mPlaybackStream;
    private Exception mUploadException;

    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm|wav)$");
    private static final Pattern ENCODED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    public static final String TAG_SOURCE_ANDROID_RECORD          = "soundcloud:source=android-record";
    public static final String TAG_RECORDING_TYPE_DEDICATED       = "soundcloud:recording-type=dedicated";
    public static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";

    public static interface Status {
        int NOT_YET_UPLOADED    = 0; // not yet uploaded, or canceled by user
        int UPLOADING           = 1; // currently uploading
        int UPLOADED            = 2; // successfully uploaded
        int ERROR               = 4; // network / api error
    }

    public Recording(File f) {
        this(f, null);
    }

    private Recording(File f, @Nullable User user) {
        if (f == null) throw new IllegalArgumentException("file is null");
        audio_path = f;
        if (user != null) {
            setRecipient(user);
        }
    }

    public Recording(Cursor c) {
        id = c.getLong(c.getColumnIndex(Recordings._ID));
        user_id = c.getLong(c.getColumnIndex(Recordings.USER_ID));
        longitude = c.getDouble(c.getColumnIndex(Recordings.LONGITUDE));
        latitude = c.getDouble(c.getColumnIndex(Recordings.LATITUDE));
        what_text = c.getString(c.getColumnIndex(Recordings.WHAT_TEXT));
        where_text = c.getString(c.getColumnIndex(Recordings.WHERE_TEXT));
        final String artwork = c.getString(c.getColumnIndex(Recordings.ARTWORK_PATH));
        artwork_path = artwork == null ? null : new File(artwork);
        final String audio = c.getString(c.getColumnIndex(Recordings.AUDIO_PATH));
        if (audio == null) throw new IllegalArgumentException("audio is null");
        audio_path = new File(audio);
        duration = c.getLong(c.getColumnIndex(Recordings.DURATION));
        description = c.getString(c.getColumnIndex(Recordings.DESCRIPTION));
        four_square_venue_id = c.getString(c.getColumnIndex(Recordings.FOUR_SQUARE_VENUE_ID));
        shared_emails = c.getString(c.getColumnIndex(Recordings.SHARED_EMAILS));
        shared_ids = c.getString(c.getColumnIndex(Recordings.SHARED_IDS));
        recipient_user_id = c.getLong(c.getColumnIndex(Recordings.PRIVATE_USER_ID));
        int usernameIdx = c.getColumnIndex(DBHelper.Users.USERNAME);
        if (usernameIdx != -1) { // gets joined in
            recipient_username = c.getString(usernameIdx);
        }
        service_ids = c.getString(c.getColumnIndex(Recordings.SERVICE_IDS));
        is_private = c.getInt(c.getColumnIndex(Recordings.IS_PRIVATE)) == 1;
        external_upload = c.getInt(c.getColumnIndex(Recordings.EXTERNAL_UPLOAD)) == 1;
        upload_status = c.getInt(c.getColumnIndex(Recordings.UPLOAD_STATUS));
        if (!external_upload) mPlaybackStream = initializePlaybackStream(c);
    }

    public File getFile() {
        return audio_path;
    }

    public File getEncodedFile() {
        return IOUtils.changeExtension(audio_path, VorbisReader.EXTENSION);
    }

    public File getProcessedFile() {
        return IOUtils.appendToFilename(getEncodedFile(), "_processed");
    }

    public File getAmplitudeFile() {
        return IOUtils.changeExtension(audio_path, "amp");
    }

    /**
     * @return the file to upload, or null. this will select a processed file if there is one.
     */
    public @Nullable File getUploadFile() {
        return getProcessedFile().exists() ? getProcessedFile() :
                (getEncodedFile().exists() ? getEncodedFile() :
                (getFile().exists() ? getFile() : null));
    }

    public User getRecipient()           { return recipient; }
    public String getRecipientUsername() { return recipient_username; }

    public PlaybackStream getPlaybackStream() {
        if (mPlaybackStream == null && !external_upload) {
            mPlaybackStream = initializePlaybackStream(null);
        }
        return mPlaybackStream;
    }

    public File generateImageFile(File imageDir) {
        if (audio_path.getName().contains(".")) {
            return new File(imageDir, audio_path.getName().substring(0, audio_path.getName().lastIndexOf(".")) + ".bmp");
        } else {
            return new File(imageDir, audio_path.getName()+".bmp");
        }
    }

    public long lastModified() {
        return audio_path.lastModified();
    }

    public String getAbsolutePath() {
        return audio_path.getAbsolutePath();
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
            if (recipient_user_id > 0) {
                tags.add(TAG_RECORDING_TYPE_DEDICATED);
            }
        }
        return tags;
    }

    public boolean exists() {
        return audio_path.exists();
    }

    private void setRecipient(User recipient) {
        this.recipient     = recipient;
        recipient_user_id  = recipient.id;
        recipient_username = recipient.getDisplayName();
        is_private = true;
    }

    public ContentValues buildContentValues(){
        ContentValues cv = super.buildContentValues();
        cv.put(Recordings.USER_ID, user_id > 0 ? user_id : SoundCloudApplication.getUserId());
        cv.put(Recordings.TIMESTAMP, lastModified());
        cv.put(Recordings.LONGITUDE, longitude);
        cv.put(Recordings.LATITUDE, latitude);
        cv.put(Recordings.WHAT_TEXT, what_text);
        cv.put(Recordings.WHERE_TEXT, where_text);
        cv.put(Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
        cv.put(Recordings.DURATION, duration);
        cv.put(Recordings.DESCRIPTION, description);
        if (artwork_path != null) cv.put(Recordings.ARTWORK_PATH, artwork_path.getAbsolutePath());
        cv.put(Recordings.FOUR_SQUARE_VENUE_ID, four_square_venue_id);
        cv.put(Recordings.SHARED_EMAILS, shared_emails);
        cv.put(Recordings.SHARED_IDS, shared_ids);
        cv.put(Recordings.PRIVATE_USER_ID, recipient_user_id);
        cv.put(Recordings.SERVICE_IDS, service_ids);
        cv.put(Recordings.IS_PRIVATE, is_private);
        cv.put(Recordings.EXTERNAL_UPLOAD, external_upload);
        cv.put(Recordings.UPLOAD_STATUS, upload_status);

        if (mPlaybackStream != null) {
            cv.put(DBHelper.Recordings.TRIM_LEFT,  mPlaybackStream.getStartPos());
            cv.put(DBHelper.Recordings.TRIM_RIGHT, mPlaybackStream.getEndPos());
            cv.put(DBHelper.Recordings.OPTIMIZE,   mPlaybackStream.isOptimized() ? 1 : 0);
            cv.put(DBHelper.Recordings.FADING,     mPlaybackStream.isFading() ? 1 : 0);
        }
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
        return id > 0 ? Content.RECORDING.forId(id) : Content.RECORDINGS.uri;
    }

    public String getStatus(Resources resources) {
        if (upload_status == Status.UPLOADING) {
            return resources.getString(R.string.cloud_upload_currently_uploading);
        } else {
            return ScTextUtils.getTimeElapsed(resources, lastModified())
                    + ", "
                    + formattedDuration()
                    + ", "
                    + (upload_status == Status.ERROR ?
                    resources.getString(R.string.cloud_upload_upload_failed) :
                    resources.getString(R.string.cloud_upload_not_yet_uploaded));
        }
    }

    /**
     * @return space separated string containing all tags, or empty string
     */
    public String tagString() {
        return TextUtils.join(" ", getTags());
    }

    public String formattedDuration() {
        return ScTextUtils.formatTimestamp(duration);
    }

    public boolean delete(@Nullable ContentResolver resolver) {
        boolean deleted = false;
        if (!external_upload) {
            deleted = IOUtils.deleteFile(audio_path);
        }
        IOUtils.deleteFile(getEncodedFile());
        IOUtils.deleteFile(getAmplitudeFile());
        if (id > 0 && resolver != null) resolver.delete(toUri(), null, null);
        return deleted;
    }

    public boolean updateStatus(ContentResolver resolver) {
        if (id > 0) {
            ContentValues cv = new ContentValues();
            cv.put(Recordings.UPLOAD_STATUS, upload_status);
            cv.put(Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
            return resolver.update(toUri(), cv, null, null) > 0;
        } else {
            return false;
        }
    }

    public void record(Context context) {
        context.startService(getRecordIntent());
    }

    public void upload(Context context) {
        context.startService(getUploadIntent());
    }

    public void cancelUpload(Context context) {
        context.startService(getCancelIntent());
    }

    public Intent getMonitorIntent() {
        return new Intent(Actions.UPLOAD_MONITOR).putExtra(UploadService.EXTRA_RECORDING, this);
    }

    public Intent getProcessIntent() {
        return new Intent(Actions.RECORDING_PROCESS)
                .setData(Uri.fromFile(getFile()))
                .putExtra("com.soundcloud.android.pd.extra.out",
                        getFile().getAbsolutePath()+"-processed.wav");
    }

    public Intent getViewIntent() {
        if (recipient_user_id > 0) {
            return new Intent(Actions.MESSAGE).putExtra("recipient", recipient_user_id);
        } else {
            return new Intent(Actions.RECORD);
        }
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

        if (recipient_user_id > 0) {
            data.put(Params.Track.SHARED_IDS, recipient_user_id);
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
            fileName = String.format("%s.%s", URLEncoder.encode(newTitle.replace(" ", "_")), VorbisReader.EXTENSION);
        } else {
            fileName = file.getName();
        }
        return request.withFile(com.soundcloud.api.Params.Track.ASSET_DATA, file, fileName)
                .withFile(com.soundcloud.api.Params.Track.ARTWORK_DATA, artwork_path)
                .setProgressListener(listener);
    }

    public boolean isError() {
        return upload_status == Status.ERROR;
    }

    /**
     * Gets called after successful upload. Clean any tmp files here.
     */
    public void onUploaded() {
        upload_status = Status.UPLOADED;
        IOUtils.deleteFile(getEncodedFile());
        IOUtils.deleteFile(getFile());
        IOUtils.deleteFile(resized_artwork_path);
    }

    public boolean isUploaded() {
        return upload_status == Status.UPLOADED;
    }

    public boolean isUploading() {
        return upload_status == Status.UPLOADING;
    }

    public boolean isCanceled() {
        return mUploadException instanceof UserCanceledException;
    }

    public Recording setUploadException(Exception e) {
        mUploadException = e;
        upload_status = e instanceof UserCanceledException ? Status.NOT_YET_UPLOADED : Status.ERROR;
        return this;
    }

    public void setPlaybackStream(PlaybackStream stream) {
        duration = stream == null ? 0 : stream.getDuration();
        mPlaybackStream = stream;
    }

    public Exception getUploadException() {
        return mUploadException;
    }

    public boolean hasArtwork() {
        return artwork_path != null && artwork_path.exists();
    }

    public File getArtwork() {
        return resized_artwork_path != null && resized_artwork_path.exists() ? resized_artwork_path : artwork_path;
    }

    public boolean isPrivateMessage() {
        return recipient_user_id > 0;
    }

    public boolean needsMigration() {
        if (audio_path != null && external_upload != true) {
            final DeprecatedProfile profile = DeprecatedProfile.getProfile(audio_path);
            return (profile != DeprecatedProfile.UNKNOWN);
        }
        return false;
    }

    /**
     * Rename files, return CV for a bulk insert
     */
    public ContentValues migrate() {
        final DeprecatedProfile profile = DeprecatedProfile.getProfile(audio_path);
        if (profile != DeprecatedProfile.UNKNOWN) {
            final File newPath = IOUtils.changeExtension(audio_path, profile.updatedExtension);
            final long lastMod = audio_path.lastModified();
            if (audio_path.renameTo(newPath)){
                newPath.setLastModified(lastMod);
                audio_path = newPath;
                ContentValues cv = new ContentValues();
                cv.put(Recordings._ID, id);
                cv.put(Recordings.EXTERNAL_UPLOAD, true);
                cv.put(Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
                return cv;
            }
        }
        return null;
    }

    @Override
    public int compareTo(Recording recording) {
        return Long.valueOf(lastModified()).compareTo(recording.lastModified());
    }

    public static Recording checkForUnusedPrivateRecording(File directory, User user) {
        if (user == null) return null;
        for (File f : IOUtils.nullSafeListFiles(directory, new RecordingFilter(null))) {
            if (Recording.getUserIdFromFile(f) == user.id) {
                Recording r = new Recording(f);
                r.recipient = user;
                return r;
            }
        }
        return null;
    }

    public static List<Recording> getUnsavedRecordings(ContentResolver resolver, File directory, Recording ignore, long userId) {
        MediaPlayer mp = null;
        List<Recording> unsaved = new ArrayList<Recording>();
        for (File f : IOUtils.nullSafeListFiles(directory, new RecordingFilter(ignore))) {
            if (getUserIdFromFile(f) != -1) continue; // ignore current file
            Recording r = SoundCloudDB.getRecordingByPath(resolver, f);
            if (r == null) {
                r = new Recording(f);
                r.user_id = userId;
                try {
                    if (mp == null) {
                        mp = new MediaPlayer();
                    }
                    mp.reset();
                    mp.setDataSource(f.getAbsolutePath());
                    mp.prepare();
                    r.duration = mp.getDuration();
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                }
                unsaved.add(r);
            }
        }
        Collections.sort(unsaved, null);
        return unsaved;
    }

    public static class RecordingFilter implements FilenameFilter {
        private Recording toIgnore;

        public RecordingFilter(@Nullable Recording ignore) {
            toIgnore = ignore;
        }

        @Override
        public boolean accept(File file, String name) {
            return Recording.isRawFilename(name) || Recording.isEncodedFilename(name) &&
                    (toIgnore == null || !toIgnore.audio_path.equals(file));
        }
    }

    public static long getUserIdFromFile(File file) {
        final String path = file.getName();
        if (TextUtils.isEmpty(path) || !path.contains("_") || path.indexOf("_") + 1 >= path.length()) {
            return -1;
        } else try {
            return Long.valueOf(
                    path.substring(path.indexOf('_') + 1,
                            path.contains(".") ? path.indexOf('.') : path.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static Recording fromIntent(Intent intent, ContentResolver resolver, long userId) {
        final String action = intent.getAction();

        if (intent.hasExtra(EXTRA))  {
            return intent.getParcelableExtra(EXTRA);
            // 3rd party sharing?
        } else if (intent.hasExtra(Intent.EXTRA_STREAM) &&
                (Intent.ACTION_SEND.equals(action) ||
                        Actions.SHARE.equals(action)   ||
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
            } else return null;
        } else if (intent.getData() != null) {
            return Recording.fromUri(intent.getData(), resolver);
        } else {
            return null;
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

    public static @NotNull Recording create() {
        return create(null);
    }

    /**
     * @param user the user this recording is for, or null if there's no recipient
     * @return a recording initialised with a file path (which will be used for the recording).
     */
    public static @NotNull Recording create(@Nullable User user) {
        File file = new File(SoundRecorder.RECORD_DIR,
                System.currentTimeMillis()
                + (user == null ? "" : "_" + user.id)
                + "."+WavReader.EXTENSION);
        return new Recording(file, user);
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

    public Recording(Parcel in) {

        Bundle data = in.readBundle(getClass().getClassLoader());
        id = data.getLong("id");
        user_id = data.getLong("user_id");
        longitude = data.getDouble("longitude");
        latitude = data.getDouble("latitude");
        what_text = data.getString("what_text");
        where_text = data.getString("where_text");
        description = data.getString("description");
        genre = data.getString("genre");
        audio_path = new File(data.getString("audio_path"));
        if (data.containsKey("artwork_path")) {
            artwork_path = new File(data.getString("artwork_path"));
        }
        if (data.containsKey("resized_artwork_path")) {
            resized_artwork_path = new File(data.getString("resized_artwork_path"));
        }
        duration = data.getLong("duration");
        four_square_venue_id = data.getString("four_square_venue_id");
        shared_emails = data.getString("shared_emails");
        shared_ids = data.getString("shared_ids");
        recipient_user_id = data.getLong("recipient_user_id");
        recipient_username = data.getString("recipient_username");
        service_ids = data.getString("service_ids");
        is_private = data.getBoolean("is_private", false);
        external_upload = data.getBoolean("external_upload", false);
        upload_status = data.getInt("upload_status");
        if (!external_upload) mPlaybackStream = data.getParcelable("playback_stream");
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        Bundle data = new Bundle();
        data.putLong("id", id);
        data.putLong("user_id", user_id);
        data.putDouble("longitude", longitude);
        data.putDouble("latitude", latitude);
        data.putString("what_text", what_text);
        data.putString("where_text", where_text);
        data.putString("audio_path", audio_path.getAbsolutePath());
        if (artwork_path != null) {
            data.putString("artwork_path", artwork_path.getAbsolutePath());
        }
        if (resized_artwork_path != null) {
            data.putString("resized_artwork_path", resized_artwork_path.getAbsolutePath());
        }
        data.putLong("duration", duration);
        data.putString("four_square_venue_id", four_square_venue_id);
        data.putString("shared_emails", shared_emails);
        data.putString("shared_ids", shared_ids);
        data.putString("description", description);
        data.putString("genre", genre);
        data.putLong("recipient_user_id", recipient_user_id);
        data.putString("recipient_username", recipient_username);
        data.putString("service_ids", service_ids);
        data.putBoolean("is_private", is_private);
        data.putBoolean("external_upload", external_upload);
        data.putInt("upload_status", upload_status);
        if (!external_upload) data.putParcelable("playback_stream", mPlaybackStream);
        out.writeBundle(data);
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

    private Intent getRecordIntent() {
        return new Intent(Actions.RECORD).putExtra(UploadService.EXTRA_RECORDING, this);
    }

    private Intent getUploadIntent() {
        return new Intent(Actions.UPLOAD).putExtra(SoundRecorder.EXTRA_RECORDING, this);
    }

    private Intent getCancelIntent() {
        return new Intent(Actions.UPLOAD_CANCEL).putExtra(SoundRecorder.EXTRA_RECORDING, this);
    }

    private PlaybackStream initializePlaybackStream(@Nullable Cursor c) {
        try {
            final AudioReader reader = AudioReader.guess(audio_path);
            PlaybackStream stream = new PlaybackStream(reader);
            if (c != null) {
                long startPos = c.getLong(c.getColumnIndex(Recordings.TRIM_LEFT));
                long endPos   = c.getLong(c.getColumnIndex(Recordings.TRIM_RIGHT));
                boolean optimize  = c.getInt(c.getColumnIndex(Recordings.OPTIMIZE)) == 1;
                boolean fade  = c.getInt(c.getColumnIndex(Recordings.FADING)) == 1;

                stream.setFading(fade);
                stream.setOptimize(optimize);
                stream.setTrim(startPos, endPos);
            }
            return stream;
        } catch (IOException e) {
            Log.w(TAG, "could not initialize playback stream", e);
            return new PlaybackStream(new EmptyReader());
        }
    }

    public static boolean migrateRecordings(List<Recording> recordings, final ContentResolver resolver) {
        final List<Recording> migrate = new ArrayList<Recording>();
        for (Recording r : recordings) {
            if (r.needsMigration()) migrate.add(r);
        }

        if (migrate.size() > 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(SoundCloudApplication.TAG,"Deprecated recordings found, trying to migrate " + migrate.size() + " recordings");
                    ContentValues[] cv = new ContentValues[migrate.size()];
                    int i = 0;
                    for (Recording r : migrate) {
                        cv[i] = r.migrate();
                        i++;
                    }
                    int updated = resolver.bulkInsert(Content.RECORDINGS.uri, cv);
                    Log.i(SoundCloudApplication.TAG,"Finished migrating " + updated + " recordings");
                }
            }).start();
            return true;
        } else {
            return false;
        }

    }

    static enum DeprecatedProfile {

        UNKNOWN(-1,null),
        ENCODED_LOW(0,"ogg"),
        ENCODED_HIGH(1,"ogg"),
        RAW(2,"wav");

        int id;
        String updatedExtension;

        DeprecatedProfile(int id, String updatedExtension){
            this.id = id;
            this.updatedExtension = updatedExtension;
        }

        public static DeprecatedProfile getProfile(File f) {
            if (f != null) {
                try {
                    final int profile = Integer.parseInt(IOUtils.extension(f));
                    for (DeprecatedProfile p : DeprecatedProfile.values()) {
                        if (p.id == profile) return p;
                    }
                } catch (NumberFormatException e) {
                }
            }
            return UNKNOWN;
        }

        public String getExtension() {
            return "." + id;
        }
    }

}


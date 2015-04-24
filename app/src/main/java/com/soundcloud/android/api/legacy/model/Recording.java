package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.creators.record.AmplitudeData;
import com.soundcloud.android.creators.record.AudioReader;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.storage.RecordingStorage;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.FiletimeComparator;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.api.Params;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Recording extends PublicApiResource implements Comparable<Recording> {

    public static final File IMAGE_DIR = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings/images");
    public static final String EXTRA = "recording";
    public static final int MAX_WAVE_CACHE = 100 * 1024 * 1024; // 100 mb
    public static final String UPLOAD_TYPE = "recording";
    public static final String TAG_SOURCE_ANDROID_RECORD = "soundcloud:source=android-record";
    public static final String TAG_RECORDING_TYPE_DEDICATED = "soundcloud:recording-type=dedicated";
    public static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";
    public static final String PROCESSED_APPEND = "_processed";
    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };
    private static final Pattern AMPLITUDE_PATTERN = Pattern.compile("^.*\\.(amp)$");
    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm|wav)$");
    private static final Pattern ENCODED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");
    // basic properties
    public long user_id;
    public String title;
    public String what_text;
    public String where_text;
    public long duration; // in msecs
    public boolean is_private;
    public String[] tags;
    public String description, genre;
    @Deprecated public double longitude;
    @Deprecated public double latitude;
    public String tip;
    // assets
    @NotNull
    public File audio_path;
    @Nullable public File artwork_path;
    @Nullable public File resized_artwork_path;
    // sharing
    @Deprecated public String four_square_venue_id; /* hex */
    public String service_ids;
    @Deprecated public String shared_emails;
    @Deprecated public String shared_ids;
    @Deprecated public String recipient_username;
    @Deprecated public long recipient_user_id;
    // status
    public boolean external_upload;
    public int upload_status;
    // private message to another user
    private PlaybackStream playbackStream;

    public Recording() {
        // No-op constr for the Blueprint
    }

    public Recording(@NotNull File f) {
        this(f, null);
    }

    private Recording(@NotNull File f, @Nullable String tip) {
        audio_path = f;

        if (!TextUtils.isEmpty(tip)) {
            this.tip = tip;
        }
    }

    public Recording(Cursor c) {
        setId(c.getLong(c.getColumnIndex(TableColumns.Recordings._ID)));
        user_id = c.getLong(c.getColumnIndex(TableColumns.Recordings.USER_ID));
        longitude = c.getDouble(c.getColumnIndex(TableColumns.Recordings.LONGITUDE));
        latitude = c.getDouble(c.getColumnIndex(TableColumns.Recordings.LATITUDE));
        what_text = c.getString(c.getColumnIndex(TableColumns.Recordings.WHAT_TEXT));
        where_text = c.getString(c.getColumnIndex(TableColumns.Recordings.WHERE_TEXT));
        final String artwork = c.getString(c.getColumnIndex(TableColumns.Recordings.ARTWORK_PATH));
        artwork_path = TextUtils.isEmpty(artwork) ? null : new File(artwork);
        final String audio = c.getString(c.getColumnIndex(TableColumns.Recordings.AUDIO_PATH));
        if (audio == null) {
            throw new IllegalArgumentException("audio is null");
        }
        audio_path = new File(audio);
        duration = c.getLong(c.getColumnIndex(TableColumns.Recordings.DURATION));
        description = c.getString(c.getColumnIndex(TableColumns.Recordings.DESCRIPTION));
        four_square_venue_id = c.getString(c.getColumnIndex(TableColumns.Recordings.FOUR_SQUARE_VENUE_ID));
        shared_emails = c.getString(c.getColumnIndex(TableColumns.Recordings.SHARED_EMAILS));
        shared_ids = c.getString(c.getColumnIndex(TableColumns.Recordings.SHARED_IDS));
        recipient_user_id = c.getLong(c.getColumnIndex(TableColumns.Recordings.PRIVATE_USER_ID));
        int usernameIdx = c.getColumnIndex(TableColumns.Users.USERNAME);
        if (usernameIdx != -1) { // gets joined in
            recipient_username = c.getString(usernameIdx);
        }
        tip = c.getString(c.getColumnIndex(TableColumns.Recordings.TIP_KEY));
        service_ids = c.getString(c.getColumnIndex(TableColumns.Recordings.SERVICE_IDS));
        is_private = c.getInt(c.getColumnIndex(TableColumns.Recordings.IS_PRIVATE)) == 1;
        external_upload = c.getInt(c.getColumnIndex(TableColumns.Recordings.EXTERNAL_UPLOAD)) == 1;
        upload_status = c.getInt(c.getColumnIndex(TableColumns.Recordings.UPLOAD_STATUS));
        if (!external_upload) {
            playbackStream = initializePlaybackStream(c);
        }
    }

    public Recording(Parcel in) {

        Bundle data = in.readBundle(getClass().getClassLoader());
        setId(data.getLong("id"));
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
        tip = data.getString("tip_key");
        service_ids = data.getString("service_ids");
        is_private = data.getBoolean("is_private", false);
        external_upload = data.getBoolean("external_upload", false);
        upload_status = data.getInt("upload_status");
        if (!external_upload) {
            playbackStream = data.getParcelable("playback_stream");
        }
    }

    public String getTitle(Resources r) {
        return TextUtils.isEmpty(title) ? sharingNote(r) : title;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.RECORDINGS.uri;
    }

    public File getFile() {
        return audio_path;
    }

    public
    @Nullable
    File getRawFile() {
        return isRawFilename(audio_path.getName()) ? audio_path : null;
    }

    public File getEncodedFile() {
        return IOUtils.changeExtension(audio_path, VorbisReader.EXTENSION);
    }

    public File getProcessedFile() {
        return IOUtils.appendToFilename(getEncodedFile(), PROCESSED_APPEND);
    }

    public File getAmplitudeFile() {
        return IOUtils.changeExtension(audio_path, AmplitudeData.EXTENSION);
    }

    /**
     * @return the file to upload, or null. this will select a processed file if there is one.
     */
    public
    @Nullable
    File getUploadFile() {
        return getProcessedFile().exists() ? getProcessedFile() :
                (getEncodedFile().exists() ? getEncodedFile() :
                        (getFile().exists() ? getFile() : null));
    }

    public
    @Nullable
    PlaybackStream getPlaybackStream() {
        if (playbackStream == null && !external_upload) {
            playbackStream = initializePlaybackStream(null);
        }
        return playbackStream;
    }

    public void setPlaybackStream(PlaybackStream stream) {
        duration = stream == null ? 0 : stream.getDuration();
        playbackStream = stream;
    }

    public File generateImageFile(File imageDir) {
        return new File(imageDir, IOUtils.changeExtension(audio_path, "bmp").getName());
    }

    /**
     * @return last modified time of recording, or 0 if file does not exist.
     */
    public long lastModified() {
        return audio_path.exists() ? audio_path.lastModified() : getEncodedFile().lastModified();
    }

    public String getAbsolutePath() {
        return audio_path.getAbsolutePath();
    }

    public List<String> getTags() {
        // add machine tags
        List<String> tags = new ArrayList<>();
        if (this.tags != null) {
            for (String t : this.tags) {
                tags.add(t.contains(" ") ? "\"" + t + "\"" : t);
            }
        }
        if (!TextUtils.isEmpty(four_square_venue_id)) {
            tags.add("foursquare:venue=" + four_square_venue_id);
        }
        if (latitude != 0) {
            tags.add("geo:lat=" + latitude);
        }
        if (longitude != 0) {
            tags.add("geo:lon=" + longitude);
        }
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
        return audio_path.exists() || getEncodedFile().exists();
    }

    public ContentValues buildContentValues() {
        ContentValues cv = buildBaseContentValues();
        cv.put(TableColumns.Recordings.LONGITUDE, longitude);
        cv.put(TableColumns.Recordings.LATITUDE, latitude);
        cv.put(TableColumns.Recordings.WHAT_TEXT, what_text);
        cv.put(TableColumns.Recordings.WHERE_TEXT, where_text);
        cv.put(TableColumns.Recordings.DESCRIPTION, description);
        cv.put(TableColumns.Recordings.ARTWORK_PATH, artwork_path == null ? "" : artwork_path.getAbsolutePath());
        cv.put(TableColumns.Recordings.FOUR_SQUARE_VENUE_ID, four_square_venue_id);
        cv.put(TableColumns.Recordings.SHARED_EMAILS, shared_emails);
        cv.put(TableColumns.Recordings.SHARED_IDS, shared_ids);
        cv.put(TableColumns.Recordings.SERVICE_IDS, service_ids);
        cv.put(TableColumns.Recordings.IS_PRIVATE, is_private);
        return cv;
    }

    public ContentValues buildBaseContentValues() {
        ContentValues cv = super.buildContentValues();
        addBaseContentValues(cv);
        return cv;
    }

    public static boolean isAmplitudeFile(String filename) {
        return AMPLITUDE_PATTERN.matcher(filename).matches();
    }

    public static boolean isRawFilename(String filename) {
        return RAW_PATTERN.matcher(filename).matches();
    }

    public static boolean isEncodedFilename(String filename) {
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
        } else if (external_upload && !isLegacyRecording()) {
            note = audio_path.getName();
        } else {
            note = res.getString(R.string.sounds_from, !TextUtils.isEmpty(where_text) ? where_text :
                    recordingDateString(res));
        }
        return note;
    }

    public Uri toUri() {
        return getId() > 0 ? Content.RECORDING.forId(getId()) : getBulkInsertUri();
    }

    public boolean isLegacyRecording() {
        return (external_upload && audio_path.getParentFile().equals(SoundRecorder.RECORD_DIR));
    }

    public String getStatusMessage(Resources resources) {
        switch (upload_status) {
            case Status.UPLOADING:
                return resources.getString(R.string.recording_uploading);
            case Status.ERROR:
                return resources.getString(R.string.recording_upload_failed);
            case Status.NOT_YET_UPLOADED:
            default:
                return resources.getString(R.string.recording_pending_upload);
        }
    }

    /**
     * @return space separated string containing all tags, or empty string
     */
    public String tagString() {
        return TextUtils.join(" ", getTags());
    }

    public String formattedDuration() {
        return ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS);
    }

    public Intent getMonitorIntent() {
        return new Intent(Actions.UPLOAD_MONITOR).setData(toUri());
    }

    public Intent getMonitorIntentWithProgress(int uploadStage, int progress) {
        return getMonitorIntent().putExtra(UploadService.EXTRA_STAGE, uploadStage).putExtra(UploadService.EXTRA_PROGRESS, progress);
    }

    public Intent getViewIntent() {
        return new Intent(Actions.RECORD);
    }

    public Map<String, ?> toParamsMap(Resources resources) {
        Map<String, Object> data = new HashMap<>();
        title = sharingNote(resources);

        data.put(Params.Track.TITLE, title);
        data.put(Params.Track.TYPE, UPLOAD_TYPE);
        data.put(Params.Track.SHARING, isPublic() ? Params.Track.PUBLIC : Params.Track.PRIVATE);
        data.put(Params.Track.DOWNLOADABLE, false);
        data.put(Params.Track.STREAMABLE, true);

        if (!TextUtils.isEmpty(tagString())) {
            data.put(Params.Track.TAG_LIST, tagString());
        }
        if (!TextUtils.isEmpty(description)) {
            data.put(Params.Track.DESCRIPTION, description);
        }
        if (!TextUtils.isEmpty(genre)) {
            data.put(Params.Track.GENRE, genre);
        }

        if (!TextUtils.isEmpty(service_ids)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, service_ids.split(","));
            data.put(Params.Track.POST_TO, ids);
            data.put(Params.Track.SHARING_NOTE, title);
        } else {
            data.put(Params.Track.POST_TO_EMPTY, "");
        }

        if (!TextUtils.isEmpty(shared_emails)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, shared_emails.split(","));
            data.put(Params.Track.SHARED_EMAILS, ids);
        }

        if (recipient_user_id > 0) {
            data.put(Params.Track.SHARED_IDS, recipient_user_id);
        } else if (!TextUtils.isEmpty(shared_ids)) {
            List<String> ids = new ArrayList<>();
            Collections.addAll(ids, shared_ids.split(","));
            data.put(Params.Track.SHARED_IDS, ids);
        }
        return data;
    }

    public boolean isError() {
        return upload_status == Status.ERROR;
    }

    /**
     * Need to re-encode if fading/optimize is enabled, or no encoding happened during recording
     */
    public boolean needsEncoding() {
        if (external_upload) {
            return false;
        }
        PlaybackStream stream = getPlaybackStream();
        if (stream != null && stream.isFiltered()) {
            return !getProcessedFile().exists();
        } else {
            return !getEncodedFile().exists();
        }
    }

    public boolean needsProcessing() {
        PlaybackStream stream = getPlaybackStream();
        return !needsEncoding()
                && stream != null
                && stream.isTrimmed()
                && (!getProcessedFile().exists() || getProcessedFile().length() == 0);
    }

    public boolean needsResizing() {
        return hasArtwork() && !hasResizedArtwork();
    }

    public boolean isUploaded() {
        return upload_status == Status.UPLOADED;
    }

    public boolean isUploading() {
        return upload_status == Status.UPLOADING;
    }

    public Recording setUploadFailed(boolean cancelled) {
        upload_status = cancelled ? Status.NOT_YET_UPLOADED : Status.ERROR;
        return this;
    }

    public boolean hasArtwork() {
        return artwork_path != null && artwork_path.exists();
    }

    public boolean hasResizedArtwork() {
        return resized_artwork_path != null && resized_artwork_path.exists();
    }

    public File getArtwork() {
        return resized_artwork_path != null && resized_artwork_path.exists() ? resized_artwork_path : artwork_path;
    }

    @Override
    public int compareTo(@NotNull Recording recording) {
        return Long.valueOf(lastModified()).compareTo(recording.lastModified());
    }

    public static long getUserIdFromFile(File file) {
        final String path = file.getName();
        if (TextUtils.isEmpty(path) || !path.contains("_") || path.indexOf('_') + 1 >= path.length()) {
            return -1;
        } else {
            try {
                return Long.valueOf(
                        path.substring(path.indexOf('_') + 1,
                                path.contains(".") ? path.indexOf('.') : path.length()));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
    }

    // TODO , not sure where this belongs
    // Yeah, because it's absolutely fucking terrible.
    public static
    @Nullable
    Recording fromIntent(@Nullable Intent intent, Context context, long userId) {
        if (intent == null) {
            return null;
        }
        final String action = intent.getAction();

        if (intent.hasExtra(EXTRA)) {
            return intent.getParcelableExtra(EXTRA);
            // 3rd party sharing?
        } else if (intent.hasExtra(Intent.EXTRA_STREAM) &&
                (Intent.ACTION_SEND.equals(action) ||
                        Actions.SHARE.equals(action) ||
                        Actions.EDIT.equals(action))) {

            Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            File file = IOUtils.getFromMediaUri(context.getContentResolver(), stream);
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
            } else {
                return null;
            }
        } else if (intent.getData() != null) {
            RecordingStorage recordings = new RecordingStorage();
            return recordings.getRecordingByUri(intent.getData());
        } else {
            return null;
        }
    }

    public void markUploaded() {
        upload_status = Status.UPLOADED;
    }

    public static void clearRecordingFromIntent(Intent intent) {
        intent.removeExtra(EXTRA);
        intent.removeExtra(Intent.EXTRA_STREAM);
        intent.setData(null);
    }

    /**
     * @param tip the name for the recording file
     * @return a recording initialised with a file path (which will be used for the recording).
     */
    public static
    @NotNull
    Recording create(@Nullable String tip) {
        File file = new File(SoundRecorder.RECORD_DIR,
                System.currentTimeMillis()
                        + "." + WavReader.EXTENSION);

        return new Recording(file, tip);
    }

    @Override
    public String toString() {
        return "Recording{" +
                "id=" + getId() +
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
                ", tip='" + tip + '\'' +
                ", is_private=" + is_private +
                ", external_upload=" + external_upload +
                ", upload_status=" + upload_status +
                ", tags=" + (tags == null ? null : Arrays.asList(tags)) +
                ", description='" + description + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        Bundle data = new Bundle();
        data.putLong("id", getId());
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
        data.putString("tip_key", tip);
        data.putString("service_ids", service_ids);
        data.putBoolean("is_private", is_private);
        data.putBoolean("external_upload", external_upload);
        data.putInt("upload_status", upload_status);
        if (!external_upload) {
            data.putParcelable("playback_stream", playbackStream);
        }
        out.writeBundle(data);
    }

    public static long trimWaveFiles(File directory, Recording ignore) {
        return trimWaveFiles(directory, ignore, MAX_WAVE_CACHE);
    }

    public static long trimWaveFiles(File directory, Recording ignore, long maxCacheSize) {
        final RecordingWavFilter filter = new RecordingWavFilter(ignore);
        final long dirSize = IOUtils.getDirSize(directory);

        long trimmed = 0;
        final long toTrim = Math.max(0, dirSize - maxCacheSize);

        if (toTrim > 0) {
            final File[] list = IOUtils.nullSafeListFiles(directory, filter);
            if (list.length > 0) {
                Arrays.sort(list, new FiletimeComparator(true));
                int i = 0;
                while (trimmed < toTrim && i < list.length) {
                    trimmed += list[i].length();
                    list[i].delete();
                    i++;
                }
            }
        }
        return trimmed;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    // Model citizen requires this name...
    public boolean getIsPrivate() {
        return is_private;
    }

    // Model citizen requires this name...
    public void setIsPrivate(boolean isPrivate) {
        this.is_private = isPrivate;
    }

    public long getUserId() {
        return user_id;
    }

    public void setUserId(long userId) {
        this.user_id = userId;
    }

    private void addBaseContentValues(ContentValues cv) {
        final long currentUserId = SoundCloudApplication.instance.getAccountOperations().getLoggedInUserId();
        cv.put(TableColumns.Recordings.USER_ID, user_id > 0 ? user_id : currentUserId);
        cv.put(TableColumns.Recordings.AUDIO_PATH, audio_path.getAbsolutePath());
        cv.put(TableColumns.Recordings.PRIVATE_USER_ID, recipient_user_id);
        cv.put(TableColumns.Recordings.TIMESTAMP, lastModified());
        cv.put(TableColumns.Recordings.DURATION, duration);
        cv.put(TableColumns.Recordings.EXTERNAL_UPLOAD, external_upload);
        cv.put(TableColumns.Recordings.TIP_KEY, tip);
        cv.put(TableColumns.Recordings.UPLOAD_STATUS, upload_status);
        if (playbackStream != null) {
            cv.put(TableColumns.Recordings.TRIM_LEFT, playbackStream.getStartPos());
            cv.put(TableColumns.Recordings.TRIM_RIGHT, playbackStream.getEndPos());
            cv.put(TableColumns.Recordings.OPTIMIZE, playbackStream.isOptimized() ? 1 : 0);
            cv.put(TableColumns.Recordings.FADING, playbackStream.isFading() ? 1 : 0);
        }
    }

    private boolean isPublic() {
        return !is_private && recipient_user_id <= 0;
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

    private PlaybackStream initializePlaybackStream(@Nullable Cursor c) {
        try {
            final AudioReader reader = AudioReader.guessMultiple(audio_path, getEncodedFile());

            PlaybackStream stream = new PlaybackStream(reader);
            if (c != null) {
                long startPos = c.getLong(c.getColumnIndex(TableColumns.Recordings.TRIM_LEFT));
                long endPos = c.getLong(c.getColumnIndex(TableColumns.Recordings.TRIM_RIGHT));

                // validate, can happen after migration
                if (endPos <= startPos) {
                    endPos = duration;
                }

                boolean optimize = c.getInt(c.getColumnIndex(TableColumns.Recordings.OPTIMIZE)) == 1;
                boolean fade = c.getInt(c.getColumnIndex(TableColumns.Recordings.FADING)) == 1;

                stream.setFading(fade);
                stream.setOptimize(optimize);
                stream.setTrim(startPos, endPos);
            }
            return stream;
        } catch (IOException e) {
            Log.w(TAG, "could not initialize playback stream", e);
            return new PlaybackStream(AudioReader.EMPTY);
        }
    }

    public static interface Status {
        int NOT_YET_UPLOADED = 0; // not yet uploaded, or canceled by user
        int UPLOADING = 1; // currently uploading
        int UPLOADED = 2; // successfully uploaded
        int ERROR = 4; // network / api error
    }

    public static class RecordingFilter implements FilenameFilter {
        private final Recording toIgnore;

        public RecordingFilter(@Nullable Recording ignore) {
            toIgnore = ignore;
        }

        @Override
        public boolean accept(File dir, String name) {
            return (Recording.isRawFilename(name) || Recording.isEncodedFilename(name) || Recording.isAmplitudeFile(name)) &&
                    (toIgnore == null || !toIgnore.audio_path.getName().equals(name));
        }
    }

    public static class RecordingWavFilter implements FilenameFilter {
        private final Recording toIgnore;

        public RecordingWavFilter(@Nullable Recording ignore) {
            toIgnore = ignore;
        }

        @Override
        public boolean accept(File dir, String name) {
            return Recording.isRawFilename(name) && (toIgnore == null || !toIgnore.audio_path.getName().equals(name));
        }
    }
}

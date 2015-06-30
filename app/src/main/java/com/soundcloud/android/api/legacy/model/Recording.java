package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.AmplitudeData;
import com.soundcloud.android.creators.record.AudioReader;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.FiletimeComparator;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Recording implements Comparable<Recording>, Parcelable {

    public static final File IMAGE_DIR = new File(Consts.EXTERNAL_STORAGE_DIRECTORY, "recordings/images");
    public static final String EXTRA = "recording";
    public static final int MAX_WAVE_CACHE = 100 * 1024 * 1024; // 100 mb
    public static final String TAG_SOURCE_ANDROID_RECORD = "soundcloud:source=android-record";
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
    public long duration; // in msecs
    public boolean is_private;
    public String[] tags;
    public String description, genre;
    // assets
    @NotNull
    public File audio_path;
    public String original_filename;
    @Nullable public File artwork_path;
    @Nullable public File resized_artwork_path;
    // status
    public boolean external_upload;
    public int upload_status;
    private PlaybackStream playbackStream;

    public Recording() {
        // No-op constr for the Blueprint
    }

    public Recording(@NotNull File f) {
        audio_path = f;
    }

    public Recording(Parcel in) {

        Bundle data = in.readBundle(getClass().getClassLoader());
        user_id = data.getLong("user_id");
        title = data.getString("title_text");
        description = data.getString("description");
        genre = data.getString("genre");
        audio_path = new File(data.getString("audio_path"));
        original_filename = data.getString("original_filename");
        if (data.containsKey("artwork_path")) {
            artwork_path = new File(data.getString("artwork_path"));
        }
        if (data.containsKey("resized_artwork_path")) {
            resized_artwork_path = new File(data.getString("resized_artwork_path"));
        }
        duration = data.getLong("duration");
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

    public File getImageFile(File imageDir) {
        return new File(imageDir, IOUtils.changeExtension(audio_path, "bmp").getName());
    }

    public File getImageFile() {
        return getImageFile(IMAGE_DIR);
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
        if (external_upload) {
            tags.add(TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD);
        } else {
            tags.add(TAG_SOURCE_ANDROID_RECORD);
        }
        return tags;
    }

    public boolean exists() {
        return audio_path.exists() || getEncodedFile().exists();
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
        if (!TextUtils.isEmpty(title)) {
            note = title;
        } else if (!TextUtils.isEmpty(original_filename)) {
            note = original_filename;
        } else if (external_upload && !isLegacyRecording() && !isUploadRecording()) {
            note = audio_path.getName();
        } else if (external_upload && isUploadRecording()) {
            note = "Upload (" + getFile().length() + " bytes)";
        } else {
            note = defaultSharingNote(res);
        }
        return note;
    }

    public String defaultSharingNote(Resources res) {
        return res.getString(R.string.sounds_from, recordingDateString(res));
    }

    public boolean isLegacyRecording() {
        return (external_upload && audio_path.getParentFile().equals(SoundRecorder.RECORD_DIR));
    }

    public boolean isUploadRecording() {
        return (external_upload && audio_path.getParentFile().equals(SoundRecorder.UPLOAD_DIR));
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

    public Intent getViewIntent() {
        return new Intent(Actions.RECORD);
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

    public void setUploadFailed(boolean cancelled) {
        upload_status = cancelled ? Status.NOT_YET_UPLOADED : Status.ERROR;
    }

    public void setUploading() {
        upload_status = Status.UPLOADING;
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

    public void clearArtwork() {
        IOUtils.deleteFile(artwork_path);
        artwork_path = null;
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



    @Nullable
    public static Recording fromIntent(@Nullable Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA)) {
            return intent.getParcelableExtra(EXTRA);
        }
        return null;
    }

    public void markUploaded() {
        upload_status = Status.UPLOADED;
    }

    public static Recording create() {
        File file = new File(SoundRecorder.RECORD_DIR,
                System.currentTimeMillis()
                        + "." + WavReader.EXTENSION);
        return new Recording(file);
    }

    @Override
    public String toString() {
        return "Recording{" +
                "user_id=" + user_id +
                ", title='" + title + '\'' +
                ", audio_path=" + audio_path +
                ", original_filename=" + original_filename +
                ", duration=" + duration +
                ", artwork_path=" + artwork_path +
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
        data.putLong("user_id", user_id);
        data.putString("title_text", title);
        data.putString("audio_path", audio_path.getAbsolutePath());
        data.putString("original_filename", original_filename);
        if (artwork_path != null) {
            data.putString("artwork_path", artwork_path.getAbsolutePath());
        }
        if (resized_artwork_path != null) {
            data.putString("resized_artwork_path", resized_artwork_path.getAbsolutePath());
        }
        data.putLong("duration", duration);
        data.putString("description", description);
        data.putString("genre", genre);
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

    public long getId() {
        return getAbsolutePath().hashCode();
    }

    public boolean isPublic() {
        return !is_private;
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

    public static abstract class Status {
        public static final int NOT_YET_UPLOADED = 0; // not yet uploaded, or canceled by user
        public static final int UPLOADING = 1; // currently uploading
        public static final int UPLOADED = 2; // successfully uploaded
        public static final int ERROR = 4; // network / api error
    }

    public static class RecordingFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return (Recording.isRawFilename(name) || Recording.isEncodedFilename(name) || Recording.isAmplitudeFile(name));
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

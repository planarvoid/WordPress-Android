package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.creators.record.AmplitudeData;
import com.soundcloud.android.creators.record.AudioReader;
import com.soundcloud.android.creators.record.PlaybackStream;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.creators.record.reader.VorbisReader;
import com.soundcloud.android.creators.record.reader.WavReader;
import com.soundcloud.android.utils.FiletimeComparator;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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

    private static final String IMAGE_DIR = "recordings/images";
    private static final int MAX_WAVE_CACHE = 100 * 1024 * 1024; // 100 mb
    private static final String TAG_SOURCE_ANDROID_RECORD = "soundcloud:source=android-record";
    private static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";
    private static final Pattern AMPLITUDE_PATTERN = Pattern.compile("^.*\\.(amp)$");
    private static final Pattern RAW_PATTERN = Pattern.compile("^.*\\.(2|pcm|wav)$");
    private static final Pattern ENCODED_PATTERN = Pattern.compile("^.*\\.(0|1|mp4|ogg)$");

    public static final String EXTRA = "recording";
    public static final String PROCESSED_APPEND = "_processed";
    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };
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
    private int upload_status;
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

    public String getTitle(Context context) {
        return TextUtils.isEmpty(title) ? sharingNote(context) : title;
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

    private File getImageFile(File imageDir) {
        return new File(imageDir, IOUtils.changeExtension(audio_path, "bmp").getName());
    }

    public File getImageFile(Context context) {
        return getImageFile(IOUtils.createExternalStorageDir(context, IMAGE_DIR));
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
            playbackStream = initializePlaybackStream();
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
    private long lastModified() {
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

    private static boolean isRawFilename(String filename) {
        return RAW_PATTERN.matcher(filename).matches();
    }

    private static boolean isEncodedFilename(String filename) {
        return ENCODED_PATTERN.matcher(filename).matches();
    }

    public String sharingNote(Context context) {
        String note;
        if (!TextUtils.isEmpty(title)) {
            note = title;
        } else if (!TextUtils.isEmpty(original_filename)) {
            note = original_filename;
        } else if (external_upload && !isLegacyRecording(context) && !isUploadRecording(context)) {
            note = audio_path.getName();
        } else {
            note = defaultSharingNote(context.getResources());
        }
        return note;
    }

    private String defaultSharingNote(Resources res) {
        return res.getString(R.string.record_default_title_sounds_from_day_time_of_day, recordingDateString(res));
    }

    public boolean isLegacyRecording(Context context) {
        return (external_upload && audio_path.getParentFile().equals(SoundRecorder.recordingDir(context)));
    }

    public boolean isUploadRecording(Context context) {
        return (external_upload && audio_path.getParentFile().equals(SoundRecorder.recordingDir(context)));
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

    private boolean hasResizedArtwork() {
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

    public static Recording create(Context context) {
        File file = new File(SoundRecorder.recordingDir(context),
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

    private static long trimWaveFiles(File directory, Recording ignore, long maxCacheSize) {
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
            id = R.string.recorded_dayofweek_morning;
        } else if (time.hour <= 17) {
            id = R.string.recorded_dayofweek_afternoon;
        } else if (time.hour <= 21) {
            id = R.string.recorded_dayofweek_evening;
        } else {
            id = R.string.recorded_dayofweek_night;
        }
        return res.getString(id, time.format("%A"));
    }

    private PlaybackStream initializePlaybackStream() {
        try {
            return new PlaybackStream(AudioReader.guessMultiple(audio_path, getEncodedFile()));
        } catch (IOException e) {
            Log.w(TAG, "could not initialize playback stream", e);
            return new PlaybackStream(AudioReader.EMPTY);
        }
    }

    public static abstract class Status {
        static final int NOT_YET_UPLOADED = 0; // not yet uploaded, or canceled by user
        static final int UPLOADING = 1; // currently uploading
        static final int UPLOADED = 2; // successfully uploaded
        public static final int ERROR = 4; // network / api error
    }

    public static class RecordingFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return (Recording.isRawFilename(name) || Recording.isEncodedFilename(name) || Recording.isAmplitudeFile(name));
        }
    }

    private static class RecordingWavFilter implements FilenameFilter {
        private final Recording toIgnore;

        RecordingWavFilter(@Nullable Recording ignore) {
            toIgnore = ignore;
        }

        @Override
        public boolean accept(File dir, String name) {
            return Recording.isRawFilename(name) && (toIgnore == null || !toIgnore.audio_path.getName().equals(name));
        }
    }
}

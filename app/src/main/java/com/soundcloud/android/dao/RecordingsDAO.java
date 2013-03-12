package com.soundcloud.android.dao;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingsDAO {
    public static Uri insert(Recording recording, ContentResolver contentResolver) {
        return insert(recording, contentResolver, true);
    }

    public static Uri insert(Recording recording, ContentResolver contentResolver, boolean fullValues) {
        recording.insertDependencies(contentResolver);
        // insert parent resource, with possible partial values
        return contentResolver.insert(recording.toUri(), fullValues ? recording.buildContentValues() : recording.buildBaseContentValues());
    }

    public static boolean updateStatus(Recording recording, ContentResolver resolver) {
        if (recording.id > 0) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.Recordings.UPLOAD_STATUS, recording.upload_status);
            cv.put(DBHelper.Recordings.AUDIO_PATH, recording.audio_path.getAbsolutePath());
            return resolver.update(recording.toUri(), cv, null, null) > 0;
        } else {
            return false;
        }
    }

    public static boolean delete(Recording recording, @Nullable ContentResolver resolver) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        if (recording.id > 0 && resolver != null) resolver.delete(recording.toUri(), null, null);
        return deleted;
    }

    /**
     * Gets called after successful upload. Clean any tmp files here.
     */
    public static void setUploaded(Recording recording, ContentResolver resolver) {
        recording.upload_status = Recording.Status.UPLOADED;
        if (!recording.external_upload) {
            IOUtils.deleteFile(recording.getFile());
            IOUtils.deleteFile(recording.getEncodedFile());
        }
        IOUtils.deleteFile(recording.resized_artwork_path);
        updateStatus(recording, resolver);
    }

    public static List<Recording> getUnsavedRecordings(ContentResolver resolver, File directory, Recording ignore, long userId) {
        MediaPlayer mp = null;
        List<Recording> unsaved = new ArrayList<Recording>();

        Map<String,File> toCheck = new HashMap<String,File>();
        final File[] list = IOUtils.nullSafeListFiles(directory, new Recording.RecordingFilter(ignore));
        Arrays.sort(list); // we want .wav files taking precedence, so make sure they appear last (alpha order)
        for (File f : list) {
            if (Recording.getUserIdFromFile(f) != -1) continue; //TODO, what to do about private messages
            toCheck.put(IOUtils.removeExtension(f).getAbsolutePath(), f);
        }
        for (File f : toCheck.values()) {
            if (Recording.isAmplitudeFile(f.getName())) {
                Log.d(TAG, "Deleting isolated amplitude file : " + f.getName() + " : " + f.delete());
            } else {
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
                    if (r.duration <= 0 || f.getName().contains(Recording.PROCESSED_APPEND)) {
                        Log.d(TAG, "Deleting unusable file : " + f.getName() + " : " + delete(r, resolver));
                    } else {
                        unsaved.add(r);
                    }
                }
            }
        }
        Collections.sort(unsaved, null);
        return unsaved;
    }
}

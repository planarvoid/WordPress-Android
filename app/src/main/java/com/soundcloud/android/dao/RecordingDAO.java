package com.soundcloud.android.dao;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.model.Recording.getUserIdFromFile;
import static com.soundcloud.android.model.Recording.isAmplitudeFile;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
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

public class RecordingDAO extends BaseDAO<Recording> {
    public RecordingDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public Uri insert(Recording recording) {
        return insert(recording, true);
    }

    public Uri insert(Recording recording, boolean fullValues) {
        recording.insertDependencies(mResolver);
        // create parent resource, with possible partial values
        return mResolver.insert(recording.toUri(), fullValues ? recording.buildContentValues() : recording.buildBaseContentValues());
    }

    public boolean updateStatus(Recording recording) {
        if (recording.id > 0) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.Recordings.UPLOAD_STATUS, recording.upload_status);
            cv.put(DBHelper.Recordings.AUDIO_PATH, recording.audio_path.getAbsolutePath());
            return mResolver.update(recording.toUri(), cv, null, null) > 0;
        } else {
            return false;
        }
    }

    public boolean delete(Recording recording) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        if (recording.id > 0) mResolver.delete(recording.toUri(), null, null);
        return deleted;
    }

    public @Nullable Recording getRecordingByUri(Uri uri) {
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        Recording recording = null;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();
        return recording;
    }

    public @Nullable Recording getRecordingByPath(File file) {
        Cursor cursor = mResolver.query(Content.RECORDINGS.uri,
                null,
                DBHelper.Recordings.AUDIO_PATH + " LIKE ?",
                new String[]{ IOUtils.removeExtension(file).getAbsolutePath() + "%" },
                null);

        Recording recording = null;
        if (cursor != null && cursor.moveToFirst()) {
            recording = new Recording(cursor);
        }
        if (cursor != null) cursor.close();

        return recording;
    }

    public List<Recording> getUnsavedRecordings(File directory, Recording ignore, long userId) {
        MediaPlayer mp = null;
        List<Recording> unsaved = new ArrayList<Recording>();

        Map<String,File> toCheck = new HashMap<String,File>();
        final File[] list = IOUtils.nullSafeListFiles(directory, new Recording.RecordingFilter(ignore));
        Arrays.sort(list); // we want .wav files taking precedence, so make sure they appear last (alpha order)
        for (File f : list) {
            if (getUserIdFromFile(f) != -1) continue; //TODO, what to do about private messages
            toCheck.put(IOUtils.removeExtension(f).getAbsolutePath(), f);
        }
        for (File f : toCheck.values()) {
            if (isAmplitudeFile(f.getName())) {
                Log.d(TAG, "Deleting isolated amplitude file : " + f.getName() + " : " + f.delete());
            } else {
                Recording r = getRecordingByPath(f);
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
                        Log.d(TAG, "Deleting unusable file : " + f.getName() + " : " + delete(r));
                    } else {
                        unsaved.add(r);
                    }
                }
            }
        }
        Collections.sort(unsaved, null);
        return unsaved;
    }


    @Override
    public Content getContent() {
        return Content.RECORDINGS;
    }
}

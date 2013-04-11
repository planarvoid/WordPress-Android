package com.soundcloud.android.dao;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.model.Recording.getUserIdFromFile;
import static com.soundcloud.android.model.Recording.isAmplitudeFile;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Context;
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

public class RecordingStorage implements Storage<Recording> {

    private final RecordingDAO mRecordingDAO;

    public RecordingStorage(Context context) {
        mRecordingDAO = new RecordingDAO(context.getContentResolver());
    }

    @Override
    public void create(Recording resource) {
        mRecordingDAO.create(resource);
    }

    public void createFromBaseValues(Recording recording) {
        mRecordingDAO.createDependencies(recording);
        long id = mRecordingDAO.create(recording.toUri(), recording.buildBaseContentValues());
        recording.setId(id);
    }

    public boolean updateStatus(Recording recording) {
        if (recording.id > 0) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.Recordings.UPLOAD_STATUS, recording.upload_status);
            cv.put(DBHelper.Recordings.AUDIO_PATH, recording.audio_path.getAbsolutePath());
            return mRecordingDAO.update(recording.id, cv);
        } else {
            return false;
        }
    }

    public @Nullable Recording getRecordingByUri(Uri uri) {
        return mRecordingDAO.queryByUri(uri);
    }

    public @Nullable Recording getRecordingByPath(File file) {
        return mRecordingDAO.buildQuery()
                .where(DBHelper.Recordings.AUDIO_PATH + " LIKE ?", IOUtils.removeExtension(file).getAbsolutePath() + "%")
                .first();
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

    public boolean delete(Recording recording) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        if (recording.id > 0) mRecordingDAO.delete(recording);
        return deleted;
    }
}

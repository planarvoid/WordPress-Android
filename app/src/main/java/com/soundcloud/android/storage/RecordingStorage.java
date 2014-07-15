package com.soundcloud.android.storage;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.api.legacy.model.Recording.getUserIdFromFile;
import static com.soundcloud.android.api.legacy.model.Recording.isAmplitudeFile;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscriber;

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

public class RecordingStorage extends ScheduledOperations implements Storage<Recording> {

    private final RecordingDAO recordingDAO;

    public RecordingStorage() {
        super(ScSchedulers.STORAGE_SCHEDULER);
        ContentResolver resolver = SoundCloudApplication.instance.getContentResolver();
        recordingDAO = new RecordingDAO(resolver);
    }

    @Override
    public Observable<Recording> storeAsync(final Recording recording) {
        return schedule(Observable.create(new Observable.OnSubscribe<Recording>() {
            @Override
            public void call(Subscriber<? super Recording> observer) {
                store(recording);
                observer.onNext(recording);
                observer.onCompleted();
            }
        }));
    }

    @Override
    public Recording store(Recording recording) {
        recordingDAO.create(recording);
        return recording;
    }

    public void createFromBaseValues(Recording recording) {
        recordingDAO.createDependencies(recording);
        long id = recordingDAO.create(recording.toUri(), recording.buildBaseContentValues());
        recording.setId(id);
    }

    public boolean updateStatus(Recording recording) {
        if (recording.getId() > 0) {
            ContentValues cv = new ContentValues();
            cv.put(TableColumns.Recordings.UPLOAD_STATUS, recording.upload_status);
            cv.put(TableColumns.Recordings.AUDIO_PATH, recording.audio_path.getAbsolutePath());
            return recordingDAO.update(recording.getId(), cv);
        } else {
            return false;
        }
    }

    public @Nullable Recording getRecordingByUri(Uri uri) {
        return recordingDAO.queryByUri(uri);
    }

    public @Nullable Recording getRecordingByPath(File file) {
        return recordingDAO.buildQuery()
                .where(TableColumns.Recordings.AUDIO_PATH + " LIKE ?", IOUtils.removeExtension(file).getAbsolutePath() + "%")
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

    public boolean delete(final Recording recording) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        if (recording.getId() > 0) {
            recordingDAO.delete(recording);
        }
        return deleted;
    }
}

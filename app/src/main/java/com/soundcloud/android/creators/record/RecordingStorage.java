package com.soundcloud.android.creators.record;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.api.legacy.model.Recording.isAmplitudeFile;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Subscriber;

import android.util.Log;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingStorage {

    private final AccountOperations accountOperations;
    private final AudioDurationHelper durationHelper;

    @Inject
    public RecordingStorage(AccountOperations accountOperations, AudioDurationHelper durationHelper) {
        this.accountOperations = accountOperations;
        this.durationHelper = durationHelper;
    }

    public Observable<CleanupRecordingsResult> cleanupRecordings(final File recordingDir) {
        return Observable.create(new Observable.OnSubscribe<CleanupRecordingsResult>() {
            @Override
            public void call(Subscriber<? super CleanupRecordingsResult> subscriber) {
                CleanupRecordingsResult recording = cleanupRecordings(recordingDir, accountOperations.getLoggedInUserUrn());
                if (recording != null) {
                    subscriber.onNext(recording);
                }
                subscriber.onCompleted();
            }
        });
    }

    public static boolean delete(final Recording recording) {
        boolean deleted = false;
        if (!recording.external_upload || recording.isLegacyRecording()) {
            deleted = IOUtils.deleteFile(recording.audio_path);
        }
        IOUtils.deleteFile(recording.getEncodedFile());
        IOUtils.deleteFile(recording.getAmplitudeFile());
        return deleted;
    }

    // this is poached from legacy code mostly. It's a bit ugly, but it is at least now tested
    private CleanupRecordingsResult cleanupRecordings(File directory, Urn loggedInUserUrn) {

        Map<String, File> toCheck = new HashMap<>();
        final File[] list = IOUtils.nullSafeListFiles(directory, new Recording.RecordingFilter());
        Arrays.sort(list); // we want .wav files taking precedence, so make sure they appear last (alpha order)
        for (File f : list) {
            toCheck.put(IOUtils.removeExtension(f).getAbsolutePath(), f);
        }

        List<Recording> unsavedRecordings = new ArrayList<>();
        int amplituedFilesRemoved = 0;
        int invalidRecordingsRemoved = 0;

        for (File f : toCheck.values()) {
            if (isAmplitudeFile(f.getName())) {
                if (f.delete()) {
                    Log.d(TAG, "Deleting isolated amplitude file : " + f.getName());
                    amplituedFilesRemoved++;
                } else {
                    Log.e(TAG, "Could not delete isolated amplitude file : " + f.getName());
                }

            } else {

                Recording recording = new Recording(f);
                recording.user_id = loggedInUserUrn.getNumericId();
                recording.duration = durationHelper.getDuration(f);

                if (recording.duration <= 0 || f.getName().contains(Recording.PROCESSED_APPEND)) {
                    if (delete(recording)) {
                        Log.d(TAG, "Deleting unusable file : " + f.getName());
                        invalidRecordingsRemoved++;
                    } else {
                        Log.e(TAG, "Could not delete unusable file : " + f.getName());
                    }
                } else {
                    unsavedRecordings.add(recording);
                }
            }
        }
        return new CleanupRecordingsResult(unsavedRecordings, amplituedFilesRemoved, invalidRecordingsRemoved);
    }

}

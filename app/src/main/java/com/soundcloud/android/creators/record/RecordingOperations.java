package com.soundcloud.android.creators.record;

import com.soundcloud.android.Actions;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.upload.UploadService;
import rx.Observable;
import rx.Scheduler;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

public class RecordingOperations {

    private final Scheduler scheduler;
    private final RecordingStorage recordingStorage;

    @Inject
    public RecordingOperations(@Named("HighPriority") Scheduler scheduler, RecordingStorage recordingStorage) {
        this.scheduler = scheduler;
        this.recordingStorage = recordingStorage;
    }

    public Observable<CleanupRecordingsResult> cleanupRecordings(File recordingDirectory) {
        return recordingStorage.cleanupRecordings(recordingDirectory).subscribeOn(scheduler);
    }
}

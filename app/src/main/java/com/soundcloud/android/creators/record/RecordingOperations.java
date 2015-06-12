package com.soundcloud.android.creators.record;

import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

class RecordingOperations {

    private final Scheduler scheduler;
    private final RecordingStorage recordingStorage;

    @Inject
    RecordingOperations(@Named("HighPriority") Scheduler scheduler, RecordingStorage recordingStorage) {
        this.scheduler = scheduler;
        this.recordingStorage = recordingStorage;
    }

    public Observable<CleanupRecordingsResult> cleanupRecordings(File recordingDirectory) {
        return recordingStorage.cleanupRecordings(recordingDirectory).subscribeOn(scheduler);
    }
}

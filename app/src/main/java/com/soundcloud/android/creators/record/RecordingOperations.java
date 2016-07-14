package com.soundcloud.android.creators.record;

import com.soundcloud.android.api.legacy.model.Recording;
import rx.Observable;
import rx.Scheduler;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.List;

class RecordingOperations {

    private final Scheduler scheduler;
    private final RecordingStorage recordingStorage;

    @Inject
    RecordingOperations(@Named("HighPriority") Scheduler scheduler, RecordingStorage recordingStorage) {
        this.scheduler = scheduler;
        this.recordingStorage = recordingStorage;
    }

    public Observable<List<Recording>> cleanupRecordings(Context context, File recordingDirectory) {
        return recordingStorage.cleanupRecordings(context, recordingDirectory).subscribeOn(scheduler);
    }

    public Observable<Void> deleteStaleUploads(Context context, File uploadsDirectory) {
        return recordingStorage.deleteStaleUploads(context, uploadsDirectory).subscribeOn(scheduler);
    }

    public Observable<Recording> upload(File uploadsDirectory, Uri stream, String type, ContentResolver resolver) {
        return recordingStorage.upload(uploadsDirectory, stream, type, resolver).subscribeOn(scheduler);
    }

}

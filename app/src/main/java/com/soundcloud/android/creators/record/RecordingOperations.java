package com.soundcloud.android.creators.record;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.legacy.model.Recording;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

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
    RecordingOperations(@Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler, RecordingStorage recordingStorage) {
        this.scheduler = scheduler;
        this.recordingStorage = recordingStorage;
    }

    Observable<List<Recording>> cleanupRecordings(Context context, File recordingDirectory) {
        return recordingStorage.cleanupRecordings(context, recordingDirectory).subscribeOn(scheduler);
    }

    Completable deleteStaleUploads(Context context, File uploadsDirectory) {
        return recordingStorage.deleteStaleUploads(context, uploadsDirectory).subscribeOn(scheduler);
    }

    public Observable<Recording> upload(File uploadsDirectory, Uri stream, String type, ContentResolver resolver) {
        return recordingStorage.upload(uploadsDirectory, stream, type, resolver).subscribeOn(scheduler);
    }

}

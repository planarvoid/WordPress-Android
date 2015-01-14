package com.soundcloud.android.configuration.experiments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import rx.Observable;
import rx.Scheduler;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class ExperimentStorage extends ScheduledOperations {

    private static final String ASSIGNMENT_FILE_NAME = ".assignment";

    private final JsonTransformer jsonTransformer;
    private final File file;

    @Inject
    ExperimentStorage(Context context, JsonTransformer jsonTransformer) {
        this(ScSchedulers.STORAGE_SCHEDULER, context, jsonTransformer);
    }

    @VisibleForTesting
    ExperimentStorage(Scheduler scheduler, Context context, JsonTransformer jsonTransformer) {
        super(scheduler);
        this.jsonTransformer = jsonTransformer;
        this.file = new File(context.getFilesDir(), ASSIGNMENT_FILE_NAME);
    }

    public void storeAssignment(Assignment assignment) {
        try {
            String json = jsonTransformer.toJson(assignment);
            IOUtils.writeFileFromString(file, json);
        } catch (ApiMapperException e) {
            ErrorUtils.handleThrowable(e, getClass());
        }
    }

    public Observable<Assignment> readAssignment() {
        return schedule(Observable.just(loadAssignment()));
    }

    private Assignment loadAssignment() {
        return file.exists() ? readAssignmentFile(file) : Assignment.empty();
    }

    private Assignment readAssignmentFile(File file) {
        String json = ScTextUtils.EMPTY_STRING;
        try {
            json = IOUtils.readInputStream(new FileInputStream(file));
            return jsonTransformer.fromJson(json, TypeToken.of(Assignment.class));
        } catch (IOException e) {
            ErrorUtils.handleSilentException(e);
            return Assignment.empty();
        } catch (ApiMapperException e) {
            // see https://www.crashlytics.com/soundcloudandroid/android/apps/com.soundcloud.android/issues/5452b652e3de5099ba2b4fea
            ErrorUtils.handleSilentException(new IllegalStateException("Failed parsing assignment; json = " + json, e));
            IOUtils.deleteFile(file);
            return Assignment.empty();
        }
    }
}

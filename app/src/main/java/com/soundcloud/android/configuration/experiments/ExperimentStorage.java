package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.Context;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class ExperimentStorage {

    private static final String ASSIGNMENT_FILE_NAME = ".assignment";

    private final Scheduler scheduler;
    private final JsonTransformer jsonTransformer;
    private final File file;

    @Inject
    ExperimentStorage(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler, Context context, JsonTransformer jsonTransformer) {
        this.scheduler = scheduler;
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
        return loadAssignment().subscribeOn(scheduler);
    }

    private Observable<Assignment> loadAssignment() {
        return Observable.create(new Observable.OnSubscribe<Assignment>() {
            @Override
            public void call(Subscriber<? super Assignment> subscriber) {
                Assignment assignment = file.exists() ? readAssignmentFile(file) : Assignment.empty();
                subscriber.onNext(assignment);
                subscriber.onCompleted();
            }
        });
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

package com.soundcloud.android.experiments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

class ExperimentStorage extends ScheduledOperations {

    private static final String ASSIGNMENT_FILE_NAME = ".assignment";

    private final Context context;
    private final JsonTransformer jsonTransformer;

    @Inject
    ExperimentStorage(Context context, JsonTransformer jsonTransformer) {
        this(ScSchedulers.STORAGE_SCHEDULER, context, jsonTransformer);
    }

    @VisibleForTesting
    ExperimentStorage(Scheduler scheduler, Context context, JsonTransformer jsonTransformer) {
        super(scheduler);
        this.context = context;
        this.jsonTransformer = jsonTransformer;
    }

    public void storeAssignment(Assignment assignment) {
        try {
            String json = jsonTransformer.toJson(assignment);
            IOUtils.writeFileFromString(getAssignmentHandle(), json);
        } catch (IOException e) {
            ErrorUtils.handleThrowable(e);
        }
    }

    public Observable<Assignment> loadAssignmentAsync() {
        return schedule(Observable.create(new Observable.OnSubscribe<Assignment>() {
            @Override
            public void call(Subscriber<? super Assignment> subscriber) {
                if (hasAssignment()) {
                    try {
                        subscriber.onNext(loadAssignment());
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }
                subscriber.onCompleted();
            }
        }));
    }

    public Assignment loadAssignment() throws Exception {
        String json = IOUtils.readInputStream(new FileInputStream(getAssignmentHandle()));
        return jsonTransformer.fromJson(json, TypeToken.of(Assignment.class));
    }

    private boolean hasAssignment() {
        return getAssignmentHandle().exists();
    }

    private File getAssignmentHandle() {
        return new File(context.getFilesDir(), ASSIGNMENT_FILE_NAME);
    }

}

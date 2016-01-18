package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;
import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.sync.SyncConfig;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;

public class AppboySyncNotificationsExperiment {
    static final String NAME = "android_push_notifications";
    static final String VARIATION_SERVER_SIDE = "server_side";
    static final String VARIATION_CLIENT_SIDE = "client_side";
    static final String CUSTOM_PUSH_ATTRIBUTE = "android_push_experiment";
    static final Experiment EXPERIMENT =
            Experiment.create(LISTENING_LAYER, NAME, Arrays.asList(VARIATION_SERVER_SIDE, VARIATION_CLIENT_SIDE));

    private final ExperimentOperations experimentOperations;
    private final AppboyWrapper appboy;
    private final SyncConfig syncConfig;
    private final Scheduler scheduler;

    @Inject
    public AppboySyncNotificationsExperiment(ExperimentOperations experimentOperations,
                                             AppboyWrapper appboy,
                                             SyncConfig syncConfig,
                                             @Named(HIGH_PRIORITY) Scheduler scheduler) {
        this.experimentOperations = experimentOperations;
        this.appboy = appboy;
        this.syncConfig = syncConfig;
        this.scheduler = scheduler;
    }

    public Observable<Boolean> configure() {
        return experimentOperations.loadAssignment()
                .map(updateConfig())
                .subscribeOn(scheduler);
    }

    private boolean isServerSideNotifications() {
        switch (experimentOperations.getExperimentVariant(NAME)) {
            case VARIATION_SERVER_SIDE:
                return true;
            case VARIATION_CLIENT_SIDE:
            default:
                return false;
        }
    }

    private Func1<Assignment, Boolean> updateConfig() {
        return new Func1<Assignment, Boolean>() {
            @Override
            public Boolean call(Assignment assignment) {
                boolean wasServerSide = syncConfig.isServerSideNotifications();
                boolean isServerSide = isServerSideNotifications();

                if (wasServerSide != isServerSide) {
                    if (isServerSide) {
                        enableServerSideNotifications();
                    } else {
                        disableServerSideNotifications();
                    }
                }

                return isServerSide;
            }
        };
    }

    private void disableServerSideNotifications() {
        appboy.setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, false);
        syncConfig.disableServerSideNotifications();
    }

    private void enableServerSideNotifications() {
        if (syncConfig.isIncomingEnabled()) {
            appboy.setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, true);
            syncConfig.enableServerSideNotifications();
        }
    }

}

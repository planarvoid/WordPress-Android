package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.AppboySyncNotificationsExperiment.CUSTOM_PUSH_ATTRIBUTE;
import static com.soundcloud.android.configuration.experiments.AppboySyncNotificationsExperiment.EXPERIMENT;
import static com.soundcloud.android.configuration.experiments.AppboySyncNotificationsExperiment.VARIATION_CLIENT_SIDE;
import static com.soundcloud.android.configuration.experiments.AppboySyncNotificationsExperiment.VARIATION_SERVER_SIDE;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.strings.Strings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.Schedulers;

public class AppboySyncNotificationsExperimentTest extends AndroidUnitTest {

    @Mock ExperimentOperations experimentOperations;
    @Mock AppboyWrapper appboy;
    @Mock SyncConfig syncConfig;

    private AppboySyncNotificationsExperiment experiment;

    @Before
    public void setUp() throws Exception {
        when(experimentOperations.loadAssignment()).thenReturn(Observable.just(Assignment.empty()));
        when(syncConfig.isIncomingEnabled()).thenReturn(true);
        experiment = new AppboySyncNotificationsExperiment(experimentOperations,
                appboy, syncConfig, Schedulers.immediate());
    }

    @Test
    public void testConfigureExperimentIsEnabled() {
        when(experimentOperations.getExperimentVariant(EXPERIMENT.getName())).thenReturn(VARIATION_SERVER_SIDE);
        when(syncConfig.isServerSideNotifications()).thenReturn(false);

        fireAndForget(experiment.configure());

        verify(syncConfig).enableServerSideNotifications();
        verify(appboy).setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, true);
    }

    @Test
    public void testConfigureExperimentIsEnabledButIncomingIsDisabled() {
        when(syncConfig.isIncomingEnabled()).thenReturn(false);
        when(experimentOperations.getExperimentVariant(EXPERIMENT.getName())).thenReturn(VARIATION_SERVER_SIDE);
        when(syncConfig.isServerSideNotifications()).thenReturn(false);

        fireAndForget(experiment.configure());

        verify(syncConfig, never()).enableServerSideNotifications();
        verify(appboy, never()).setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, true);
    }

    @Test
    public void testConfigureExperimentDidNotChange() {
        when(experimentOperations.getExperimentVariant(EXPERIMENT.getName())).thenReturn(VARIATION_CLIENT_SIDE);
        when(syncConfig.isServerSideNotifications()).thenReturn(false);

        fireAndForget(experiment.configure());

        verify(syncConfig, never()).enableServerSideNotifications();
        verify(appboy, never()).setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, false);
    }

    @Test
    public void testConfigureExperimentWasEnabled() {
        when(experimentOperations.getExperimentVariant(EXPERIMENT.getName())).thenReturn(Strings.EMPTY);
        when(syncConfig.isServerSideNotifications()).thenReturn(true);

        fireAndForget(experiment.configure());

        verify(syncConfig).disableServerSideNotifications();
        verify(appboy).setCustomUserAttribute(CUSTOM_PUSH_ATTRIBUTE, false);
    }

}

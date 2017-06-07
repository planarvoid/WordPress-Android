package com.soundcloud.android.configuration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.offline.OfflineContentController;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationFeatureControllerTest {

    @Mock private OfflineContentController offlineController;
    @Mock private FeatureOperations featureOperations;

    private ConfigurationFeatureController controller;
    private io.reactivex.subjects.PublishSubject<Boolean> featureUpdatesObservable;

    @Before
    public void setUp() throws Exception {
        featureUpdatesObservable = PublishSubject.create();
        when(featureOperations.offlineContentEnabled()).thenReturn(featureUpdatesObservable);
        controller = new ConfigurationFeatureController(offlineController, featureOperations);
    }

    @Test
    public void initialiseSubscribesOfflineControllerWhenFeatureEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        controller.subscribe();

        verify(offlineController).subscribe();
    }

    @Test
    public void initialiseDoesNotSubscribeOfflineControllerWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        controller.subscribe();

        verifyNoMoreInteractions(offlineController);
    }

    @Test
    public void unsubscribeOfflineControllerOnFeatureDisabled() {
        controller.subscribe();

        featureUpdatesObservable.onNext(false);

        verify(offlineController).unsubscribe();
    }

    @Test
    public void subscribeOfflineControllerOnFeatureEnabled() {
        controller.subscribe();

        featureUpdatesObservable.onNext(true);

        verify(offlineController).subscribe();
    }
}

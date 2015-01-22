package com.soundcloud.android.configuration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

@RunWith(SoundCloudTestRunner.class)
public class ConfigurationFeatureControllerTest {

    @Mock private OfflineContentController offlineController;
    @Mock private FeatureOperations featureOperations;

    private ConfigurationFeatureController controller;
    private PublishSubject<Boolean> featureUpdatesObservable;

    @Before
    public void setUp() throws Exception {
        featureUpdatesObservable = PublishSubject.create();
        when(featureOperations.offlineSyncEnabled()).thenReturn(featureUpdatesObservable);
        controller = new ConfigurationFeatureController(offlineController, featureOperations);
    }

    @Test
    public void initialiseSubscribesOfflineControllerWhenFeatureEnabled() {
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);

        controller.subscribe();

        verify(offlineController).subscribe();
    }

    @Test
    public void initialiseDoesNotSubscribeOfflineControllerWhenFeatureDisabled() {
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(false);

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

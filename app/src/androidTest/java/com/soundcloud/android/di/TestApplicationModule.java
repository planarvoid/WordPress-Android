package com.soundcloud.android.di;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.ConfigurationSettingsStorage;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.ForceUpdateHandler;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.PlanChangeDetector;
import com.soundcloud.android.configuration.TestConfigurationOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.image.ImageConfigurationStorage;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.TestConnectionHelper;
import com.soundcloud.android.utils.TryWithBackOff;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Scheduler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

import javax.inject.Named;

public class TestApplicationModule extends ApplicationModule {

    public TestApplicationModule(SoundCloudTestApplication application) {
        super(application);
    }

    @Override
    public ConnectionHelper provideConnectionHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager, EventBus eventBus) {
        return new TestConnectionHelper(eventBus);
    }

    @Override
    public ConfigurationOperations provideConfigurationOperations(ApiClientRx apiClientRx,
                                                                  ExperimentOperations experimentOperations,
                                                                  FeatureOperations featureOperations,
                                                                  PendingPlanOperations pendingPlanOperations,
                                                                  ConfigurationSettingsStorage configurationSettingsStorage,
                                                                  TryWithBackOff.Factory tryWithBackOffFactory,
                                                                  @Named(HIGH_PRIORITY) Scheduler scheduler,
                                                                  PlanChangeDetector planChangeDetector,
                                                                  ForceUpdateHandler forceUpdateHandler,
                                                                  ImageConfigurationStorage imageConfigurationStorage) {
        return new TestConfigurationOperations(apiClientRx,
                                               experimentOperations,
                                               featureOperations,
                                               pendingPlanOperations,
                                               configurationSettingsStorage,
                                               tryWithBackOffFactory,
                                               scheduler,
                                               planChangeDetector,
                                               forceUpdateHandler,
                                               imageConfigurationStorage);
    }

    @Override
    public GooglePlayServicesWrapper provideGooglePlayServicesWrapper() {
        final GooglePlayServicesWrapper googlePlayServicesWrapper = mock(GooglePlayServicesWrapper.class);
        when(googlePlayServicesWrapper.isPlayServiceAvailable(any(Context.class))).thenReturn(true);
        when(googlePlayServicesWrapper.isPlayServiceAvailable(any(Context.class), anyInt())).thenReturn(true);
        return googlePlayServicesWrapper;
    }
}

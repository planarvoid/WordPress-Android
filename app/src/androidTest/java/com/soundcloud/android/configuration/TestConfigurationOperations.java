package com.soundcloud.android.configuration;


import static com.soundcloud.android.ApplicationModule.HIGH_PRIORITY;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.image.ImageConfigurationStorage;
import com.soundcloud.android.utils.TryWithBackOff;
import rx.Scheduler;

import javax.inject.Named;
import java.util.Collections;

/**
 * This class performs almost exactly the same function as the production ConfigurationOperations class except that it does not store the experiment(s) returned by the API.
 *
 * This is done so that the acceptance tests will not be part of of any experiments by default. We can, however, still explicitly set up an experiment for a test by using ExperimentsHelper.set(...)
 */
public class TestConfigurationOperations extends ConfigurationOperations {

    public TestConfigurationOperations(ApiClientRx apiClientRx,
                                       ExperimentOperations experimentOperations,
                                       FeatureOperations featureOperations,
                                       PendingPlanOperations pendingPlanOperations,
                                       ConfigurationSettingsStorage configurationSettingsStorage,
                                       TryWithBackOff.Factory tryWithBackOffFactory,
                                       @Named(HIGH_PRIORITY) Scheduler scheduler,
                                       PlanChangeDetector planChangeDetector,
                                       ForceUpdateHandler forceUpdateHandler,
                                       ImageConfigurationStorage imageConfigurationStorage) {
        super(apiClientRx,
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
    public void saveConfiguration(Configuration configuration) {
        Configuration configurationWithoutExperiment = Configuration.create(configuration.getFeatures(),
                                                                            configuration.getUserPlan(),
                                                                            Collections.emptyList(),
                                                                            configuration.getDeviceManagement(),
                                                                            configuration.isSelfDestruct(),
                                                                            configuration.getImageSizeSpecs());
        super.saveConfiguration(configurationWithoutExperiment);
    }
}

package com.soundcloud.android.configuration;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class ForceUpdateHandler {

    private final EventBus eventBus;
    private final BuildHelper buildHelper;
    private final DeviceHelper deviceHelper;
    private final ConfigurationSettingsStorage storage;

    @Inject
    ForceUpdateHandler(EventBus eventBus,
                       BuildHelper buildHelper,
                       DeviceHelper deviceHelper,
                       ConfigurationSettingsStorage storage) {
        this.eventBus = eventBus;
        this.buildHelper = buildHelper;
        this.deviceHelper = deviceHelper;
        this.storage = storage;
    }

    void checkForForcedUpdate(Configuration configuration) {
        if (configuration.selfDestruct) {
            storage.storeForceUpdateVersion(deviceHelper.getAppVersionCode());
            publishForceUpdateEvent();
        } else {
            storage.clearForceUpdateVersion();
        }
    }

    void checkPendingForcedUpdate() {
        if (storage.getForceUpdateVersion() == deviceHelper.getAppVersionCode()) {
            publishForceUpdateEvent();
        }
    }

    private void publishForceUpdateEvent() {
        final ForceUpdateEvent event = new ForceUpdateEvent(
                buildHelper.getAndroidReleaseVersion(),
                deviceHelper.getAppVersionName(),
                deviceHelper.getAppVersionCode());
        eventBus.publish(EventQueue.FORCE_UPDATE, event);
    }
}

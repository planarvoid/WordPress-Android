package com.soundcloud.android.configuration;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ForceUpdateHandler {

    private final EventBusV2 eventBus;
    private final BuildHelper buildHelper;
    private final DeviceHelper deviceHelper;
    private final ConfigurationSettingsStorage storage;

    @Inject
    ForceUpdateHandler(EventBusV2 eventBus,
                       BuildHelper buildHelper,
                       DeviceHelper deviceHelper,
                       ConfigurationSettingsStorage storage) {
        this.eventBus = eventBus;
        this.buildHelper = buildHelper;
        this.deviceHelper = deviceHelper;
        this.storage = storage;
    }

    void checkForForcedUpdate(Configuration configuration) {
        if (configuration.isSelfDestruct()) {
            storage.storeForceUpdateVersion(deviceHelper.getAppVersionCode());
            publishForceUpdateEvent();
        } else {
            storage.clearForceUpdateVersion();
        }
    }

    public void checkPendingForcedUpdate() {
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

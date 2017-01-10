package com.soundcloud.android.configuration;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurationManager {

    public static final String TAG = "Configuration";

    private final ConfigurationOperations configurationOperations;
    private final AccountOperations accountOperations;
    private final DeviceManagementStorage deviceManagementStorage;
    private final ForceUpdateHandler forceUpdateHandler;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public ConfigurationManager(ConfigurationOperations configurationOperations,
                                AccountOperations accountOperations,
                                DeviceManagementStorage deviceManagementStorage,
                                ForceUpdateHandler forceUpdateHandler) {
        this.configurationOperations = configurationOperations;
        this.accountOperations = accountOperations;
        this.deviceManagementStorage = deviceManagementStorage;
        this.forceUpdateHandler = forceUpdateHandler;
    }

    public void forceConfigurationUpdate() {
        Log.d(TAG, "Forcing configuration update");
        subscription.unsubscribe();
        subscription = configurationOperations.update().subscribe(new ConfigurationSubscriber());
    }

    void requestConfigurationUpdate() {
        Log.d(TAG, "Requesting configuration update");
        subscription.unsubscribe();
        subscription = configurationOperations.updateIfNecessary().subscribe(new ConfigurationSubscriber());
    }

    public void checkForForcedApplicationUpdate() {
        forceUpdateHandler.checkPendingForcedUpdate();
    }

    boolean isPendingDowngrade() {
        return configurationOperations.isPendingDowngrade();
    }

    boolean isPendingUpgrade() {
        return configurationOperations.isPendingUpgrade();
    }

    public boolean shouldDisplayDeviceConflict() {
        return deviceManagementStorage.hadDeviceConflict();
    }

    public void clearDeviceConflict() {
        deviceManagementStorage.clearDeviceConflict();
    }

    private class ConfigurationSubscriber extends DefaultSubscriber<Configuration> {
        @Override
        public void onNext(Configuration configuration) {
            Log.d(TAG, "Received new configuration");
            if (configuration.getDeviceManagement().isUnauthorized()) {
                Log.d(TAG, "Unauthorized device, logging out");
                deviceManagementStorage.setDeviceConflict();
                fireAndForget(accountOperations.logout());
            } else {
                configurationOperations.saveConfiguration(configuration);
            }
        }
    }

}

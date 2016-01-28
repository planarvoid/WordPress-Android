package com.soundcloud.android.configuration;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;

import android.util.Log;

import javax.inject.Inject;

public class ConfigurationManager {

    private static final String TAG = "Configuration";

    private final ConfigurationOperations configurationOperations;
    private final AccountOperations accountOperations;
    private final DeviceManagementStorage deviceManagementStorage;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public ConfigurationManager(ConfigurationOperations configurationOperations,
                                AccountOperations accountOperations,
                                DeviceManagementStorage deviceManagementStorage) {
        this.configurationOperations = configurationOperations;
        this.accountOperations = accountOperations;
        this.deviceManagementStorage = deviceManagementStorage;
    }

    public void update() {
        Log.d(TAG, "Requesting configuration");
        subscription.unsubscribe();
        subscription = configurationOperations.update().subscribe(new ConfigurationSubscriber());
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
            if (configuration.deviceManagement.isUnauthorized()) {
                Log.d(TAG, "Unauthorized device, logging out");
                deviceManagementStorage.setDeviceConflict();
                fireAndForget(accountOperations.logout());
            } else {
                configurationOperations.saveConfiguration(configuration);
            }
        }
    }

}

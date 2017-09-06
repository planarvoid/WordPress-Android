package com.soundcloud.android.configuration;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import rx.Subscription;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurationManager {

    public static final String TAG = "Configuration";

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

    public void forceConfigurationUpdate() {
        Log.d(TAG, "Forcing configuration fetch");
        subscription.unsubscribe();
        subscription = configurationOperations.fetch().subscribe(new ConfigurationSubscriber());
    }

    void requestConfigurationUpdate() {
        Log.d(TAG, "Requesting configuration fetch");
        subscription.unsubscribe();
        subscription = configurationOperations.fetchIfNecessary().subscribe(new ConfigurationSubscriber());
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
                accountOperations.logout().subscribe(new DefaultCompletableObserver());
            } else {
                configurationOperations.saveConfiguration(configuration);
            }
        }
    }

}

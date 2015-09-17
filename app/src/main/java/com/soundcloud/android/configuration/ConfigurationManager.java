package com.soundcloud.android.configuration;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

import android.util.Log;

import javax.inject.Inject;
import java.util.List;

public class ConfigurationManager {

    private static final String TAG = "Configuration";

    private final ConfigurationOperations configurationOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final AccountOperations accountOperations;
    private final DeviceManagementStorage deviceManagementStorage;

    private Subscription subscription = RxUtils.invalidSubscription();

    private final Func1<Void, Observable<List<Urn>>> clearOfflineContent = new Func1<Void, Observable<List<Urn>>>() {
        @Override
        public Observable<List<Urn>> call(Void ignore) {
            return offlineContentOperations.clearOfflineContent();
        }
    };

    @Inject
    public ConfigurationManager(ConfigurationOperations configurationOperations,
                                OfflineContentOperations offlineContentOperations, AccountOperations accountOperations,
                                DeviceManagementStorage deviceManagementStorage) {
        this.configurationOperations = configurationOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.accountOperations = accountOperations;
        this.deviceManagementStorage = deviceManagementStorage;
    }

    public void update() {
        Log.d(TAG, "Requesting configuration");
        subscription.unsubscribe();
        subscription = configurationOperations.update().subscribe(new ConfigurationSubscriber());
    }

    public void updateUntilPlanChanged() {
        Log.d(TAG, "Polling for plan update");
        subscription.unsubscribe();
        subscription = configurationOperations.updateUntilPlanChanged().subscribe(new ConfigurationSubscriber());
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
            if (configuration.deviceManagement.isNotAuthorized()) {
                Log.d(TAG, "Unauthorized device, logging out");
                deviceManagementStorage.setDeviceConflict();
                fireAndForget(accountOperations.logout().flatMap(clearOfflineContent));
            } else {
                configurationOperations.saveConfiguration(configuration);
            }
        }
    }

}

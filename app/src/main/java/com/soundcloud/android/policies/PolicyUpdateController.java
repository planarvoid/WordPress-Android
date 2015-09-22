package com.soundcloud.android.policies;

import static com.soundcloud.android.offline.OfflineContentService.TAG;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateController extends DefaultActivityLightCycle<AppCompatActivity> {

    static final int OFFLINE_DAYS_WARNING_THRESHOLD = 27;
    static final int OFFLINE_DAYS_ERROR_THRESHOLD = 30;

    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PolicySettingsStorage policySettingsStorage;
    private final DateProvider dateProvider;
    private final GoBackOnlineDialogPresenter goBackOnlineDialogPresenter;
    private final NetworkConnectionHelper connectionHelper;

    @Inject
    public PolicyUpdateController(FeatureOperations featureOperations,
                                  OfflineContentOperations offlineContentOperations,
                                  PolicySettingsStorage policySettingsStorage,
                                  CurrentDateProvider dateProvider,
                                  GoBackOnlineDialogPresenter goBackOnlineDialogPresenter,
                                  NetworkConnectionHelper connectionHelper) {
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.policySettingsStorage = policySettingsStorage;
        this.dateProvider = dateProvider;
        this.goBackOnlineDialogPresenter = goBackOnlineDialogPresenter;
        this.connectionHelper = connectionHelper;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (featureOperations.isOfflineContentEnabled()) {

            if (shouldCheckPolicyUpdates()) {
                Log.d(TAG, "No policy update in a least 27 days");
                long lastPolicyUpdate = policySettingsStorage.getPolicyUpdateTime();

                if (shouldNotifyUser(lastPolicyUpdate)) {
                    goBackOnlineDialogPresenter.show(activity, lastPolicyUpdate);
                    policySettingsStorage.setLastPolicyCheckTime(dateProvider.getCurrentTime());

                    if (shouldDeleteOfflineContent(lastPolicyUpdate)) {
                        Log.d(TAG, "No policy update in last 30 days");
                        fireAndForget(offlineContentOperations.clearOfflineContent());
                    }
                }
            }
        }
    }

    private boolean shouldCheckPolicyUpdates() {
        long lastShownTimeStamp = policySettingsStorage.getLastPolicyCheckTime();
        final long timeElapsed = dateProvider.getCurrentTime() - lastShownTimeStamp;
        return TimeUnit.MILLISECONDS.toDays(timeElapsed) > 0;
    }

    private boolean shouldNotifyUser(Long lastUpdate) {
        // this is required because policy updates are scheduled by alarm manager,
        // user can be already online but the policy update hasn't run yets
        if (!connectionHelper.isNetworkConnected()) {
            final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
            return daysElapsed >= OFFLINE_DAYS_WARNING_THRESHOLD;
        }
        return false;
    }

    private boolean shouldDeleteOfflineContent(Long lastUpdate) {
        final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
        return daysElapsed >= OFFLINE_DAYS_ERROR_THRESHOLD;
    }
}

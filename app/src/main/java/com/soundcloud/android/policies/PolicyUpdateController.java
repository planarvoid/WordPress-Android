package com.soundcloud.android.policies;

import static com.soundcloud.android.offline.OfflineContentService.TAG;

import com.soundcloud.android.Consts;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateController extends DefaultActivityLightCycle<AppCompatActivity> {

    static final int OFFLINE_DAYS_ERROR_THRESHOLD = 30;
    private static final int OFFLINE_DAYS_WARNING_THRESHOLD = 27;

    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final PolicyOperations policyOperations;
    private final PolicySettingsStorage policySettingsStorage;
    private final DateProvider dateProvider;
    private final GoBackOnlineDialogPresenter goBackOnlineDialogPresenter;
    private final NetworkConnectionHelper connectionHelper;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public PolicyUpdateController(FeatureOperations featureOperations,
                                  OfflineContentOperations offlineContentOperations,
                                  PolicyOperations policyOperations,
                                  PolicySettingsStorage policySettingsStorage,
                                  CurrentDateProvider dateProvider,
                                  GoBackOnlineDialogPresenter goBackOnlineDialogPresenter,
                                  NetworkConnectionHelper connectionHelper) {
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.policyOperations = policyOperations;
        this.policySettingsStorage = policySettingsStorage;
        this.dateProvider = dateProvider;
        this.goBackOnlineDialogPresenter = goBackOnlineDialogPresenter;
        this.connectionHelper = connectionHelper;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (featureOperations.isOfflineContentEnabled()) {

            if (shouldCheckPolicyUpdates()) {
                subscription.unsubscribe();
                subscription = policyOperations
                        .getMostRecentPolicyUpdateTimestamp()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ShouldNotifyUserSubscriber(activity));
            }
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();

        super.onDestroy(activity);
    }

    private boolean shouldCheckPolicyUpdates() {
        long lastShownTimeStamp = policySettingsStorage.getLastPolicyCheckTime();
        final long timeElapsed = dateProvider.getCurrentTime() - lastShownTimeStamp;
        Log.d(TAG, "Last valid policy check was: " + TimeUnit.MILLISECONDS.toDays(timeElapsed) + " days ago");
        return TimeUnit.MILLISECONDS.toDays(timeElapsed) > 0;
    }

    private class ShouldNotifyUserSubscriber extends DefaultSubscriber<Long> {

        private AppCompatActivity activity;

        ShouldNotifyUserSubscriber(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onNext(Long lastPolicyUpdate) {
            if (shouldNotifyUser(lastPolicyUpdate)) {
                goBackOnlineDialogPresenter.show(activity, lastPolicyUpdate);

                if (shouldDeleteOfflineContent(lastPolicyUpdate)) {
                    Log.d(TAG, "No policy update in last 30 days");
                    fireAndForget(offlineContentOperations.clearOfflineContent());
                }
            }
        }
    }

    private boolean shouldNotifyUser(Long lastUpdate) {
        // this is required because policy updates are scheduled by alarm manager,
        // user can be already online but the policy update hasn't run yet
        if (lastUpdate != Consts.NOT_SET) {
            policySettingsStorage.setLastPolicyCheckTime(dateProvider.getCurrentTime());

            if (!connectionHelper.isNetworkConnected()) {
                final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
                Log.d(TAG, "Days elapsed since last update: " + daysElapsed);
                return daysElapsed >= OFFLINE_DAYS_WARNING_THRESHOLD;
            }
        }
        return false;
    }

    private boolean shouldDeleteOfflineContent(Long lastUpdate) {
        final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
        return daysElapsed >= OFFLINE_DAYS_ERROR_THRESHOLD;
    }
}

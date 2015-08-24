package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentService.*;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Log;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class PolicyUpdateController extends DefaultActivityLightCycle<AppCompatActivity> {

    static final int OFFLINE_DAYS_WARNING_THRESHOLD = 27;
    static final int OFFLINE_DAYS_ERROR_THRESHOLD = 30;

    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final DateProvider dateProvider;
    private final GoBackOnlineDialogPresenter goBackOnlineDialogPresenter;
    private final Context context;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public PolicyUpdateController(Context context, FeatureOperations featureOperations,
                                  OfflineContentOperations offlineContentOperations,
                                  OfflineSettingsStorage offlineSettingsStorage,
                                  DateProvider dateProvider,
                                  GoBackOnlineDialogPresenter goBackOnlineDialogPresenter) {
        this.context = context;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.dateProvider = dateProvider;
        this.goBackOnlineDialogPresenter = goBackOnlineDialogPresenter;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (shouldCheckStalePolicies()) {
            subscription = offlineContentOperations
                    .tryToUpdateAndLoadLastPoliciesUpdateTime()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ShouldNotifyToGoOnlineSubscriber(activity));
        }
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.unsubscribe();
    }

    public boolean shouldCheckStalePolicies() {
        final long timeElapsed = dateProvider.getCurrentTime() - offlineSettingsStorage.getPolicyUpdateCheckTime();
        final long daysSinceLastCheck = TimeUnit.MILLISECONDS.toDays(timeElapsed);
        return featureOperations.isOfflineContentEnabled() && daysSinceLastCheck > 0;
    }

    private class ShouldNotifyToGoOnlineSubscriber extends DefaultSubscriber<Long> {
        private final Activity activity;

        public ShouldNotifyToGoOnlineSubscriber(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onNext(Long lastPolicyUpdateDate) {
            if (shouldNotifyUser(lastPolicyUpdateDate)) {
                Log.d(TAG, "No policy update in last 27 days");
                goBackOnlineDialogPresenter.show(activity, lastPolicyUpdateDate);

                if (shouldDeleteOfflineContent(lastPolicyUpdateDate)) {
                    Log.d(TAG, "No policy update in last 30 days");
                    fireAndForget(offlineContentOperations.clearOfflineContent());
                }
            }
        }

        private boolean shouldNotifyUser(Long lastUpdate) {
            final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
            return daysElapsed >= OFFLINE_DAYS_WARNING_THRESHOLD;
        }

        private boolean shouldDeleteOfflineContent(Long lastUpdate) {
            final long daysElapsed = TimeUnit.MILLISECONDS.toDays(dateProvider.getCurrentTime() - lastUpdate);
            return daysElapsed >= OFFLINE_DAYS_ERROR_THRESHOLD;
        }

        @Override
        public void onCompleted() {
            offlineSettingsStorage.setPolicyUpdateCheckTime(dateProvider.getCurrentTime());
        }
    }
}

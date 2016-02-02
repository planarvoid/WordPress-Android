package com.soundcloud.android.upgrade;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.LoadingButtonLayout;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class OfflineOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Navigator navigator;
    private final UpgradeProgressOperations upgradeProgressOperations;
    private Subscription subscription = RxUtils.invalidSubscription();
    private boolean isLoaded = false;
    private AppCompatActivity activity;

    @Bind(R.id.btn_go_setup_offline) LoadingButtonLayout setUpOfflineButton;
    @Bind(R.id.btn_go_setup_later) LoadingButtonLayout skipOfflineButton;

    @Inject
    OfflineOnboardingPresenter(Navigator navigator, UpgradeProgressOperations upgradeProgressOperations) {
        this.navigator = navigator;
        this.upgradeProgressOperations = upgradeProgressOperations;
    }

    @VisibleForTesting
    OfflineOnboardingPresenter(Navigator navigator, UpgradeProgressOperations upgradeProgressOperations,
                               LoadingButtonLayout setUpOfflineButton, LoadingButtonLayout skipOfflineButton) {
        this(navigator, upgradeProgressOperations);
        this.setUpOfflineButton = setUpOfflineButton;
        this.skipOfflineButton = skipOfflineButton;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        ButterKnife.bind(this, activity);
        awaitAccountUpgrade();
    }

    @VisibleForTesting
    void awaitAccountUpgrade() {
        subscription = upgradeProgressOperations.awaitAccountUpgrade()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new UpgradeCompleteSubscriber());
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        this.activity = null;
    }

    @OnClick(R.id.btn_go_setup_offline)
    void onSetupOfflineClicked(){
        if (isLoaded) {
            goToOfflineOnboarding();
        } else {
            setUpOfflineButton.setWaiting();
            skipOfflineButton.setEnabled(false);
        }
    }

    @OnClick(R.id.btn_go_setup_later)
    void onSetupLaterClicked() {
        if (isLoaded) {
            goToStream();
        } else {
            skipOfflineButton.setWaiting();
            setUpOfflineButton.setEnabled(false);
        }
    }

    private void goToStream() {
        navigator.openHome(activity);
    }

    private void goToOfflineOnboarding() {
        // TODO
    }

    private class UpgradeCompleteSubscriber extends DefaultSubscriber<Object> {

        @Override
        public void onCompleted() {
            isLoaded = true;
            if (setUpOfflineButton.isWaiting()) {
                goToOfflineOnboarding();
            } else if (skipOfflineButton.isWaiting()) {
                goToStream();
            }
        }
    }
}

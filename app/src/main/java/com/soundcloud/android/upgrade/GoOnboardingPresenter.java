package com.soundcloud.android.upgrade;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.LoadingButtonLayout;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

class GoOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Navigator navigator;
    private final UpgradeProgressOperations upgradeProgressOperations;
    private Subscription subscription = RxUtils.invalidSubscription();
    private boolean isLoaded = false;
    private AppCompatActivity activity;

    @Bind(R.id.btn_go_setup_offline) LoadingButtonLayout setUpOfflineButton;
    @Bind(R.id.btn_go_setup_later) LoadingButtonLayout setUpLaterButton;

    @Inject
    GoOnboardingPresenter(Navigator navigator, UpgradeProgressOperations upgradeProgressOperations) {
        this.navigator = navigator;
        this.upgradeProgressOperations = upgradeProgressOperations;
    }

    @VisibleForTesting
    GoOnboardingPresenter(Navigator navigator, UpgradeProgressOperations upgradeProgressOperations,
                          LoadingButtonLayout setUpOfflineButton, LoadingButtonLayout setUpLaterButton) {
        this(navigator, upgradeProgressOperations);
        this.setUpOfflineButton = setUpOfflineButton;
        this.setUpLaterButton = setUpLaterButton;
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
            setUpLaterButton.setEnabled(false);
        }
    }

    @OnClick(R.id.btn_go_setup_later)
    void onSetupLaterClicked() {
        if (isLoaded) {
            goToStream();
        } else {
            setUpLaterButton.setWaiting();
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
            } else if (setUpLaterButton.isWaiting()) {
                goToStream();
            }
        }
    }
}

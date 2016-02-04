package com.soundcloud.android.upgrade;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButtonLayout;

import android.app.Activity;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

class GoOnboardingView {

    @Bind(R.id.btn_go_setup_offline) LoadingButtonLayout setUpOfflineButton;
    @Bind(R.id.btn_go_setup_later) LoadingButtonLayout setUpLaterButton;
    private GoOnboardingPresenter presenter;

    @Inject
    public GoOnboardingView() {
    }

    void bind(Activity activity, GoOnboardingPresenter presenter) {
        this.presenter = presenter;
        ButterKnife.bind(this, activity);
    }

    @OnClick(R.id.btn_go_setup_offline)
    void onSetupOfflineClicked() {
        presenter.onSetupOfflineClicked();
    }

    @OnClick(R.id.btn_go_setup_later)
    void onSetupLaterClicked() {
        presenter.onSetupLaterClicked();
    }

    void setSetUpOfflineButtonWaiting() {
        setUpLaterButton.setEnabled(false);
        setUpOfflineButton.setEnabled(false);
        setUpOfflineButton.setWaiting();
    }

    void setSetUpOfflineButtonRetry() {
        setUpLaterButton.setEnabled(true);
        setUpOfflineButton.setEnabled(true);
        setUpOfflineButton.setRetry();
    }

    void setSetUpLaterButtonWaiting() {
        setUpOfflineButton.setEnabled(false);
        setUpOfflineButton.setEnabled(false);
        setUpLaterButton.setWaiting();
    }

    void setSetUpLaterButtonRetry() {
        setUpOfflineButton.setEnabled(true);
        setUpOfflineButton.setEnabled(true);
        setUpLaterButton.setRetry();
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

}

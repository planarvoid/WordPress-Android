package com.soundcloud.android.upgrade;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButton;

import android.app.Activity;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

class GoOnboardingView {

    @Bind(R.id.btn_go_setup_offline) LoadingButton setUpOfflineButton;
    @Bind(R.id.btn_go_setup_later) LoadingButton setUpLaterButton;
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

    void reset() {
        setEnabled(true);
        setUpLaterButton.setLoading(false);
        setUpOfflineButton.setLoading(false);
    }

    void setSetUpOfflineButtonWaiting() {
        setUpLaterButton.setEnabled(false);
        setUpOfflineButton.setEnabled(false);
        setUpOfflineButton.setLoading(true);
    }

    void setSetUpOfflineButtonRetry() {
        setEnabled(true);
        setUpOfflineButton.setRetry();
    }

    void setSetUpLaterButtonWaiting() {
        setEnabled(false);
        setUpLaterButton.setLoading(true);
    }

    void setSetUpLaterButtonRetry() {
        setEnabled(true);
        setUpLaterButton.setRetry();
    }

    private void setEnabled(boolean isEnabled) {
        setUpLaterButton.setEnabled(isEnabled);
        setUpOfflineButton.setEnabled(isEnabled);
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

}

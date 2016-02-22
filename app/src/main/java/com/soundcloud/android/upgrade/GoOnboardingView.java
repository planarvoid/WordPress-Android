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

    @Bind(R.id.btn_go_setup_start) LoadingButton setUpOfflineButton;
    private GoOnboardingPresenter presenter;

    @Inject
    public GoOnboardingView() {
    }

    void bind(Activity activity, GoOnboardingPresenter presenter) {
        this.presenter = presenter;
        ButterKnife.bind(this, activity);
    }

    @OnClick(R.id.btn_go_setup_start)
    void onSetupOfflineClicked() {
        presenter.onSetupOfflineClicked();
    }

    void reset() {
        setEnabled(true);
        setUpOfflineButton.setLoading(false);
    }

    void setSetUpOfflineButtonWaiting() {
        setUpOfflineButton.setEnabled(false);
        setUpOfflineButton.setLoading(true);
    }

    void setSetUpOfflineButtonRetry() {
        setEnabled(true);
        setUpOfflineButton.setRetry();
    }

    private void setEnabled(boolean isEnabled) {
        setUpOfflineButton.setEnabled(isEnabled);
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

}

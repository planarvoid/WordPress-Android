package com.soundcloud.android.downgrade;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.upgrade.UnrecoverableErrorDialog;
import com.soundcloud.android.view.LoadingButton;

import android.app.Activity;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

class GoOffboardingView {

    @BindView(R.id.btn_offboarding_resubscribe) LoadingButton resubscribeButton;
    @BindView(R.id.btn_offboarding_continue) LoadingButton continueButton;
    private GoOffboardingPresenter presenter;
    private Unbinder unbinder;

    @Inject
    public GoOffboardingView() {
    }

    void bind(Activity activity, GoOffboardingPresenter presenter) {
        this.presenter = presenter;
        unbinder = ButterKnife.bind(this, activity);
    }

    void unbind() {
        unbinder.unbind();
    }

    @OnClick(R.id.btn_offboarding_resubscribe)
    void onResubscribeClicked() {
        presenter.onResubscribeClicked();
    }

    @OnClick(R.id.btn_offboarding_continue)
    void onContinueClicked() {
        presenter.onContinueClicked();
    }

    void reset() {
        setEnabled(true);
        continueButton.setLoading(false);
        resubscribeButton.setLoading(false);
    }

    void setResubscribeButtonWaiting() {
        continueButton.setEnabled(false);
        resubscribeButton.setEnabled(false);
        resubscribeButton.setLoading(true);
    }

    void setResubscribeButtonRetry() {
        setEnabled(true);
        resubscribeButton.setRetry();
    }

    void setContinueButtonWaiting() {
        setEnabled(false);
        continueButton.setLoading(true);
    }

    void setContinueButtonRetry() {
        setEnabled(true);
        continueButton.setRetry();
    }

    private void setEnabled(boolean isEnabled) {
        continueButton.setEnabled(isEnabled);
        resubscribeButton.setEnabled(isEnabled);
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

}

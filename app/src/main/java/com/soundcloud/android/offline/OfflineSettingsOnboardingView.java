package com.soundcloud.android.offline;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;

import android.app.Activity;

import javax.inject.Inject;

class OfflineSettingsOnboardingView {

    private OfflineSettingsOnboardingPresenter presenter;

    @Inject
    public OfflineSettingsOnboardingView() {
    }

    void bind(Activity activity, OfflineSettingsOnboardingPresenter presenter) {
        this.presenter = presenter;
        ButterKnife.bind(this, activity);
    }

    @OnClick(R.id.btn_continue)
    void onContinue() {
        presenter.onContinue();
    }
}

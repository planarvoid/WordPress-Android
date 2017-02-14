package com.soundcloud.android.downgrade;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.upgrade.UnrecoverableErrorDialog;
import com.soundcloud.android.view.LoadingButton;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.widget.TextView;

import javax.inject.Inject;

class GoOffboardingView {

    @BindView(R.id.offboarding_primary_text) TextView title;
    @BindView(R.id.offboarding_secondary_text) TextView description;
    @BindView(R.id.btn_offboarding_resubscribe) LoadingButton resubscribeButton;
    @BindView(R.id.btn_offboarding_continue) LoadingButton continueButton;

    private final FeatureFlags flags;

    private GoOffboardingPresenter presenter;
    private Unbinder unbinder;

    @Inject
    GoOffboardingView(FeatureFlags flags) {
        this.flags = flags;
    }

    void bind(Activity activity, GoOffboardingPresenter presenter, Plan plan) {
        this.presenter = presenter;
        unbinder = ButterKnife.bind(this, activity);
        configureCopy(plan);
    }

    private void configureCopy(Plan plan) {
        title.setText(plan == Plan.MID_TIER
                      ? R.string.go_offboard_to_mid_title
                      : R.string.go_offboard_to_free_title);
        description.setText(plan == Plan.MID_TIER
                            ? R.string.go_offboard_to_mid_description
                            : adaptDescriptionForLegacyPlanNaming());
    }

    @StringRes
    private int adaptDescriptionForLegacyPlanNaming() {
        return flags.isEnabled(Flag.MID_TIER_ROLLOUT)
                ? R.string.go_offboard_to_free_description
                : R.string.go_offboard_to_free_description_legacy;
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

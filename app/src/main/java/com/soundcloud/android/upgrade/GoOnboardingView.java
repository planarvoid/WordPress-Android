package com.soundcloud.android.upgrade;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButton;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class GoOnboardingView implements ViewPager.OnPageChangeListener {

    @Bind(R.id.go_onboarding_pager) ViewPager pager;
    @Bind(R.id.go_onboarding_indicator) CirclePageIndicator indicator;
    @Bind(R.id.btn_go_setup_start) LoadingButton doneButton;

    private GoOnboardingPresenter presenter;
    private final GoOnboardingAdapter adapter;

    @Inject
    public GoOnboardingView(GoOnboardingAdapter adapter) {
        this.adapter = adapter;
    }

    void bind(Activity activity, GoOnboardingPresenter presenter) {
        this.presenter = presenter;
        ButterKnife.bind(this, activity);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        pager.addOnPageChangeListener(this);
    }

    @OnClick(R.id.btn_go_setup_start)
    void onSetupOfflineClicked() {
        presenter.onSetupOfflineClicked();
    }

    void reset() {
        setEnabled(true);
        doneButton.setLoading(false);
    }

    void setSetUpOfflineButtonWaiting() {
        doneButton.setEnabled(false);
        doneButton.setLoading(true);
    }

    void setSetUpOfflineButtonRetry() {
        setEnabled(true);
        doneButton.setRetry();
    }

    private void setEnabled(boolean isEnabled) {
        doneButton.setEnabled(isEnabled);
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        doneButton.setBackgroundResource(isLastPage(position)
                ? R.drawable.btn_primary
                : R.drawable.btn_primary_transparent);
    }

    private boolean isLastPage(int position) {
        return position == pager.getAdapter().getCount() - 1;
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

}

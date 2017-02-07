package com.soundcloud.android.upgrade;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButton;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;

import android.app.Activity;
import android.graphics.drawable.TransitionDrawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class GoOnboardingView implements ViewPager.OnPageChangeListener {

    private static final int BUTTON_TRANSITION_MS = 200;

    @BindView(R.id.go_onboarding_pager) ViewPager pager;
    @BindView(R.id.go_onboarding_indicator) CirclePageIndicator indicator;
    @BindView(R.id.btn_go_setup_start) LoadingButton doneButton;

    private GoOnboardingPresenter presenter;
    private final GoOnboardingAdapter adapter;

    @Inject
    GoOnboardingView(GoOnboardingAdapter adapter) {
        this.adapter = adapter;
    }

    void bind(Activity activity, GoOnboardingPresenter presenter) {
        this.presenter = presenter;
        ButterKnife.bind(this, activity);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        pager.addOnPageChangeListener(this);
        pager.addOnPageChangeListener(adapter);
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
        if (isLastPage(position)) {
            doneButton.setBackgroundResource(R.drawable.btn_primary_transition);
            final TransitionDrawable transition = (TransitionDrawable) doneButton.getBackground();
            transition.startTransition(BUTTON_TRANSITION_MS);
        } else {
            doneButton.setBackgroundResource(R.drawable.btn_transparent);
        }
    }

    private boolean isLastPage(int position) {
        return position == pager.getAdapter().getCount() - 1;
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

}

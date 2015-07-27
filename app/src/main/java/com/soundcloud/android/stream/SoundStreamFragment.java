package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SoundStreamFragment extends LightCycleSupportFragment implements RefreshableScreen {

    @VisibleForTesting
    static final String ONBOARDING_RESULT_EXTRA = "onboarding.result";

    @Inject @LightCycle SoundStreamPresenter presenter;

    public static SoundStreamFragment create(boolean onboardingSucceeded) {
        final Bundle args = new Bundle();
        args.putBoolean(ONBOARDING_RESULT_EXTRA, onboardingSucceeded);
        SoundStreamFragment fragment = new SoundStreamFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SoundStreamFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter.setOnboardingSuccess(getArguments().getBoolean(ONBOARDING_RESULT_EXTRA));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}

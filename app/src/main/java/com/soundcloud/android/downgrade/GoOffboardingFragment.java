package com.soundcloud.android.downgrade;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class GoOffboardingFragment extends LightCycleSupportFragment {

    @Inject @LightCycle GoOffboardingPresenter presenter;

    public GoOffboardingFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    void enterScreen() {
        presenter.trackResubscribeButtonImpression();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.go_offboarding_fragment, container, false);
    }
}

package com.soundcloud.android.analytics;

import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

// When used, we plan on moving this to a base fragment implementation
public class FragmentReferringEventProvider extends DefaultSupportFragmentLightCycle<Fragment> {
    private final ReferringEventProvider referringEventProvider;

    @Inject
    public FragmentReferringEventProvider(ReferringEventProvider referringEventProvider) {
        this.referringEventProvider = referringEventProvider;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            referringEventProvider.setupReferringEvent();
        } else {
            referringEventProvider.restoreReferringEvent(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) {
        referringEventProvider.saveReferringEvent(bundle);
    }
}

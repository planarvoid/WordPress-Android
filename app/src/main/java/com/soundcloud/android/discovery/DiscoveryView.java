package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class DiscoveryView extends DefaultSupportFragmentLightCycle<Fragment> {

    @Inject
    public DiscoveryView() {
    }

    @Override
    public void onViewCreated(final Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        ButterKnife.inject(this, view);

        // this is for behind the cards. should figure out another way to set this
        view.setBackgroundColor(fragment.getResources().getColor(R.color.card_list_background));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        ButterKnife.reset(this);
        super.onDestroyView(fragment);
    }
}

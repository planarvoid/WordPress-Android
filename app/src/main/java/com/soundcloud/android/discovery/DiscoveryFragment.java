package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.ScrollContent;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class DiscoveryFragment extends LightCycleSupportFragment<DiscoveryFragment> implements ScrollContent {

    @Inject @LightCycle DiscoveryPresenter presenter;

    public DiscoveryFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.recyclerview_with_emptyview, container, false);
        fragmentView.setBackgroundColor(getResources().getColor(R.color.page_background));
        return fragmentView;
    }

    @Override
    public void resetScroll() {
        presenter.scrollToTop();
    }
}

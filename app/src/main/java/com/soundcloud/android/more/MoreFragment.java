package com.soundcloud.android.more;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.MainPagerAdapter;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class MoreFragment extends LightCycleSupportFragment<MoreFragment> implements MainPagerAdapter.ScrollContent, MainPagerAdapter.FocusListener {

    @Inject @LightCycle MoreTabPresenter presenter;

    public MoreFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public void resetScroll() {
        presenter.resetScroll();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.more, container, false);
    }

    @Override
    public void onFocusChange(boolean hasFocus) {
        presenter.onFocusChange(hasFocus);
    }

}

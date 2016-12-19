package com.soundcloud.android.main;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

public class EnterScreenDispatcher extends ActivityLightCycleDispatcher<RootActivity> implements ViewPager.OnPageChangeListener {
    private RootActivity activity;

    public interface Listener {
        void onEnterScreen(RootActivity activity);
    }

    @LightCycle final ScreenStateProvider screenStateProvider;
    private Optional<Listener> listener = Optional.absent();

    @Inject
    public EnterScreenDispatcher(ScreenStateProvider screenStateProvider) {
        this.screenStateProvider = screenStateProvider;
    }

    public void setListener(Listener listener) {
        this.listener = Optional.of(listener);
    }

    @Override
    public void onResume(RootActivity activity) {
        super.onResume(activity);

        this.activity = activity;

        if (listener.isPresent() && screenStateProvider.isEnteringScreen()) {
            listener.get().onEnterScreen(activity);
        }
    }

    @Override
    public void onPause(RootActivity activity) {
        super.onPause(activity);

        this.activity = null;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (listener.isPresent() && activity != null) {
            listener.get().onEnterScreen(activity);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}

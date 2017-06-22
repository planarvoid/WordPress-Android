package com.soundcloud.android.main;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

public class EnterScreenDispatcher extends ActivityLightCycleDispatcher<RootActivity> implements ViewPager.OnPageChangeListener {

    @LightCycle final ScreenStateProvider screenStateProvider;
    private final BehaviorSubject<Long> enterScreen = BehaviorSubject.create();

    private RootActivity activity;
    private Optional<Listener> listener = Optional.absent();

    public interface Listener {

        void onEnterScreen(RootActivity activity);
    }

    public Observable<Long> enterScreenTimestamp() {
        return enterScreen;
    }

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
        enterScreen.onNext(System.currentTimeMillis());
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
        if (activity != null && listener.isPresent()) {
            listener.get().onEnterScreen(activity);
        }
        enterScreen.onNext(System.currentTimeMillis());
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}

package com.soundcloud.android.main;

import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import javax.inject.Inject;

public class EnterScreenDispatcher extends ActivityLightCycleDispatcher<RootActivity>  {

    @LightCycle final ScreenStateProvider screenStateProvider;
    private final BehaviorSubject<Optional<Long>> enterScreen = BehaviorSubject.create();
    private final BehaviorSubject<Long> pageSelected = BehaviorSubject.create();

    private RootActivity activity;
    private Optional<Listener> listener = Optional.absent();

    public interface Listener {
        void onReenterScreen(RootActivity activity);
        void onEnterScreen(RootActivity activity, int position);
    }

    public Observable<Long> enterScreenTimestamp() {
        return enterScreen
                .doOnNext(option -> option.ifPresent(value -> enterScreen.onNext(Optional.absent())))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public Observable<Long> pageSelectedTimestamp() {
        return pageSelected;
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
            listener.get().onReenterScreen(activity);
        }
        enterScreen.onNext(Optional.of(System.currentTimeMillis()));
        pageSelected.onNext(System.currentTimeMillis());
    }

    @Override
    public void onPause(RootActivity activity) {
        super.onPause(activity);

        this.activity = null;
    }

    public void onPageSelected(int position) {
        if (activity != null && listener.isPresent()) {
            listener.get().onEnterScreen(activity, position);
        }
        final long timestamp = System.currentTimeMillis();
        enterScreen.onNext(Optional.of(timestamp));
        pageSelected.onNext(timestamp);
    }

}

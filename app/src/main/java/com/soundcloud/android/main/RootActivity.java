package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsConnector;
import com.soundcloud.android.configuration.ConfigurationUpdateLightCycle;
import com.soundcloud.android.configuration.ForceUpdateLightCycle;
import com.soundcloud.android.configuration.experiments.ItalianExperiment;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.image.ImageOperationsController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleAppCompatActivity;
import com.soundcloud.lightcycle.LightCycles;
import io.reactivex.disposables.CompositeDisposable;
import rx.Observable;

import android.os.Bundle;

import javax.inject.Inject;

public abstract class RootActivity extends LightCycleAppCompatActivity<RootActivity> {

    @Inject @LightCycle ActivityLifeCyclePublisher lifeCyclePublisher;
    @Inject @LightCycle ActivityLifeCycleLogger lifeCycleLogger;
    @Inject @LightCycle AnalyticsConnector analyticsConnector;
    @Inject @LightCycle ImageOperationsController imageOperationsController;
    @Inject @LightCycle protected ScreenTracker screenTracker;
    @Inject @LightCycle ForceUpdateLightCycle forceUpdateLightCycle;
    @Inject ConfigurationUpdateLightCycle configurationUpdateLightCycle;
    @Inject ItalianExperiment italianExperiment;
    @Inject NavigationDelegate navigationDelegate;
    @Inject NavigationDelegate_ObserverFactory navigationDelegateObserverFactory;

    private final CompositeDisposable disposable = new CompositeDisposable();

    public RootActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        if (receiveConfigurationUpdates()) {
            bind(LightCycles.lift(configurationUpdateLightCycle));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        italianExperiment.configure(getResources());
        super.onCreate(savedInstanceState);
    }

    public abstract Screen getScreen();

    @Override
    protected void onResume() {
        super.onResume();
        disposable.add(navigationDelegate.listenToNavigation()
                                         .subscribeWith(navigationDelegateObserverFactory.create()));
    }

    @Override
    protected void onPause() {
        disposable.clear();
        super.onPause();
    }

    public Optional<ReferringEvent> getReferringEvent() {
        return screenTracker.referringEventProvider.getReferringEvent();
    }

    protected boolean receiveConfigurationUpdates() {
        return true;
    }

    public Observable<Void> enterScreen() {
        return screenTracker.enterScreen;
    }
}

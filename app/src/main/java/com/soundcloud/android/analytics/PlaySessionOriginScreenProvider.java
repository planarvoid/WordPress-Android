package com.soundcloud.android.analytics;

import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.disposables.CompositeDisposable;

import android.annotation.SuppressLint;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PlaySessionOriginScreenProvider {

    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private boolean wasUserInCollectionScreen;
    private Optional<String> recentlyPlayedOriginScreen = Optional.absent();

    @SuppressLint("sc.MissingCompositeDisposableRecycle") // disposable tied to app lifecycle
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public PlaySessionOriginScreenProvider(EventBus eventBus, ScreenProvider screenProvider) {
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
    }

    public void subscribe() {
        compositeDisposable.addAll(
                RxJavaInterop.toV2Observable(eventBus.queue(EventQueue.TRACKING))
                             .ofType(ScreenEvent.class)
                             .subscribeWith(new ScreenTrackingEventSubscriber()),
                RxJavaInterop.toV2Observable(eventBus.queue(EventQueue.TRACKING))
                             .ofType(CollectionEvent.class)
                             .subscribeWith(new CollectionEventSubscriber())
        );
    }

    public String getOriginScreen() {
        if (recentlyPlayedOriginScreen.isPresent()) {
            return recentlyPlayedOriginScreen.get();
        } else {
            return screenProvider.getLastScreen().get();
        }
    }

    private boolean isOneOfTheRecentlyPlayedEntitiesScreen(Screen screen) {
        return screen == Screen.PLAYLISTS || screen == Screen.STATIONS_INFO || screen == Screen.USER_MAIN;
    }

    private boolean userNavigatedViaRecentlyPlayedBucket(CollectionEvent event) {
        final Optional<DiscoverySource> discoverySourceOpt = event.source();
        return discoverySourceOpt.isPresent() && DiscoverySource.from(discoverySourceOpt.get().value()) == DiscoverySource.RECENTLY_PLAYED;
    }

    private class ScreenTrackingEventSubscriber extends DefaultObserver<ScreenEvent> {
        @Override
        public void onNext(ScreenEvent event) {
            final Screen screen = Screen.fromTag(event.screen());
            if (!wasUserInCollectionScreen || !isOneOfTheRecentlyPlayedEntitiesScreen(screen)) {
                wasUserInCollectionScreen = screen == Screen.COLLECTIONS;
                recentlyPlayedOriginScreen = Optional.absent();
            }
        }
    }

    private class CollectionEventSubscriber extends DefaultObserver<CollectionEvent> {
        @Override
        public void onNext(CollectionEvent event) {
            if (wasUserInCollectionScreen && userNavigatedViaRecentlyPlayedBucket(event)) {
                recentlyPlayedOriginScreen = Optional.of(event.pageName());
            } else {
                recentlyPlayedOriginScreen = Optional.absent();
            }
        }
    }
}

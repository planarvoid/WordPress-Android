package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class TrackPagerAdapter extends PagerAdapter implements CastConnectionHelper.OnConnectionChangeListener {

    static final int TRACK_VIEW_POOL_SIZE = 6;
    private static final int TYPE_TRACK_VIEW = 0;
    private static final int TYPE_AD_VIEW = 1;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final TrackRepository trackRepository;
    private final TrackPagePresenter trackPagePresenter;
    private final AdPagePresenter adPagePresenter;
    private final CastConnectionHelper castConnectionHelper;
    private final EventBus eventBus;
    private final TrackPageRecycler trackPageRecycler;
    private final Map<View, TrackPageData> trackByViews = new HashMap<>(TRACK_VIEW_POOL_SIZE);

    private View adView;
    private SkipListener skipListener;
    private List<TrackPageData> currentData = Collections.emptyList();
    private ViewVisibilityProvider viewVisibilityProvider;

    private CompositeSubscription foregroundSubscription = new CompositeSubscription();
    private CompositeSubscription backgroundSubscription = new CompositeSubscription();
    private boolean isForeground;

    // WeakHashSet, to avoid re-subscribing subscribed views without holding strong refs
    private final Set<View> subscribedTrackViews = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
        private final LruCache<Urn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<>(TRACK_CACHE_SIZE);

    private final Func1<PlaybackProgressEvent, Boolean> currentTrackFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return playQueueManager.isCurrentTrack(progressEvent.getTrackUrn());
        }
    };

    private final Action1<EntityStateChangedEvent> invalidateTrackCacheAction = new Action1<EntityStateChangedEvent>() {
        @Override
        public void call(EntityStateChangedEvent trackChangedEvent) {
            trackObservableCache.remove(trackChangedEvent.getFirstUrn());
        }
    };
    private static final Func1<PropertySet, PlayerItem> TO_PLAYER_AD = new Func1<PropertySet, PlayerItem>() {
        @Override
        public PlayerItem call(PropertySet propertySet) {
            return new PlayerAd(propertySet);
        }
    };
    private final Func1<PropertySet, PlayerTrackState> toPlayerTrack = new Func1<PropertySet, PlayerTrackState>() {
        @Override
        public PlayerTrackState call(PropertySet propertySet) {
            return new PlayerTrackState(propertySet,
                    playQueueManager.isCurrentTrack(propertySet.get(TrackProperty.URN)),
                    isForeground, viewVisibilityProvider
            );
        }
    };

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager,
                      PlaySessionStateProvider playSessionStateProvider,
                      TrackRepository trackRepository,
                      TrackPagePresenter trackPagePresenter,
                      AdPagePresenter adPagePresenter,
                      CastConnectionHelper castConnectionHelper,
                      EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adPagePresenter = adPagePresenter;
        this.castConnectionHelper = castConnectionHelper;
        this.eventBus = eventBus;
        this.trackPageRecycler = new TrackPageRecycler();
    }

    public void setCurrentData(List<TrackPageData> data) {
        currentData = data;
        notifyDataSetChanged();
    }

    public void onPlayerSlide(float slideOffset) {
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            getPresenter(entry.getValue()).onPlayerSlide(entry.getKey(), slideOffset);
        }
    }

    public int getPlayQueuePosition(int position) {
        return currentData.get(position).getPositionInPlayQueue();
    }

    public boolean isAudioAdAtPosition(int position) {
        return currentData.get(position).isAdPage();
    }

    void onPause() {
        isForeground = false;
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            getPresenter(entry.getValue()).onBackground(entry.getKey());
        }

        foregroundSubscription.unsubscribe();
        foregroundSubscription = new CompositeSubscription();
    }

    void onResume() {
        isForeground = true;
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            final TrackPageData trackPageData = entry.getValue();
            final PlayerPagePresenter presenter = getPresenter(trackPageData);
            final View view = entry.getKey();

            presenter.onForeground(view);
            subscribeToForegroundEvents(trackPageData, presenter, view);
        }
    }

    void onViewCreated(ViewGroup container, SkipListener skipListener, ViewVisibilityProvider viewVisibilityProvider) {
        this.skipListener = skipListener;
        this.viewVisibilityProvider = viewVisibilityProvider;
        for (int i = 0; i < TRACK_VIEW_POOL_SIZE; i++) {
            final View itemView = trackPagePresenter.createItemView(container, skipListener);
            trackPageRecycler.addScrapView(itemView);
        }
        castConnectionHelper.addOnConnectionChangeListener(this);
    }

    void onViewDestroyed() {
        backgroundSubscription.unsubscribe();
        backgroundSubscription = new CompositeSubscription();
        subscribedTrackViews.clear();
        castConnectionHelper.removeOnConnectionChangeListener(this);
    }

    public int getItemViewType(int position) {
        return currentData.get(position).isAdPage() ? TYPE_AD_VIEW : TYPE_TRACK_VIEW;
    }

    public int getItemViewTypeFromObject(Object object) {
        return trackPagePresenter.accept((View) object) ? TYPE_TRACK_VIEW : TYPE_AD_VIEW;
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        View view;
        if (getItemViewType(position) == TYPE_TRACK_VIEW) {
            view = instantiateTrackView(position);
        } else {
            view = instantiateAdView(container, position);
        }

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        container.removeView(view);

        if (getItemViewTypeFromObject(view) == TYPE_TRACK_VIEW) {
            final Urn trackUrn = trackByViews.get(view).getTrackUrn();
            trackPageRecycler.recyclePage(trackUrn, view);
            if (!playQueueManager.isCurrentTrack(trackUrn)) {
                trackPagePresenter.onBackground(view);
            }
        }

        trackByViews.remove(view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void onCastConnectionChange() {
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            getPresenter(entry.getValue()).setCastDeviceName(entry.getKey(), castConnectionHelper.getDeviceName());
        }
    }

    private View instantiateTrackView(int position) {
        View view;
        final TrackPageData trackPageData = currentData.get(position);
        Urn urn = trackPageData.getTrackUrn();

        if (trackPageRecycler.hasExistingPage(urn)){
            view = trackPageRecycler.removePageByUrn(urn);
            trackByViews.put(view, trackPageData);
            if (isForeground){
                trackPagePresenter.onForeground(view);
            } else {
                trackPagePresenter.onBackground(view);
            }
        } else {
            view = trackPageRecycler.getRecycledPage();
            bindView(position, view);
        }

        onTrackPageSet(view, position);
        return view;
    }

    private View instantiateAdView(ViewGroup container, int position) {
        if (adView == null) {
            adView = adPagePresenter.createItemView(container, skipListener);
        }
        bindView(position, adView);
        return adView;
    }

    private View bindView(int position, View view) {
        final TrackPageData trackPageData = currentData.get(position);
        trackByViews.put(view, trackPageData);

        final PlayerPagePresenter presenter = getPresenter(trackPageData);
        presenter.clearItemView(view);

        if (isForeground){
            // this will attach the cast button
            presenter.onForeground(view);
        }

        if (!subscribedTrackViews.contains(view)) {
            subscribeToEvents(presenter, view);
            subscribedTrackViews.add(view);
        }

        subscribeToForegroundEvents(trackPageData, presenter, view);
        return view;
    }

    private PlayerPagePresenter getPresenter(TrackPageData trackPageData) {
        return trackPageData.isAdPage() ? adPagePresenter : trackPagePresenter;
    }

    private Observable<? extends PlayerItem> getSoundObservable(TrackPageData viewData) {
        if (viewData.isAdPage()) {
            return getAdObservable(viewData.getTrackUrn(), viewData.getProperties())
                    .map(TO_PLAYER_AD);
        } else if (viewData.hasRelatedTrack()) {
            return Observable.zip(
                    getTrackObservable(viewData.getTrackUrn(), viewData.getProperties()).map(toPlayerTrack),
                    getTrackObservable(viewData.getRelatedTrackUrn()),
                    new Func2<PlayerTrackState, PropertySet, PlayerItem>() {
                        @Override
                        public PlayerItem call(PlayerTrackState playerTrackState, PropertySet propertySet) {
                            playerTrackState.setRelatedTrack(propertySet);
                            return playerTrackState;
                        }
                    }
            );
        } else {
            return getTrackObservable(viewData.getTrackUrn(), viewData.getProperties())
                    .map(toPlayerTrack);
        }
    }

    private Observable<PropertySet> getTrackObservable(Urn urn, final PropertySet adOverlayData) {
        return getTrackObservable(urn).doOnNext(new Action1<PropertySet>() {
            @Override
            public void call(PropertySet track) {
                adOverlayData.put(TrackProperty.URN, track.get(TrackProperty.URN))
                        .put(TrackProperty.TITLE, track.get(TrackProperty.TITLE))
                        .put(TrackProperty.CREATOR_NAME, track.get(TrackProperty.CREATOR_NAME));
            }
        });
    }

    private Observable<PropertySet> getAdObservable(Urn urn, final PropertySet audioAd) {
        // merge together audio ad track data and track data from the upcoming monetizable track
        return Observable.zip(getTrackObservable(urn), getTrackObservable(audioAd.get(AdProperty.MONETIZABLE_TRACK_URN)),
                new Func2<PropertySet, PropertySet, PropertySet>() {
                    @Override
                    public PropertySet call(PropertySet audioAdTrack, PropertySet monetizableTrack) {
                        return audioAdTrack.merge(audioAd)
                                .put(AdProperty.MONETIZABLE_TRACK_TITLE, monetizableTrack.get(PlayableProperty.TITLE))
                                .put(AdProperty.MONETIZABLE_TRACK_CREATOR, monetizableTrack.get(PlayableProperty.CREATOR_NAME));
                    }
                });
    }

    private void subscribeToEvents(PlayerPagePresenter presenter, final View trackPage) {
        backgroundSubscription.add(eventBus
                .subscribe(EventQueue.PLAYER_UI, new PlayerPanelSubscriber(presenter, trackPage)));
        backgroundSubscription.add(eventBus
                .queue(EventQueue.ENTITY_STATE_CHANGED)
                .filter(EntityStateChangedEvent.IS_TRACK_FILTER)
                .doOnNext(invalidateTrackCacheAction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new TrackChangedSubscriber(presenter, trackPage)));
        backgroundSubscription.add(eventBus
                .queue(EventQueue.PLAY_QUEUE_TRACK)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ClearAdOverlaySubscriber(presenter, trackPage)));
    }

    private void subscribeToForegroundEvents(TrackPageData data, PlayerPagePresenter presenter, final View trackPage) {
        foregroundSubscription.add(getSoundObservable(data)
                .filter(isTrackRelatedToView(trackPage))
                .subscribe(new TrackSubscriber(presenter, trackPage)));
        foregroundSubscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED,
                new PlaybackStateSubscriber(presenter, trackPage)));
        foregroundSubscription.add(eventBus
                .queue(EventQueue.PLAYBACK_PROGRESS)
                .filter(currentTrackFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaybackProgressSubscriber(presenter, trackPage)));
    }

    @NonNull
    private Func1<PlayerItem, Boolean> isTrackRelatedToView(final View trackPage) {
        return new Func1<PlayerItem, Boolean>() {
            @Override
            public Boolean call(PlayerItem playerItem) {
                return isTrackRelatedToView(trackPage, playerItem.getTrackUrn());
            }
        };
    }

    void onTrackChange() {
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            final View trackView = entry.getKey();
            if (getItemViewTypeFromObject(trackView) == TYPE_TRACK_VIEW) {
                Urn urn = entry.getValue().getTrackUrn();
                trackPagePresenter.onPageChange(trackView);
                updateProgress(trackPagePresenter, trackView, urn);
            }
        }
    }

    private void onTrackPageSet(View view, int position) {
        final TrackPageData trackPageData = currentData.get(position);
        trackPagePresenter.onPositionSet(view, position, currentData.size());
        trackPagePresenter.setCastDeviceName(view, castConnectionHelper.getDeviceName());
        if (trackPageData.hasAdOverlay()){
            trackPagePresenter.setAdOverlay(view, trackPageData.getProperties());
        } else {
            trackPagePresenter.clearAdOverlay(view);
        }
    }

    // Getter with side effects. We are forced to adjust our internal datasets based on position changes here.
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return currentData.size();
    }

    private Observable<PropertySet> getTrackObservable(Urn urn) {
        ReplaySubject<PropertySet> trackSubject = trackObservableCache.get(urn);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackRepository
                    .track(urn)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackSubject);
            trackObservableCache.put(urn, trackSubject);
        }
        return trackSubject;
    }

    private Boolean isTrackRelatedToView(View trackPage, Urn urn) {
        return trackByViews.containsKey(trackPage) && trackByViews.get(trackPage).getTrackUrn().equals(urn)
                || trackPageRecycler.isPageForUrn(trackPage, urn);
    }

    private void updateProgress(PlayerPagePresenter presenter, View trackView, Urn urn) {
        presenter.setProgress(trackView, playSessionStateProvider.getLastProgressForTrack(urn));
    }

    private static class TrackSubscriber extends DefaultSubscriber<PlayerItem> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public TrackSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlayerItem playerItem) {
            presenter.bindItemView(trackPage, playerItem);
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public PlaybackStateSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            final boolean viewPresentingCurrentTrack = trackByViews.containsKey(trackPage)
                    && stateTransition.getTrackUrn().equals(trackByViews.get(trackPage).getTrackUrn());

            presenter.setPlayState(trackPage, stateTransition, viewPresentingCurrentTrack, isForeground);
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public PlaybackProgressSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlaybackProgressEvent progress) {
            if (isTrackRelatedToView(trackPage, progress.getTrackUrn())) {
                presenter.setProgress(trackPage, progress.getPlaybackProgress());
            }
        }
    }

    private final class PlayerPanelSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public PlayerPanelSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlayerUIEvent event) {
            final int kind = event.getKind();
            if (kind == PlayerUIEvent.PLAYER_EXPANDED) {
                presenter.setExpanded(trackPage);
            } else if (kind == PlayerUIEvent.PLAYER_COLLAPSED) {
                presenter.setCollapsed(trackPage);
            }
        }
    }

    private final class TrackChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public TrackChangedSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(EntityStateChangedEvent event) {
            if (isTrackRelatedToView(trackPage, event.getFirstUrn())) {
                presenter.onPlayableUpdated(trackPage, event);
            }
        }
    }

    private final class ClearAdOverlaySubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent>{

        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public ClearAdOverlaySubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(CurrentPlayQueueTrackEvent args) {
            if (trackByViews.containsKey(trackPage) && !playQueueManager.isCurrentTrack(trackByViews.get(trackPage).getTrackUrn())) {
                presenter.clearAdOverlay(trackPage);
            }
        }
    }
}

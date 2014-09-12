package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

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

public class TrackPagerAdapter extends PagerAdapter {

    static final int TRACKVIEW_POOL_SIZE = 6;
    private static final int TYPE_TRACK_VIEW = 0;
    private static final int TYPE_AD_VIEW = 1;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final AdPagePresenter adPagePresenter;
    private final EventBus eventBus;
    private final FeatureFlags featureFlags;
    private final TrackPageRecycler trackPageRecycler;

    private View adView;
    private SkipListener skipListener;
    private List<TrackPageData> currentData = Collections.emptyList();

    // WeakHashSet, to avoid re-subscribing subscribed views without holding strong refs
    private final Set<View> subscribedTrackViews = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());

    private final LruCache<TrackUrn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<TrackUrn, ReplaySubject<PropertySet>>(TRACK_CACHE_SIZE);

    private final Map<View, TrackPageData> trackByViews = new HashMap<View, TrackPageData>(TRACKVIEW_POOL_SIZE);

    private final Func1<PlaybackProgressEvent, Boolean> currentTrackFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return playQueueManager.isCurrentTrack(progressEvent.getTrackUrn());
        }
    };

    private final Action1<PlayableUpdatedEvent> invalidateTrackCacheAction = new Action1<PlayableUpdatedEvent>() {
        @Override
        public void call(PlayableUpdatedEvent playableUpdatedEvent) {
            trackObservableCache.remove((TrackUrn) playableUpdatedEvent.getUrn());
        }
    };

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, PlaySessionStateProvider playSessionStateProvider,
                      TrackOperations trackOperations, TrackPagePresenter trackPagePresenter, AdPagePresenter adPagePresenter,
                      EventBus eventBus, FeatureFlags featureFlags) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adPagePresenter = adPagePresenter;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
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
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            getPresenter(entry.getValue()).onBackground(entry.getKey());
        }
    }

    void onResume() {
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            getPresenter(entry.getValue()).onForeground(entry.getKey());
        }
    }

    void unsubscribe() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription();
        subscribedTrackViews.clear();
    }

    void initialize(ViewGroup container, SkipListener skipListener) {
        this.skipListener = skipListener;
        for (int i = 0; i < TRACKVIEW_POOL_SIZE; i++) {
            final View itemView = trackPagePresenter.createItemView(container, skipListener);
            trackPageRecycler.addScrapView(itemView);
        }
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
            final TrackUrn trackUrn = trackByViews.get(view).getTrackUrn();
            trackPageRecycler.recyclePage(trackUrn, view);
            if (!playQueueManager.isCurrentTrack(trackUrn)) {
                trackPagePresenter.onBackground(view);
            }
        }

        trackByViews.remove(view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private View instantiateTrackView(int position) {
        View view;
        final TrackPageData trackPageData = currentData.get(position);
        TrackUrn urn = trackPageData.getTrackUrn();

        if (trackPageRecycler.hasExistingPage(urn)){
            view = trackPageRecycler.removePageByUrn(urn);
            trackByViews.put(view, trackPageData);
            trackPagePresenter.onForeground(view);
        } else {
            view = trackPageRecycler.getRecycledPage();
            bindView(position, view);
        }

        onPagePositionSet(view, position);
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

        if (!subscribedTrackViews.contains(view)) {
            subscribeToPlayEvents(presenter, view);
            subscribedTrackViews.add(view);
        }

        getSoundObservable(trackPageData).subscribe(new TrackSubscriber(presenter, view));
        return view;
    }

    private PlayerPagePresenter getPresenter(TrackPageData trackPageData) {
        return trackPageData.isAdPage() ? adPagePresenter : trackPagePresenter;
    }

    private Observable<PropertySet> getSoundObservable(TrackPageData viewData) {
        final Observable<PropertySet> trackObservable;
        if (viewData.isAdPage()) {
            trackObservable = getAdObservable(viewData.getTrackUrn(), viewData.getAudioAd());
        } else {
            trackObservable = getTrackObservable(viewData.getTrackUrn());
        }
        return trackObservable;
    }

    private Observable<PropertySet> getAdObservable(TrackUrn urn, final PropertySet audioAd) {
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

    private View subscribeToPlayEvents(PlayerPagePresenter presenter, View trackPage) {
        subscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber(presenter, trackPage)));
        subscription.add(eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerPanelSubscriber(presenter, trackPage)));
        subscription.add(eventBus
                .queue(EventQueue.PLAYABLE_CHANGED)
                .filter(PlayableUpdatedEvent.IS_TRACK_FILTER)
                .doOnNext(invalidateTrackCacheAction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlayableChangedSubscriber(presenter, trackPage)));
        subscription.add(eventBus
                .queue(EventQueue.PLAYBACK_PROGRESS)
                .filter(currentTrackFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaybackProgressSubscriber(presenter, trackPage)));
        return trackPage;
    }

    void onTrackChange() {
        for (Map.Entry<View, TrackPageData> entry : trackByViews.entrySet()) {
            if (getItemViewTypeFromObject(entry.getKey()) == TYPE_TRACK_VIEW) {
                TrackUrn urn = entry.getValue().getTrackUrn();
                trackPagePresenter.onPageChange(entry.getKey());
                updateProgress(trackPagePresenter, entry.getKey(), urn);
            }
        }
    }

    private void onPagePositionSet(View view, int position) {
        getPresenter(currentData.get(position)).onPositionSet(view, position, currentData.size());
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

    private Observable<PropertySet> getTrackObservable(TrackUrn urn) {
        ReplaySubject<PropertySet> trackSubject = trackObservableCache.get(urn);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackOperations
                    .track(urn)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(trackSubject);
            trackObservableCache.put(urn, trackSubject);
        }
        return trackSubject;
    }

    private boolean isViewPresentingCurrentTrack(View trackPage) {
        return trackByViews.containsKey(trackPage) && playQueueManager.isCurrentTrack(trackByViews.get(trackPage).getTrackUrn());
    }

    private Boolean isTrackRelatedToView(View trackPage, Urn urn) {
        return trackByViews.containsKey(trackPage) && trackByViews.get(trackPage).getTrackUrn().equals(urn)
                || trackPageRecycler.isPageForUrn(trackPage, urn);
    }

    private void updateProgress(PlayerPagePresenter presenter, View trackView, TrackUrn urn) {
        presenter.setProgress(trackView, playSessionStateProvider.getLastProgressByUrn(urn));
    }

    private class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public TrackSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PropertySet track) {
            TrackUrn trackUrn = track.get(TrackProperty.URN);
            if (isTrackRelatedToView(trackPage, trackUrn)) {
                presenter.bindItemView(trackPage, track, playQueueManager.isCurrentTrack(trackUrn));
                updateProgress(presenter, trackPage, trackUrn);

                // TODO: Temporary for developing leave behind
                if (featureFlags.isEnabled(Feature.LEAVE_BEHIND) && trackPagePresenter.accept(trackPage)) {
                    LeaveBehind data = new LeaveBehind("https://cloud.githubusercontent.com/assets/283794/4230685/ab66ec14-3987-11e4-9f94-d85a84daa8fa.jpg",
                            "http://www.meridianpeakhypnosis.com/wp-content/uploads/2014/02/money-addiction.jpg");
                    trackPagePresenter.showLeaveBehind(trackPage, data);
                }
            }
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
            presenter.setPlayState(trackPage, stateTransition, isViewPresentingCurrentTrack(trackPage));
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

    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableUpdatedEvent> {
        private final PlayerPagePresenter presenter;
        private final View trackPage;

        public PlayableChangedSubscriber(PlayerPagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlayableUpdatedEvent event) {
            if (isTrackRelatedToView(trackPage, event.getUrn())) {
                presenter.onPlayableUpdated(trackPage, event);
            }
        }
    }
}

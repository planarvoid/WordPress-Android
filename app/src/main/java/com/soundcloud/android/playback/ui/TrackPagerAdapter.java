package com.soundcloud.android.playback.ui;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private static final int TYPE_TRACK_VIEW = 0;
    private static final int TYPE_AD_VIEW = 1;
    private static final int EXPECTED_TRACKVIEW_COUNT = 5;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final AdPagePresenter adPagePresenter;
    private final EventBus eventBus;

    // WeakHashSet, to avoid re-subscribing subscribed views without holding strong refs
    private final Set<View> subscribedTrackViews = Collections.newSetFromMap(new WeakHashMap<View, Boolean>());

    private SkipListener skipListener;

    private final LruCache<TrackUrn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<TrackUrn, ReplaySubject<PropertySet>>(TRACK_CACHE_SIZE);
    private final Map<View, ViewPageData> trackByViews = new HashMap<View, ViewPageData>(EXPECTED_TRACKVIEW_COUNT);

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
                      EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionStateProvider = playSessionStateProvider;
        this.adPagePresenter = adPagePresenter;
        this.eventBus = eventBus;
    }

    public void onPlayerSlide(float slideOffset) {
        for (Map.Entry<View, ViewPageData> entry : trackByViews.entrySet()) {
            int viewPosition = entry.getValue().positionInPlayQueue;
            getPresenter(viewPosition).onPlayerSlide(entry.getKey(), slideOffset);
        }
    }

    void unsubscribe() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription();
        subscribedTrackViews.clear();
    }

    void warmupViewCache(ViewGroup container) {
        for (int i = 0; i < EXPECTED_TRACKVIEW_COUNT; i++) {
            final View itemView = trackPagePresenter.createItemView(container, skipListener);
            addScrapView(i, itemView, TYPE_TRACK_VIEW);
        }
    }

    void setSkipListener(SkipListener skipListener) {
        this.skipListener = skipListener;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return playQueueManager.isAudioAdAtPosition(position) ? TYPE_AD_VIEW : TYPE_TRACK_VIEW;
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return trackPagePresenter.accept((View) object) ? TYPE_TRACK_VIEW : TYPE_AD_VIEW;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        TrackUrn urn = playQueueManager.getUrnAtPosition(position);

        final PlayerPagePresenter presenter = getPresenter(position);
        final View contentView = convertView == null
                ? presenter.createItemView(container, skipListener)
                : presenter.clearItemView(convertView);

        final ViewPageData viewData = new ViewPageData(position, urn);
        updateViewMap(contentView, viewData);

        if (!subscribedTrackViews.contains(contentView)) {
            subscribeToPlayEvents(presenter, contentView);
            subscribedTrackViews.add(contentView);
        }

        getSoundObservable(viewData).subscribe(new TrackSubscriber(presenter, contentView));
        return contentView;
    }

    private void updateViewMap(View contentView, ViewPageData viewData) {
        // ensure uniqueness in current view at position map
        final View existingView = getCurrentViewAtPosition(viewData.positionInPlayQueue);
        if (existingView != null) {
            trackByViews.remove(existingView);
        }
        trackByViews.put(contentView, viewData);
    }

    private View getCurrentViewAtPosition(final int position) {
        Map.Entry<View, ViewPageData> entry = Iterables.find(trackByViews.entrySet(), new Predicate<Map.Entry<View, ViewPageData>>() {
            @Override
            public boolean apply(Map.Entry<View, ViewPageData> input) {
                return input.getValue().positionInPlayQueue == position;
            }
        }, null);
        return entry == null ? null : entry.getKey();
    }

    private PlayerPagePresenter getPresenter(int position) {
        if (getItemViewType(position) == TYPE_TRACK_VIEW) {
            return trackPagePresenter;
        }
        return adPagePresenter;
    }

    private Observable<PropertySet> getSoundObservable(ViewPageData viewData) {
        final Observable<PropertySet> trackObservable;
        if (playQueueManager.isAudioAdAtPosition(viewData.positionInPlayQueue)) {
            trackObservable = getAdObservable(viewData.trackUrn, playQueueManager.getAudioAd());
        } else {
            trackObservable = getTrackObservable(viewData.trackUrn);
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
        for (View view : trackByViews.keySet()) {
            if (getItemViewTypeFromObject(view) == TYPE_TRACK_VIEW) {
                trackPagePresenter.onPageChange(view);
                updateProgress(trackPagePresenter, view, trackByViews.get(view).trackUrn);
            }
        }
    }

    @Override
    public void notifyDataSetChanged() {
        removeCachedAudioAdPages();
        super.notifyDataSetChanged();
        updatePagePositions();
    }

    private void updatePagePositions() {
        for (Map.Entry<View, ViewPageData> entry : trackByViews.entrySet()) {
            final TrackUrn urn = entry.getValue().trackUrn;
            final int position = entry.getValue().positionInPlayQueue;
            getPresenter(position).onPositionSet(entry.getKey(), playQueueManager.getPositionForUrn(urn), playQueueManager.getQueueSize());
        }
    }

    @Override
    public int getItemPosition(Object object) {
        final View view = (View) object;
        if (isViewInSamePosition(view)) {
            return POSITION_UNCHANGED;
        }

        final ViewPageData viewPageData = getUpdatedViewPageData(view);
        if (viewPageData == null) {
            trackByViews.remove(object);
            return POSITION_NONE;
        } else {
            updateViewMap(view, viewPageData);
            return viewPageData.positionInPlayQueue;
        }
    }

    // since we remove audio ads from the play queue after they played, we need to make sure we're not trying
    // to reuse their views if they were cached
    private void removeCachedAudioAdPages() {
        View adPageKey = null;
        for (Map.Entry<View, ViewPageData> entry : trackByViews.entrySet()) {
            if (entry.getValue().isAdPage) {
                adPageKey = entry.getKey();
            }
        }
        if (adPageKey != null) {
            trackByViews.remove(adPageKey);
        }
    }

    private boolean isViewInSamePosition(View view) {
        if (trackByViews.containsKey(view)) {
            final ViewPageData viewPageData = trackByViews.get(view);
            final TrackUrn trackInPlayQueue = playQueueManager.getUrnAtPosition(viewPageData.positionInPlayQueue);
            return viewPageData.trackUrn.equals(trackInPlayQueue);
        }
        return false;
    }

    @Nullable
    private ViewPageData getUpdatedViewPageData(View view) {
        if (trackByViews.containsKey(view)) {
            final ViewPageData viewPageData = trackByViews.get(view);
            final int newPosition = playQueueManager.getPositionForUrn(viewPageData.trackUrn);
            if (newPosition != Consts.NOT_SET) {
                return new ViewPageData(newPosition, viewPageData.trackUrn);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return playQueueManager.getQueueSize();
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
        return trackByViews.containsKey(trackPage) && playQueueManager.isCurrentTrack(trackByViews.get(trackPage).trackUrn);
    }

    private Boolean isTrackRelatedToView(View trackPage, Urn urn) {
        return trackByViews.containsKey(trackPage) && trackByViews.get(trackPage).trackUrn.equals(urn);
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
                presenter.onPositionSet(trackPage, playQueueManager.getPositionForUrn(trackUrn), playQueueManager.getQueueSize());
                updateProgress(presenter, trackPage, trackUrn);
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

    private  final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
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

    private class ViewPageData {
        private final int positionInPlayQueue;
        private final TrackUrn trackUrn;
        private final boolean isAdPage;

        ViewPageData(int positionInPlayQueue, @NotNull TrackUrn trackUrn) {
            this.positionInPlayQueue = positionInPlayQueue;
            this.trackUrn = trackUrn;
            this.isAdPage = playQueueManager.isAudioAdAtPosition(positionInPlayQueue);
        }
    }

}

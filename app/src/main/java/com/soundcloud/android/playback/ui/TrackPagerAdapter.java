package com.soundcloud.android.playback.ui;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
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
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private static final int TYPE_TRACK_VIEW = 0;
    private static final int TYPE_AD_VIEW = 1;
    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final AdPagePresenter adPagePresenter;
    private final EventBus eventBus;


    private final LruCache<TrackUrn, ReplaySubject<PropertySet>> trackObservableCache =
            new LruCache<TrackUrn, ReplaySubject<PropertySet>>(TRACK_CACHE_SIZE);
    private final Map<View, ViewPageData> trackByViews = new HashMap<View, ViewPageData>(EXPECTED_TRACKVIEW_COUNT);
    private final Func1<PlaybackProgressEvent, Boolean> currentTrackFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return playQueueManager.isCurrentTrack(progressEvent.getTrackUrn());
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

    void unsubscribe() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (playQueueManager.isAudioAdAtPosition(position)) {
            return TYPE_AD_VIEW;
        } else {
            return TYPE_TRACK_VIEW;
        }
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return trackPagePresenter.accept(((View) object)) ? TYPE_TRACK_VIEW : TYPE_AD_VIEW;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        TrackUrn urn = playQueueManager.getUrnAtPosition(position);

        final boolean isNewView = convertView == null;
        final PagePresenter presenter = getPresenter(position);
        final View contentView = isNewView
                ? presenter.createItemView(container)
                : presenter.clearItemView(convertView);

        final ViewPageData viewData = new ViewPageData(position, urn);
        trackByViews.put(contentView, viewData); // forcePut to remove existing entry
        if (isNewView) {
            subscribeToPlayEvents(presenter, contentView);
        }

        getSoundObservable(viewData)
                .filter(new TrackIntendedViewFilter(contentView))
                .subscribe(new TrackSubscriber(presenter, contentView));
        return contentView;
    }

    private PagePresenter getPresenter(int position) {
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
        return getTrackObservable(urn)
                .map(new Func1<PropertySet, PropertySet>() {
                    @Override
                    public PropertySet call(PropertySet propertySet) {
                        return propertySet.merge(audioAd);
                    }
                });
    }

    private View subscribeToPlayEvents(PagePresenter presenter, View trackPage) {
        subscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber(presenter, trackPage)));
        subscription.add(eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerPanelSubscriber(presenter, trackPage)));
        subscription.add(eventBus
                .queue(EventQueue.PLAYBACK_PROGRESS)
                .filter(currentTrackFilter)
                .filter(new ProgressEventIntendedViewFilter(trackPage))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaybackProgressSubscriber(presenter, trackPage)));
        return trackPage;
    }

    void onTrackChange() {
        for (View view : trackByViews.keySet()) {
            if (getItemViewTypeFromObject(view) == TYPE_TRACK_VIEW) {
                trackPagePresenter.clearScrubState(view);
                updateProgress(trackPagePresenter, view, trackByViews.get(view).trackUrn);
            }
        }
    }

    @Override
    public int getItemPosition(Object object) {
        final View view = (View) object;
        if (isViewInSamePosition(view)) {
            return POSITION_UNCHANGED;
        }

        final ViewPageData viewPageData = getUpdatedViewPageData(view);
        if (viewPageData != null) {
            trackByViews.put(view, viewPageData);
            return viewPageData.positionInPlayQueue;
        } else {
            trackByViews.remove(object);
            return POSITION_NONE;
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

    private Boolean isTrackRelatedToView(View trackPage, TrackUrn trackUrn) {
        return trackByViews.containsKey(trackPage) && trackByViews.get(trackPage).trackUrn.equals(trackUrn);
    }

    private void updateProgress(final PagePresenter presenter, final View trackView, final TrackUrn urn) {
        presenter.setProgress(trackView, playSessionStateProvider.getCurrentProgress(urn));
    }

    private class TrackSubscriber extends DefaultSubscriber<PropertySet> {
        private final PagePresenter presenter;
        private final View trackView;

        private TrackSubscriber(PagePresenter presenter, View trackView) {
            this.presenter = presenter;
            this.trackView = trackView;
        }

        @Override
        public void onNext(PropertySet track) {
            presenter.bindItemView(trackView, track);
            updateProgress(presenter, trackView, track.get(TrackProperty.URN));
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        private final PagePresenter presenter;
        private final View trackPage;

        public PlaybackStateSubscriber(PagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            presenter.setPlayState(trackPage, stateTransition, isViewPresentingCurrentTrack(trackPage));
        }
    }

    private  final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        private final PagePresenter presenter;
        private final View trackPage;

        public PlaybackProgressSubscriber(PagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlaybackProgressEvent progress) {
            presenter.setProgress(trackPage, progress.getPlaybackProgress());
        }
    }

    private final class PlayerPanelSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final PagePresenter presenter;
        private final View trackPage;

        public PlayerPanelSubscriber(PagePresenter presenter, View trackPage) {
            this.presenter = presenter;
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                presenter.setExpanded(trackPage, playSessionStateProvider.isPlaying());
            } else if (event.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
                presenter.setCollapsed(trackPage);
            }
        }
    }

    private final class ProgressEventIntendedViewFilter implements Func1<PlaybackProgressEvent, Boolean> {
        private final View trackPage;

        private ProgressEventIntendedViewFilter(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return isTrackRelatedToView(trackPage, progressEvent.getTrackUrn());
        }
    }

    private final class TrackIntendedViewFilter implements Func1<PropertySet, Boolean> {
        private final View trackPage;

        private TrackIntendedViewFilter(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public Boolean call(PropertySet track) {
            return isTrackRelatedToView(trackPage, track.get(TrackProperty.URN));
        }
    }

    private static class ViewPageData {
        private final int positionInPlayQueue;
        private final TrackUrn trackUrn;

        ViewPageData(int positionInPlayQueue, @NotNull TrackUrn trackUrn) {
            this.positionInPlayQueue = positionInPlayQueue;
            this.trackUrn = trackUrn;
        }
    }
}

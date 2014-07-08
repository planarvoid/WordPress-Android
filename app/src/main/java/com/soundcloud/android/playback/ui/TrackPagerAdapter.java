package com.soundcloud.android.playback.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import rx.subscriptions.CompositeSubscription;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Map;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private static final int TYPE_TRACK_VIEW = 0;
    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final EventBus eventBus;

    private final LruCache<TrackUrn, ReplaySubject<PlayerTrack>> trackObservableCache =
            new LruCache<TrackUrn, ReplaySubject<PlayerTrack>>(TRACK_CACHE_SIZE);
    private final BiMap<View, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);
    private final Func1<PropertySet, PlayerTrack> toPlayerTrack = new Func1<PropertySet, PlayerTrack>() {
        @Override
        public PlayerTrack call(PropertySet source) {
            return new PlayerTrack(source);
        }
    };
    private final Func1<PlaybackProgressEvent, Boolean> currentTrackFilter = new Func1<PlaybackProgressEvent, Boolean>() {
        @Override
        public Boolean call(PlaybackProgressEvent progressEvent) {
            return playQueueManager.isCurrentTrack(progressEvent.getTrackUrn());
        }
    };
    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, PlaySessionController playSessionController,
                      TrackOperations trackOperations, TrackPagePresenter trackPagePresenter, EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionController = playSessionController;
        this.eventBus = eventBus;
    }

    void unsubscribe() {
        subscription.unsubscribe();
        subscription = new CompositeSubscription();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_TRACK_VIEW;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        TrackUrn urn = playQueueManager.getUrnAtPosition(position);

        final boolean isNewView = convertView == null;
        final View contentView = isNewView
                ? trackPagePresenter.createTrackPage(container)
                : trackPagePresenter.clearView(convertView, urn);

        trackViewsByPosition.forcePut(contentView, position); // forcePut to remove existing entry
        if (isNewView) {
            subscribe(contentView);
        }
        loadPlayerItem(contentView, urn);
        return contentView;
    }

    private View subscribe(View trackPage) {
        subscription.add(eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlaybackStateSubscriber(trackPage)));
        subscription.add(eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerPanelSubscriber(trackPage)));
        subscription.add(eventBus
                .queue(EventQueue.PLAYBACK_PROGRESS)
                .filter(currentTrackFilter)
                .filter(new ProgressEventIntendedViewFilter(trackPage))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlaybackProgressSubscriber(trackPage)));
        return trackPage;
    }

    @Override
    public void notifyDataSetChanged() {
        trackViewsByPosition.clear();
        super.notifyDataSetChanged();
    }

    void onTrackChange() {
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            trackPagePresenter.clearScrubState(entry.getKey());
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return TYPE_TRACK_VIEW;
    }

    @Override
    public int getCount() {
        return playQueueManager.getQueueSize();
    }

    private void loadPlayerItem(View trackView, TrackUrn trackUrn) {
        getTrackObservable(trackUrn)
                .filter(new TrackIntendedViewFilter(trackView))
                .subscribe(new TrackSubscriber(trackView));
    }

    public Observable<PlayerTrack> getTrackObservable(TrackUrn urn) {
        ReplaySubject<PlayerTrack> trackSubject = trackObservableCache.get(urn);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackOperations.track(urn).map(toPlayerTrack).observeOn(AndroidSchedulers.mainThread()).subscribe(trackSubject);
            trackObservableCache.put(urn, trackSubject);
        }
        return trackSubject;
    }

    private boolean isViewPresentingCurrentTrack(View trackPage) {
        if (trackViewsByPosition.containsKey(trackPage)) {
            return playQueueManager.isCurrentPosition(trackViewsByPosition.get(trackPage));
        }
        return false;
    }

    private Boolean isTrackRelatedToView(View trackPage, TrackUrn trackUrn) {
        if (trackViewsByPosition.containsKey(trackPage)) {
            return playQueueManager.getUrnAtPosition(trackViewsByPosition.get(trackPage)).equals(trackUrn);
        }
        return false;
    }

    private class TrackSubscriber extends DefaultSubscriber<PlayerTrack> {
        private final View trackView;

        private TrackSubscriber(View trackView) {
            this.trackView = trackView;
        }

        @Override
        public void onNext(PlayerTrack track) {
            if (!playSessionController.isPlaying()) {
                resetProgress(track, trackView);
            }
            trackPagePresenter.populateTrackPage(trackView, track);
        }

        private void resetProgress(final PlayerTrack track, final View trackView) {
            final PlaybackProgress progress = playSessionController.getCurrentProgress(track.getUrn());
            trackPagePresenter.setProgress(trackView, progress);
        }
    }

    private final class PlaybackStateSubscriber extends DefaultSubscriber<Playa.StateTransition> {
        private final View trackPage;

        public PlaybackStateSubscriber(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(Playa.StateTransition stateTransition) {
            trackPagePresenter.setPlayState(trackPage, stateTransition, isViewPresentingCurrentTrack(trackPage));
        }
    }

    private final class PlaybackProgressSubscriber extends DefaultSubscriber<PlaybackProgressEvent> {
        private final View trackPage;

        public PlaybackProgressSubscriber(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlaybackProgressEvent progress) {
            trackPagePresenter.setProgress(trackPage, progress.getPlaybackProgress());
        }
    }

    private final class PlayerPanelSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        private final View trackPage;

        public PlayerPanelSubscriber(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                trackPagePresenter.setExpanded(trackPage, playSessionController.isPlaying());
            } else if (event.getKind() == PlayerUIEvent.PLAYER_COLLAPSED) {
                trackPagePresenter.setCollapsed(trackPage);
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

    private final class TrackIntendedViewFilter implements Func1<PlayerTrack, Boolean> {
        private final View trackPage;

        private TrackIntendedViewFilter(View trackPage) {
            this.trackPage = trackPage;
        }

        @Override
        public Boolean call(PlayerTrack track) {
            return isTrackRelatedToView(trackPage, track.getUrn());
        }
    }

    @VisibleForTesting
    View getTrackViewByPosition(int id) {
        return trackViewsByPosition.inverse().get(id);
    }
}

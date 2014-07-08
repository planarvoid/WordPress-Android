package com.soundcloud.android.playback.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Map;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;

    private final LruCache<TrackUrn, ReplaySubject<PlayerTrack>> trackObservableCache =
            new LruCache<TrackUrn, ReplaySubject<PlayerTrack>>(TRACK_CACHE_SIZE);
    private final BiMap<View, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);
    private final Func1<PropertySet, PlayerTrack> toPlayerTrack = new Func1<PropertySet, PlayerTrack>() {
        @Override
        public PlayerTrack call(PropertySet source) {
            return new PlayerTrack(source);
        }
    };

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, PlaySessionController playSessionController,
                      TrackOperations trackOperations, TrackPagePresenter trackPagePresenter) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionController = playSessionController;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        final View contentView = convertView == null
                ? trackPagePresenter.createTrackPage(container)
                : convertView;

        trackViewsByPosition.forcePut(contentView, position); // forcePut to remove existing entry
        loadPlayerItem(playQueueManager.getUrnAtPosition(position));
        return contentView;
    }

    @Override
    public void notifyDataSetChanged() {
        trackViewsByPosition.clear();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @VisibleForTesting
    View getTrackViewByPosition(int id) {
        return trackViewsByPosition.inverse().get(id);
    }

    public void setProgressOnCurrentTrack(PlaybackProgressEvent progressEvent) {
        if (playQueueManager.isCurrentTrack(progressEvent.getTrackUrn())) {
            View currentTrackView = trackViewsByPosition.inverse().get(playQueueManager.getCurrentPosition());
            if (currentTrackView != null) {
                trackPagePresenter.setProgress(currentTrackView, progressEvent.getPlaybackProgress());
            }
        }
    }

    public void onCurrentPageChanged() {
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            View trackView = entry.getKey();
            Integer position = entry.getValue();
            final TrackUrn urnAtPosition = playQueueManager.getUrnAtPosition(position);
            trackPagePresenter.reset(trackView);
            trackPagePresenter.setProgress(trackView, playSessionController.getCurrentProgress(urnAtPosition));
        }
    }

    public void setPlayState(Playa.StateTransition stateTransition) {
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            setPlayState(stateTransition, entry.getKey(), entry.getValue());
        }
    }

    private void setPlayState(Playa.StateTransition stateTransition, View view, Integer position) {
        trackPagePresenter.setPlayState(view, stateTransition, playQueueManager.isCurrentPosition(position));
    }

    public void setExpandedMode(boolean isExpanded) {
        trackPagePresenter.setExpandedMode(isExpanded);
        for (View view : trackViewsByPosition.keySet()) {
            if (isExpanded) {
                trackPagePresenter.setExpanded(view, playSessionController.isPlaying());
            } else {
                trackPagePresenter.setCollapsed(view);
            }
        }
    }

    private void loadPlayerItem(TrackUrn urn) {
        getTrackObservable(urn).subscribe(new TrackSubscriber(urn));
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

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return 0;
    }

    @Override
    public int getCount() {
        return playQueueManager.getQueueSize();
    }

    @Nullable
    private View getViewForTrackUrn(TrackUrn trackUrn) {
        for (View trackView : trackViewsByPosition.keySet()) {
            final Integer position = trackViewsByPosition.get(trackView);
            if (trackUrn.equals(playQueueManager.getUrnAtPosition(position))) {
                return trackView;
            }
        }
        return null;
    }

    private class TrackSubscriber extends DefaultSubscriber<PlayerTrack> {
        private final TrackUrn trackUrn;

        private TrackSubscriber(TrackUrn trackUrn) {
            this.trackUrn = trackUrn;
        }

        @Override
        public void onNext(PlayerTrack track) {
            final View trackView = getViewForTrackUrn(trackUrn);
            if (trackView != null) {
                final PlaybackProgress currentProgress = playSessionController.getCurrentProgress(track.getUrn());
                trackPagePresenter.populateTrackPage(trackView, track, currentProgress);
                trackPagePresenter.setPlayState(trackView, playSessionController.getPlayState(), playQueueManager.isCurrentTrack(track.getUrn()));
            }
        }
    }

}

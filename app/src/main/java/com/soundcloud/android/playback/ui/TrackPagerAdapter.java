package com.soundcloud.android.playback.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.LegacyTrackOperations;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
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
    private final LegacyTrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;

    private final LruCache<Long, Observable<Track>> trackObservableCache = new LruCache<Long, Observable<Track>>(TRACK_CACHE_SIZE);
    private final BiMap<View, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, PlaySessionController playSessionController,
                      LegacyTrackOperations trackOperations, TrackPagePresenter trackPagePresenter) {
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
        loadPlayerItem(playQueueManager.getIdAtPosition(position));
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
        if (playQueueManager.isCurrentTrack(progressEvent.getTrackUrn())){
            View currentTrackView = trackViewsByPosition.inverse().get(playQueueManager.getCurrentPosition());
            if (currentTrackView != null) {
                trackPagePresenter.setProgress(currentTrackView, progressEvent.getPlaybackProgress());
            }
        }
    }

    public void setProgressOnAllViews(){
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            View trackView = entry.getKey();
            Integer position = entry.getValue();
            final TrackUrn urnAtPosition = playQueueManager.getUrnAtPosition(position);
            trackPagePresenter.setProgress(trackView, playSessionController.getCurrentProgress(urnAtPosition));
        }
    }

    public void setPlayState(Playa.StateTransition stateTransition) {
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            final boolean isCurrentQueuePosition = playQueueManager.isCurrentPosition(entry.getValue());
            trackPagePresenter.setPlayState(entry.getKey(), stateTransition, isCurrentQueuePosition);
        }
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

    private void loadPlayerItem(long id) {
        getTrackObservable(id).subscribe(new TrackSubscriber(id));
    }

    public Observable<Track> getTrackObservable(long id) {
        Observable<Track> trackSubject = trackObservableCache.get(id);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackOperations.loadTrack(id, AndroidSchedulers.mainThread()).subscribe((ReplaySubject<Track>) trackSubject);
            trackObservableCache.put(id, trackSubject);
        }
        return trackSubject;
    }

    private class TrackSubscriber extends DefaultSubscriber<Track> {
        private final long trackId;

        private TrackSubscriber(long trackId) {
            this.trackId = trackId;
        }

        @Override
        public void onNext(Track track) {
            for (View trackView : trackViewsByPosition.keySet()) {
                final Integer position = trackViewsByPosition.get(trackView);
                final long idOfQueueView = playQueueManager.getIdAtPosition(position);
                if (trackId == idOfQueueView) {
                    final PlaybackProgress currentProgress = playSessionController.getCurrentProgress(track.getUrn());
                    trackPagePresenter.populateTrackPage(trackView, track, currentProgress);
                }
            }
        }
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return 0;
    }

    @Override
    public int getCount() {
        return playQueueManager.getQueueSize();
    }

}

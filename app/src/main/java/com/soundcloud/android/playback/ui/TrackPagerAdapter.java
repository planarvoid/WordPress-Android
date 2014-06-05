package com.soundcloud.android.playback.ui;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
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

public class TrackPagerAdapter extends RecyclingPagerAdapter implements TrackPagePresenter.Listener {

    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final PlaySessionController playSessionController;
    private final LegacyTrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final PlaybackOperations playbackOperations;
    private final EventBus eventBus;

    private boolean newPagesInFullScreenMode;

    private final LruCache<Long, Observable<Track>> trackObservableCache = new LruCache<Long, Observable<Track>>(TRACK_CACHE_SIZE);
    private final BiMap<View, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, PlaySessionController playSessionController,
                      LegacyTrackOperations trackOperations, TrackPagePresenter trackPagePresenter,
                      PlaybackOperations playbackOperations, EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
        this.playSessionController = playSessionController;
        this.playbackOperations = playbackOperations;
        this.eventBus = eventBus;
        trackPagePresenter.setListener(this);
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
                ? trackPagePresenter.createTrackPage(container, newPagesInFullScreenMode)
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

    public void setProgressOnCurrentTrack(PlaybackProgressEvent progress) {
        View currentTrackView = trackViewsByPosition.inverse().get(playQueueManager.getCurrentPosition());
        trackPagePresenter.setProgress(currentTrackView, progress);
    }

    public void setProgressOnAllViews(){
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            View trackView = entry.getKey();
            Integer position = entry.getValue();
            if (playSessionController.isPlayingTrack(playQueueManager.getUrnAtPosition(position))) {
                trackPagePresenter.setProgress(trackView, playSessionController.getCurrentProgress());
            } else {
                trackPagePresenter.resetProgress(trackView);
            }
        }
    }

    public void setPlayState(boolean isPlaying) {
        for (Map.Entry<View, Integer> entry : trackViewsByPosition.entrySet()) {
            trackPagePresenter.setPlayState(entry.getKey(), isPlaying && playQueueManager.isCurrentPosition(entry.getValue()));
        }
    }

    public void fullScreenMode(boolean fullScreen) {
        newPagesInFullScreenMode = fullScreen;
        for (View view : trackViewsByPosition.keySet()) {
            trackPagePresenter.setFullScreen(view, fullScreen);
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
                    if (playSessionController.isPlayingTrack(track)){
                        trackPagePresenter.populateTrackPage(trackView, track, playSessionController.getCurrentProgress());
                    } else {
                        trackPagePresenter.populateTrackPage(trackView, track);
                    }
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

    @Override
    public void onTogglePlay() {
        playbackOperations.togglePlayback();
    }

    @Override
    public void onFooterTap() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    @Override
    public void onPlayerClose() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());
    }

    @Override
    public void onNext() { playbackOperations.nextTrack(); }

    @Override
    public void onPrevious() { playbackOperations.previousTrack(); }

}

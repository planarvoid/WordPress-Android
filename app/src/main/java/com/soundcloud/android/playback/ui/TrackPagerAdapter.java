package com.soundcloud.android.playback.ui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;

    private final PlayQueueManager playQueueManager;
    private final TrackOperations trackOperations;
    private final TrackPagePresenter trackPagePresenter;
    private final LruCache<Long, Observable<Track>> trackObservableCache = new LruCache<Long, Observable<Track>>(TRACK_CACHE_SIZE);
    private final BiMap<View, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager, TrackOperations trackOperations, TrackPagePresenter trackPagePresenter) {
        this.playQueueManager = playQueueManager;
        this.trackOperations = trackOperations;
        this.trackPagePresenter = trackPagePresenter;
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
                    trackPagePresenter.populateTrackPage(trackView, track);
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
        return playQueueManager.getCurrentPlayQueueSize();
    }
}

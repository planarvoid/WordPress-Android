package com.soundcloud.android.playback;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.LegacyPlayerTrackView;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.LegacyTrackOperations;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

import android.content.Context;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

public abstract class  PlayerTrackPagerAdapter<T extends View & PlayerTrackView> extends RecyclingPagerAdapter {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "PlayerTrackPagerAdapter";

    // 4 = 3 views visible at any time and 1 bonus because of how the recycler works
    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;
    private static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_TRACK = 1;
    private static final long EMPTY_VIEW_ID = -1;

    private final BiMap<T, Integer> trackViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);
    private final LegacyTrackOperations trackOperations;
    private final PlaybackStateProvider stateProvider;

    protected PlayQueueView playQueue = PlayQueueView.EMPTY;

    // FIXME : Using ReplaySubjects here is pretty wasteful. We just want the last (post sync) item. Make custom subject
    private final LruCache<Long, ReplaySubject<Track>> trackSubjectCache =
            new LruCache<Long, ReplaySubject<Track>>(TRACK_CACHE_SIZE);

    public PlayerTrackPagerAdapter(LegacyTrackOperations trackOperations, PlaybackStateProvider stateProvider) {
        this.trackOperations = trackOperations;
        this.stateProvider = stateProvider;
    }

    public Collection<T> getPlayerTrackViews() {
        return trackViewsByPosition.keySet();
    }

    public void setPlayQueue(PlayQueueView playQueue) {
        this.playQueue = playQueue;
    }

    public void onConnected() {
        for (T ptv : getPlayerTrackViews()) {
            ptv.onDataConnected();
        }
    }

    public void onStop() {
        for (T ptv : getPlayerTrackViews()) {
            ptv.onStop();
        }
    }

    public void onDestroy() {
        for (T ptv : getPlayerTrackViews()) {
            ptv.onDestroy();
        }
    }

    @Override
    public int getCount() {
        return shouldDisplayExtraItem() ? playQueue.size() + 1 : playQueue.size();
    }

    private boolean shouldDisplayExtraItem() {
        return playQueue.isLoading() || playQueue.lastLoadFailed() || playQueue.lastLoadWasEmpty();
    }

    protected long getIdByPosition(int position) {
        if (position >= playQueue.size()) {
            return EMPTY_VIEW_ID;
        } else {
            return playQueue.getTrackIdAt(position);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        long id = getIdByPosition(position);
        if (getItemViewType(position) == VIEW_TYPE_EMPTY){

            final EmptyView emptyView = convertView != null ? (EmptyView) convertView :
                    createEmptyListView(container.getContext());

            switch (playQueue.getAppendState()){
                case LOADING:
                    emptyView.setStatus(EmptyView.Status.WAITING);
                    break;
                case ERROR:
                    emptyView.setStatus(EmptyView.Status.ERROR);
                    break;
                default:
                    emptyView.setStatus(EmptyView.Status.OK);
                    break;
            }
            return emptyView;

        } else {
            final T playerTrackView = convertView != null ? (T) convertView : createPlayerTrackView(container.getContext());
            trackViewsByPosition.forcePut(playerTrackView, position); // forcePut to remove existing entry
            loadPlayerItem(id);
            return playerTrackView;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= playQueue.size()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return VIEW_TYPE_TRACK;
        }
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        if (object instanceof LegacyPlayerTrackView) {
            return VIEW_TYPE_TRACK;
        } else {
            return VIEW_TYPE_EMPTY;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof LegacyPlayerTrackView) {
            return POSITION_UNCHANGED;
        } else {
            return POSITION_NONE;
        }
    }

    @Nullable
    public T getPlayerTrackViewById(long id) {
        for (T playerTrackView : getPlayerTrackViews()) {
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    @Nullable
    public T getPlayerTrackViewByPosition(int position) {
        return trackViewsByPosition.inverse().get(position);
    }

    public ReplaySubject<Track> getTrackObservable(long id) {
        ReplaySubject<Track> trackSubject = trackSubjectCache.get(id);
        if (trackSubject == null) {
            trackSubject = ReplaySubject.create();
            trackOperations.loadSyncedTrack(id, AndroidSchedulers.mainThread()).subscribe(trackSubject);
            trackSubjectCache.put(id, trackSubject);
        }
        return trackSubject;
    }

    private void loadPlayerItem(long id) {
        getTrackObservable(id).subscribe(new TrackSubscriber(id));
    }

    private class TrackSubscriber extends DefaultSubscriber<Track> {
        private final long trackId;

        private TrackSubscriber(long trackId) {
            this.trackId = trackId;
        }

        @Override
        public void onNext(Track track) {
            for (T playerTrackView : trackViewsByPosition.keySet()) {
                final Integer position = trackViewsByPosition.get(playerTrackView);
                final long idOfQueueView = getIdByPosition(position);
                if (trackId == idOfQueueView) {
                    setTrackOnPlayerTrackView(track, playerTrackView, position);
                }
            }
        }
    }

    protected void setTrackOnPlayerTrackView(Track track, T playerTrackView, Integer queuePosition) {
        playerTrackView.setTrackState(track, queuePosition, stateProvider);
    }

    protected abstract T createPlayerTrackView(Context context);

    protected abstract EmptyView createEmptyListView(Context context);
}

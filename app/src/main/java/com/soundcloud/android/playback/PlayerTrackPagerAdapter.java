package com.soundcloud.android.playback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collection;

public class PlayerTrackPagerAdapter extends RecyclingPagerAdapter {

    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "PlayerTrackPagerAdapter";

    // 4 = 3 views visible at any time and 1 bonus because of how the recycler works
    private static final int EXPECTED_TRACKVIEW_COUNT = 4;
    private static final int TRACK_CACHE_SIZE = 10;
    private static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_TRACK = 1;
    private static final long EMPTY_VIEW_ID = -1;

    private final BiMap<PlayerTrackView, Integer> queueViewsByPosition = HashBiMap.create(EXPECTED_TRACKVIEW_COUNT);
    private final TrackOperations trackOperations;
    private final PlaybackStateProvider stateProvider;
    private final ViewFactory playerViewFactory;

    protected PlayQueueView playQueue = PlayQueueView.EMPTY;

    // FIXME : Using ReplaySubjects here is pretty wasteful. We just want the last (post sync) item. Make custom subject
    private final LruCache<Long, ReplaySubject<Track>> trackSubjectCache =
            new LruCache<Long, ReplaySubject<Track>>(TRACK_CACHE_SIZE);

    @Inject
    public PlayerTrackPagerAdapter(TrackOperations trackOperations, PlaybackStateProvider stateProvider,
                                   ViewFactory playerViewFactory) {
        this.trackOperations = trackOperations;
        this.stateProvider = stateProvider;
        this.playerViewFactory = playerViewFactory;
    }

    public Collection<PlayerTrackView> getPlayerTrackViews() {
        return queueViewsByPosition.keySet();
    }

    public void setPlayQueue(PlayQueueView playQueue) {
        this.playQueue = playQueue;
    }

    public void onConnected() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onDataConnected();
        }
    }

    public void onStop() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onStop(true);
        }
    }

    public void onDestroy() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        long id = getIdByPosition(position);
        if (getItemViewType(position) == VIEW_TYPE_EMPTY){

            final EmptyListView emptyView = convertView != null ? (EmptyListView) convertView :
                    playerViewFactory.createEmptyListView(container.getContext());

            switch (playQueue.getAppendState()){
                case LOADING:
                    emptyView.setStatus(EmptyListView.Status.WAITING);
                    break;
                case ERROR:
                    emptyView.setStatus(EmptyListView.Status.ERROR);
                    break;
                default:
                    emptyView.setStatus(EmptyListView.Status.OK);
                    break;
            }
            return emptyView;

        } else {
            final PlayerTrackView playerTrackView = convertView != null ? (PlayerTrackView) convertView :
                    playerViewFactory.createPlayerTrackView(container.getContext());

            queueViewsByPosition.forcePut(playerTrackView, position); // forcePut to remove existing entry
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
        if (object instanceof PlayerTrackView) {
            return VIEW_TYPE_TRACK;
        } else {
            return VIEW_TYPE_EMPTY;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof PlayerTrackView) {
            return POSITION_UNCHANGED;
        } else {
            return POSITION_NONE;
        }
    }

    @Nullable
    public PlayerTrackView getPlayerTrackViewById(long id) {
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()) {
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    @Nullable
    public PlayerTrackView getPlayerTrackViewByPosition(int position) {
        return queueViewsByPosition.inverse().get(position);
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
            for (PlayerTrackView playerQueueView : queueViewsByPosition.keySet()) {
                final Integer position = queueViewsByPosition.get(playerQueueView);
                final long idOfQueueView = getIdByPosition(position);
                if (trackId == idOfQueueView) {
                    setTrackOnPlayerTrackView(track, playerQueueView, position);
                }
            }
        }
    }

    protected void setTrackOnPlayerTrackView(Track track, PlayerTrackView playerQueueView, Integer queuePosition) {
        playerQueueView.setTrackState(track, queuePosition, stateProvider);
    }

    @VisibleForTesting
    static class ViewFactory {

        @Inject
        ViewFactory() { }

        protected PlayerTrackView createPlayerTrackView(Context context) {
            return (PlayerTrackView) View.inflate(context, R.layout.player_track_view, null);
        }

        protected EmptyListView createEmptyListView(Context context) {
            EmptyListView emptyListView = new EmptyListView(context, R.layout.empty_player_track);
            emptyListView.setBackgroundColor(Color.WHITE);
            emptyListView.setMessageText(R.string.player_no_recommended_tracks);
            return emptyListView;
        }
    }
}

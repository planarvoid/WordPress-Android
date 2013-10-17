package com.soundcloud.android.service.playback;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.service.playback.PlayQueue.AppendState;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.List;

public class PlayQueueManager {

    @VisibleForTesting
    protected static String SC_PLAYQUEUE_URI = "sc_playlist_uri";

    private final Context mContext;
    private final PlayQueueStorage mPlayQueueStorage;
    private final SharedPreferences mSharedPreferences;
    private final ExploreTracksOperations mExploreTrackOperations;

    private PlayQueue mPlayQueue = PlayQueue.EMPTY;
    private Subscription mFetchRelatedSubscription, mReloadSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    public PlayQueueManager(Context context, PlayQueueStorage playQueueStorage, ExploreTracksOperations exploreTracksOperations, SharedPreferences sharedPreferences) {
        mContext = context;
        mPlayQueueStorage = playQueueStorage;
        mExploreTrackOperations = exploreTracksOperations;
        mSharedPreferences = sharedPreferences;
    }

    public void setNewPlayQueue(PlayQueue playQueue) {
        stopLoadingOperations();

        mPlayQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        broadcastPlayQueueChanged();

        saveCurrentPosition(0L);
        mPlayQueueStorage.storeAsync(mPlayQueue).subscribe(DefaultObserver.NOOP_OBSERVER);
    }

    public void saveCurrentPosition(long currentTrackProgress) {
        if (!mPlayQueue.isEmpty()) {
            final String playQueueState = mPlayQueue.getPlayQueueState(currentTrackProgress, mPlayQueue.getCurrentTrackId()).toString();
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putString(SC_PLAYQUEUE_URI, playQueueState));
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public ResumeInfo reloadPlayQueue() {
        final String lastUri = mSharedPreferences.getString(SC_PLAYQUEUE_URI, null);
        if (ScTextUtils.isNotBlank(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mReloadSubscription = mPlayQueueStorage.getTrackIds().subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> trackIds) {
                        setNewPlayQueue(new PlayQueue(trackIds, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo()));
                    }
                });
                return new ResumeInfo(trackId, seekPos);
            } else {
                final String message = "Unexpected track id when reloading playqueue: " + trackId;
                SoundCloudApplication.handleSilentException(message, new IllegalArgumentException(message));
            }
        } else {
            if (TextUtils.isEmpty(lastUri)) {
                // this is so the player can finish() instead of display waiting to the user
                broadcastPlayQueueChanged();
            }
        }

        return null;
    }

    public boolean shouldReloadQueue(){
        return mPlayQueue.isEmpty() && mReloadSubscription == null;
    }

    public void fetchRelatedTracks(long trackId){
        mRelatedTracksObservable = mExploreTrackOperations.getRelatedTracks(trackId);
        loadRelatedTracks();
    }

    // TODO
    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(){
        mSharedPreferences.edit().remove(SC_PLAYQUEUE_URI).commit();
        mPlayQueueStorage.clearState();
        mPlayQueue = PlayQueue.EMPTY;
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;
    }

    private void stopLoadingOperations() {
        if (mFetchRelatedSubscription != null){
            mFetchRelatedSubscription.unsubscribe();
            mFetchRelatedSubscription = null;
        }

        if (mReloadSubscription != null){
            /** do not null out this subscription as it is used to determine {@link this#shouldReloadQueue()} */
            mReloadSubscription.unsubscribe();
        }
    }

    private void loadRelatedTracks() {

        mPlayQueue.setRelatedLoadingState(AppendState.LOADING);
        broadcastRelatedLoadStateChanged();

        mFetchRelatedSubscription = mRelatedTracksObservable.subscribe(new Observer<RelatedTracksCollection>() {
            private boolean mGotRelatedTracks;

            @Override
            public void onCompleted() {
                // TODO, save new tracks to database
                final AppendState appendState = mGotRelatedTracks ? AppendState.IDLE : AppendState.EMPTY;
                mPlayQueue.setRelatedLoadingState(appendState);
                broadcastRelatedLoadStateChanged();
            }

            @Override
            public void onError(Throwable e) {
                mPlayQueue.setRelatedLoadingState(AppendState.ERROR);
                broadcastRelatedLoadStateChanged();
            }

            @Override
            public void onNext(RelatedTracksCollection relatedTracks) {
                final String recommenderVersion = relatedTracks.getSourceVersion();
                mPlayQueue.getPlaySourceInfo().setRecommenderVersion(recommenderVersion);
                for (TrackSummary item : relatedTracks) {
                    SoundCloudApplication.MODEL_MANAGER.cache(new Track(item));
                    mPlayQueue.addTrack(item.getId());
                }
                mGotRelatedTracks = true;
            }
        });
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(PlayQueue.EXTRA, mPlayQueue);
        mContext.sendBroadcast(intent);
    }

    private void broadcastRelatedLoadStateChanged() {
        final Intent intent = new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED)
                .putExtra(PlayQueue.EXTRA, mPlayQueue);
        mContext.sendBroadcast(intent);
    }

    public static class ResumeInfo {
        private long mTrackId;
        private long mTime;

        public ResumeInfo(long trackId, long time) {
            this.mTrackId = trackId;
            this.mTime = time;
        }

        public long getTrackId() {
            return mTrackId;
        }

        public long getTime() {
            return mTime;
        }
    }

}

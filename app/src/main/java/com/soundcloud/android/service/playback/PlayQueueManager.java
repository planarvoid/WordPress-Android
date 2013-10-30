package com.soundcloud.android.service.playback;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.service.playback.PlayQueue.AppendState;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class PlayQueueManager implements Observer<RelatedTracksCollection> {

    @VisibleForTesting
    protected static final String PLAYQUEUE_URI_PREF_KEY = "sc_playlist_uri";

    private final Context mContext;
    private final PlayQueueStorage mPlayQueueStorage;
    private final SharedPreferences mSharedPreferences;
    private final ExploreTracksOperations mExploreTrackOperations;
    private final ScModelManager mModelManager;

    private PlayQueue mPlayQueue = PlayQueue.EMPTY;
    private Subscription mFetchRelatedSubscription = Subscriptions.empty();
    private Subscription mPlayQueueSubscription = Subscriptions.empty();
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    private boolean mGotRelatedTracks;

    public PlayQueueManager(Context context, PlayQueueStorage playQueueStorage, ExploreTracksOperations exploreTracksOperations,
                            SharedPreferences sharedPreferences, ScModelManager modelManager) {
        mContext = context;
        mPlayQueueStorage = playQueueStorage;
        mExploreTrackOperations = exploreTracksOperations;
        mSharedPreferences = sharedPreferences;
        mModelManager = modelManager;
    }

    public void setNewPlayQueue(PlayQueue playQueue) {
        stopLoadingOperations();

        mPlayQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        mPlayQueue.setCurrentTrackToUserTriggered();
        broadcastPlayQueueChanged();

        saveCurrentPosition(0L);
        mPlayQueueStorage.storeAsync(mPlayQueue).subscribe(DefaultObserver.NOOP_OBSERVER);
    }

    public void saveCurrentPosition(long currentTrackProgress) {
        if (!mPlayQueue.isEmpty()) {
            final String playQueueState = mPlayQueue.getPlayQueueState(currentTrackProgress, mPlayQueue.getCurrentTrackId()).toString();
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putString(PLAYQUEUE_URI_PREF_KEY, playQueueState));
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public ResumeInfo loadPlayQueue() {
        final String lastUri = mSharedPreferences.getString(PLAYQUEUE_URI_PREF_KEY, null);
        if (ScTextUtils.isNotBlank(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mPlayQueueSubscription = mPlayQueueStorage.getPlayQueueAsync(
                        playQueueUri.getPos(), playQueueUri.getPlaySourceInfo())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<PlayQueue>() {
                            @Override
                            public void call(PlayQueue playQueue) {
                                setNewPlayQueue(playQueue);
                            }
                        });
                return new ResumeInfo(trackId, seekPos);
            } else {
                final String message = "Unexpected track id when reloading playqueue: " + trackId;
                SoundCloudApplication.handleSilentException(message, new IllegalArgumentException(message));
            }
        } else {
            // this is so the player can finish() instead of display waiting to the user
            broadcastPlayQueueChanged();
        }

        return null;
    }

    public boolean shouldReloadQueue(){
        return mPlayQueue.isEmpty();
    }

    public void fetchRelatedTracks(long trackId){
        mRelatedTracksObservable = mExploreTrackOperations.getRelatedTracks(trackId);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(){
        SharedPreferencesUtils.apply(mSharedPreferences.edit().remove(PLAYQUEUE_URI_PREF_KEY));
        mPlayQueueStorage.clearState();
        mPlayQueue = PlayQueue.EMPTY;
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;
    }

    private void loadRelatedTracks() {
        setNewRelatedLoadingState(AppendState.LOADING);
        mGotRelatedTracks = false;
        mFetchRelatedSubscription = mRelatedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RelatedTracksCollection relatedTracks) {
        mPlayQueue.getPlaySourceInfo().setRecommenderVersion(relatedTracks.getSourceVersion());
        for (TrackSummary item : relatedTracks) {
            final Track track = new Track(item);
            mModelManager.cache(track);
            mPlayQueue.addTrackId(track.getId());
        }
        mGotRelatedTracks = true;
    }

    @Override
    public void onCompleted() {
        // TODO, save new tracks to database
        setNewRelatedLoadingState(mGotRelatedTracks ? AppendState.IDLE : AppendState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(AppendState.ERROR);
    }

    private void setNewRelatedLoadingState(AppendState appendState) {
        mPlayQueue.setAppendState(appendState);
        final Intent intent = new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED)
                .putExtra(PlayQueue.EXTRA, mPlayQueue);
        mContext.sendBroadcast(intent);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(PlayQueue.EXTRA, mPlayQueue);
        mContext.sendBroadcast(intent);
    }

    private void stopLoadingOperations() {
        mFetchRelatedSubscription.unsubscribe();
        mFetchRelatedSubscription = Subscriptions.empty();

        mPlayQueueSubscription.unsubscribe();
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

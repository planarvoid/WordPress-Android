package com.soundcloud.android.playback.service;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.rx.observers.RxObserverHelper.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueManager implements Observer<RelatedTracksCollection> {

    @VisibleForTesting
    protected static final String PLAYQUEUE_URI_PREF_KEY = "sc_playlist_uri";

    private final Context mContext;
    private final PlayQueueStorage mPlayQueueStorage;
    private final SharedPreferences mSharedPreferences;
    private final ScModelManager mModelManager;
    private final PlaybackOperations mPlaybackOperations;

    private PlayQueue mPlayQueue = PlayQueue.EMPTY;
    private Subscription mFetchRelatedSubscription = Subscriptions.empty();
    private Subscription mPlayQueueSubscription = Subscriptions.empty();
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    private boolean mGotRelatedTracks;
    private PlaybackOperations.AppendState mAppendState = PlaybackOperations.AppendState.IDLE;

    @Inject
    public PlayQueueManager(Context context, PlayQueueStorage playQueueStorage, PlaybackOperations playbackOperations,
                            SharedPreferences sharedPreferences, ScModelManager modelManager) {
        mContext = context;
        mPlayQueueStorage = playQueueStorage;
        mPlaybackOperations = playbackOperations;
        mSharedPreferences = sharedPreferences;
        mModelManager = modelManager;
    }

    public void setNewPlayQueue(PlayQueue playQueue) {
        stopLoadingOperations();

        mPlayQueue = checkNotNull(playQueue, "Playqueue to update should not be null");
        mPlayQueue.setCurrentTrackToUserTriggered();
        broadcastPlayQueueChanged();

        saveCurrentPosition(0L);
        fireAndForget(mPlayQueueStorage.storeCollectionAsync(mPlayQueue.getItems()));
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
    public PlaybackProgressInfo loadPlayQueue() {
        final String lastUri = mSharedPreferences.getString(PLAYQUEUE_URI_PREF_KEY, null);
        if (ScTextUtils.isNotBlank(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mPlayQueueSubscription = getLastStoredPlayQueue(playQueueUri.getPlaySessionSource());
                return new PlaybackProgressInfo(trackId, seekPos);
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
        mRelatedTracksObservable = mPlaybackOperations.getRelatedTracks(trackId);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(){
        clearPlayQueueUri(mSharedPreferences);
        mPlayQueueStorage.clearState();
        mPlayQueue = PlayQueue.EMPTY;
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;
    }

    public PlayQueueView getPlayQueueView() {
        return mPlayQueue.getViewWithAppendState(mAppendState);
    }

    private void loadRelatedTracks() {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.LOADING);
        mGotRelatedTracks = false;
        mFetchRelatedSubscription = mRelatedTracksObservable.subscribe(this);
    }

    @Override
    public void onNext(RelatedTracksCollection relatedTracks) {
        for (TrackSummary item : relatedTracks) {
            final Track track = new Track(item);
            mModelManager.cache(track);
            mPlayQueue.addTrack(track.getId(), PlaySessionSource.DiscoverySource.RECOMMENDER.value(),
                    relatedTracks.getSourceVersion());
        }
        mGotRelatedTracks = true;
    }

    @Override
    public void onCompleted() {
        // TODO, save new tracks to database
        setNewRelatedLoadingState(mGotRelatedTracks ? PlaybackOperations.AppendState.IDLE : PlaybackOperations.AppendState.EMPTY);
    }

    @Override
    public void onError(Throwable e) {
        setNewRelatedLoadingState(PlaybackOperations.AppendState.ERROR);
    }

    private Subscription getLastStoredPlayQueue(final PlaySessionSource playSessionSource) {
        return mPlayQueueStorage.getPlayQueueItemsAsync()
                .observeOn(AndroidSchedulers.mainThread()).map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                    @Override
                    public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                        return new PlayQueue(playQueueItems, playSessionSource);
                    }

                }).subscribe(new Action1<PlayQueue>() {
                    @Override
                    public void call(PlayQueue playQueue) {
                        setNewPlayQueue(playQueue);
                    }
                });
    }

    private void setNewRelatedLoadingState(PlaybackOperations.AppendState appendState) {
        mAppendState = appendState;
        final Intent intent = new Intent(PlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, mPlayQueue.getViewWithAppendState(appendState));
        mContext.sendBroadcast(intent);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(PlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(PlayQueueView.EXTRA, mPlayQueue.getViewWithAppendState(mAppendState));
        mContext.sendBroadcast(intent);
    }

    private void stopLoadingOperations() {
        mFetchRelatedSubscription.unsubscribe();
        mFetchRelatedSubscription = Subscriptions.empty();

        mPlayQueueSubscription.unsubscribe();
    }

    public static void clearPlayQueueUri(SharedPreferences defaultSharedPreferences){
        SharedPreferencesUtils.apply(defaultSharedPreferences.edit().remove(PLAYQUEUE_URI_PREF_KEY));
    }
}

package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.PlayQueue.AppendState;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.observers.DefaultObserver;
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

    private final Context mContext;
    private final PlayQueueStorage mPlayQueueStorage;
    private final SharedPreferences mSharedPreferences;
    private final ExploreTracksOperations mExploreTrackOperations;

    private PlayQueue mPlayQueue = PlayQueue.EMPTY;
    private Subscription mFetchRelatedSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    public PlayQueueManager(Context context, PlayQueueStorage playQueueStorage, ExploreTracksOperations exploreTracksOperations, SharedPreferences sharedPreferences) {
        mContext = context;
        mPlayQueueStorage = playQueueStorage;
        mExploreTrackOperations = exploreTracksOperations;
        mSharedPreferences = sharedPreferences;
    }

    public void setNewPlayQueue(PlayQueue playQueue) {
        if (mFetchRelatedSubscription != null){
            mFetchRelatedSubscription.unsubscribe();
        }

        mPlayQueue = playQueue;
        broadcastPlayQueueChanged();

        saveCurrentPosition(0L);
        mPlayQueueStorage.storeAsync(mPlayQueue).subscribe(DefaultObserver.NOOP_OBSERVER);
    }

    public void saveCurrentPosition(long currentTrackProgress) {
        if (!mPlayQueue.isEmpty()) {
            final String playQueueState = mPlayQueue.getPlayQueueState(currentTrackProgress, mPlayQueue.getCurrentTrackId()).toString();
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, playQueueState));
        }
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public long reloadPlayQueue() {

        final String lastUri = mSharedPreferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);
        if (!TextUtils.isEmpty(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mPlayQueueStorage.getTrackIds().subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> trackIds) {
                        setNewPlayQueue(new PlayQueue(trackIds, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo()));
                    }
                });
            }
            return seekPos;
        } else {
            if (TextUtils.isEmpty(lastUri)) {
                // this is so the player can finish() instead of display waiting to the user
                broadcastPlayQueueChanged();
            }
            return -1; // seekpos
        }
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
        mSharedPreferences.edit().remove(Consts.PrefKeys.SC_PLAYQUEUE_URI).commit();
        mPlayQueueStorage.clearState();
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;
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


}

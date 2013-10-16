package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.PlayQueue.*;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

public class PlayQueueManager {

    private Context mContext;
    private PlayQueueStorage mPlayQueueStorage;
    private SharedPreferences mSharedPreferences;
    private final ExploreTracksOperations mExploreTrackOperations;
    private PlayQueue mPlayQueue = PlayQueue.EMPTY;

    private Subscription mLoadingSubscription;
    private Subscription mFetchRelatedSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;

    public PlayQueueManager(Context mContext, PlayQueueStorage mPlayQueueStorage, ExploreTracksOperations exploreTracksOperations, SharedPreferences mSharedPreferences) {
        this.mContext = mContext;
        this.mPlayQueueStorage = mPlayQueueStorage;
        this.mSharedPreferences = mSharedPreferences;
        this.mExploreTrackOperations = exploreTracksOperations;
    }

    public void loadFromNewQueue(PlayQueue playQueue) {
        stopLoadingRelatedTracks();
        mPlayQueue = playQueue;
        broadcastPlayQueueChanged(playQueue);
    }

    public void savePlayQueueMetadata(PlayQueue playQueue, long seekPos) {
        final long currentTrackId = playQueue.getCurrentTrackId();
        if (currentTrackId != -1) {
            final String playQueueState = playQueue.getPlayQueueState(seekPos, currentTrackId).toString();
            Log.d(CloudPlaybackService.TAG, "Saving playqueue state: " + playQueueState);
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, playQueueState));
        }
    }

    public void savePlayQueue(PlayQueue playQueue, long seekPos) {
        savePlayQueueMetadata(playQueue, seekPos);
        mPlayQueueStorage.storeAsync(playQueue).subscribe(DefaultObserver.NOOP_OBSERVER);
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public long reloadPlayQueue(final PlayQueue playQueue) {

        final String lastUri = mSharedPreferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);
        if (!TextUtils.isEmpty(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mPlayQueueStorage.getTrackIds().subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> trackIds) {
                        loadFromNewQueue(new PlayQueue(trackIds, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo()));
                    }
                });
            }
            return seekPos;
        } else {
            if (TextUtils.isEmpty(lastUri)) {
                // this is so the player can finish() instead of display waiting to the user
                broadcastPlayQueueChanged(playQueue);
            }
            return -1; // seekpos
        }
    }

    public void fetchRelatedTracks(Track track){
        mRelatedTracksObservable = mExploreTrackOperations.getRelatedTracks(track);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    public void clearAll(Context context){
        clearLastPlayed(context);
        mPlayQueueStorage.clearState();
    }

    public void clearLastPlayed(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(Consts.PrefKeys.SC_PLAYQUEUE_URI)
                .commit();
    }

    public PlayQueue getCurrentPlayQueue() {
        return mPlayQueue;  //To change body of created methods use File | Settings | File Templates.
    }

    private void loadRelatedTracks() {

        mPlayQueue.setRelatedLoadingState(AppendState.LOADING);
        mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));

        mFetchRelatedSubscription = mRelatedTracksObservable.subscribe(new Observer<RelatedTracksCollection>() {

            private boolean mGotRelatedTracks;

            @Override
            public void onCompleted() {
                mPlayQueue.setRelatedLoadingState(mGotRelatedTracks ? AppendState.IDLE : AppendState.EMPTY);
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
            }

            @Override
            public void onError(Throwable e) {
                mPlayQueue.setRelatedLoadingState(AppendState.ERROR);
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
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

    private void stopLoadingRelatedTracks() {
        mPlayQueue.setRelatedLoadingState(AppendState.IDLE);
        if (mFetchRelatedSubscription != null){
            mFetchRelatedSubscription.unsubscribe();
        }
    }

    private void broadcastPlayQueueChanged(PlayQueue playQueue) {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED).putExtra(PlayQueue.EXTRA, playQueue);
        mContext.sendBroadcast(intent);
    }


}

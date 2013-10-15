package com.soundcloud.android.service.playback;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayQueueManager {
    private List<Long> mPlayQueue = new ArrayList<Long>();
    private PlayQueueUri mPlayQueueUri = new PlayQueueUri();
    private final PlayQueueStorage mPlayQueueStorage;

    private final ExploreTracksOperations mExploreTrackOperations;
    private final TrackStorage mTrackStorage;

    private int mPlayPos;
    private final Context mContext;

    private long mUserId;
    private Subscription mLoadingSubscription = Subscriptions.empty();

    private PlayQueueState.AppendState mAppendingState = PlayQueueState.AppendState.IDLE;
    private Subscription mRelatedSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;
    @NotNull
    private PlaySourceInfo mCurrentPlaySourceInfo = PlaySourceInfo.EMPTY;

    public PlayQueueManager(Context context){
        this(context, SoundCloudApplication.getUserId(), new ExploreTracksOperations(), new TrackStorage());
    }

    @VisibleForTesting
    protected PlayQueueManager(Context context, long userId, ExploreTracksOperations exploreTracksOperations, TrackStorage trackStorage) {
        mContext = context;
        mUserId = userId;
        mExploreTrackOperations = exploreTracksOperations;
        mPlayQueueStorage = new PlayQueueStorage();
        mTrackStorage = trackStorage;

    }

    public PlayQueueState getState() {
        return new PlayQueueState(mPlayQueue, mPlayPos, mAppendingState);
    }

    public int length() {
        return mPlayQueue.size();
    }
    public boolean isEmpty() {
        return mPlayQueue.size() == 0;
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mPlayQueue.size()) {
            mPlayPos = playPos;
            return true;
        } else {
            return false;
        }
    }

    public Observable<Track> getCurrentTrack() {
        return getTrackAt(mPlayPos);
    }

    public long getCurrentTrackId() {
        return getTrackIdAt(mPlayPos);
    }

    public long getTrackIdAt(int playPos){
        return mPlayQueue.get(playPos);
    }

    /**
     * TODO : We need to figure out how to decouple event logger params from the playqueue
     */
    public String getCurrentEventLoggerParams() {

        final TrackSourceInfo trackSourceInfo = mCurrentPlaySourceInfo.getTrackSourceById(getCurrentTrackId());
        return trackSourceInfo.createEventLoggerParams(getCurrentPlaySourceInfo());
    }

    public boolean prev() {
        if (mPlayPos > 0) {
            mPlayPos--;
            return true;
        }
        return false;
    }

    public Boolean next() {
        if (mPlayPos < mPlayQueue.size() - 1) {
            mPlayPos++;
            return true;
        }
        return false;
    }

    public Observable<Track> getPrev() {
        if (mPlayPos > 0) {
            return getTrackAt(mPlayPos - 1);
        } else {
            return null;
        }
    }

    public Observable<Track> getNext() {
        if (mPlayPos < length() - 1) {
            return getTrackAt(mPlayPos + 1);
        } else {
            return null;
        }
    }

    @VisibleForTesting
    PlaySourceInfo getCurrentPlaySourceInfo(){
        return mCurrentPlaySourceInfo;
    }

    public void loadTrack(Track toBePlayed, boolean saveQueue, PlaySourceInfo trackingInfo) {
        stopLoadingTasks();

        mCurrentPlaySourceInfo = trackingInfo == null ? PlaySourceInfo.EMPTY : trackingInfo;
        SoundCloudApplication.MODEL_MANAGER.cache(toBePlayed, ScResource.CacheUpdateMode.NONE);
        mPlayQueue.clear();
        mPlayQueue.add(toBePlayed.getId());
        mPlayQueueUri = new PlayQueueUri();
        mPlayPos = 0;

        broadcastPlayQueueChanged();
        if (saveQueue) saveQueue(0, true);
    }

    private Observable<Track> getTrackAt(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mTrackStorage.getTrack(mPlayQueue.get(pos));
        } else {
            return null;
        }
    }

    private void stopLoadingTasks() {
        stopLoadingRelatedTracks();
        mLoadingSubscription.unsubscribe();
    }

    public void fetchRelatedTracks(Track track){
        mRelatedTracksObservable = mExploreTrackOperations.getRelatedTracks(track);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    private void loadRelatedTracks() {
        mAppendingState = PlayQueueState.AppendState.LOADING;
        mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
        mRelatedSubscription = mRelatedTracksObservable.subscribe(new Observer<RelatedTracksCollection>() {

            private boolean mGotRelatedTracks;

            @Override
            public void onCompleted() {
                mAppendingState = mGotRelatedTracks ? PlayQueueState.AppendState.IDLE : PlayQueueState.AppendState.EMPTY;
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
            }

            @Override
            public void onError(Throwable e) {
                mAppendingState = PlayQueueState.AppendState.ERROR;
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
            }

            @Override
            public void onNext(RelatedTracksCollection relatedTracks) {
                final String recommenderVersion = relatedTracks.getSourceVersion();
                mCurrentPlaySourceInfo.setRecommenderVersion(recommenderVersion);
                for (TrackSummary item : relatedTracks) {
                    SoundCloudApplication.MODEL_MANAGER.cache(new Track(item));
                    mPlayQueue.add(item.getId());
                }
                mGotRelatedTracks = true;
            }
        });
    }

    private void stopLoadingRelatedTracks() {
        mAppendingState = PlayQueueState.AppendState.IDLE;
        if (mRelatedSubscription != null){
            mRelatedSubscription.unsubscribe();
        }
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlayQueueFromTrackIds(final long[] trackIds, int playPos, PlaySourceInfo trackingInfo) {
        mCurrentPlaySourceInfo = trackingInfo;
        mPlayQueueUri = new PlayQueueUri();

        List<Long> newQueue = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) newQueue.add(n);

        setPlayQueueInternal(newQueue, playPos);
        saveQueue(0, true);
    }

    private void setPlayQueueInternal(List<Long> playQueue, int playPos) {
        mPlayQueue = playQueue;
        if (playPos >= 0 && playPos <= mPlayQueue.size() - 1){
            mPlayPos = playPos;
        } else {
            // invalid play position, default to 0
            mPlayPos = 0;
            Log.e(this, "Unexpected queue position [" + playPos + "]");
        }
        broadcastPlayQueueChanged();
    }

    private void persistPlayQueue() {
        if (mUserId < 0) return;

        new ParallelAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final List<Long> playQueueItems = new ArrayList<Long>(mPlayQueue);
                mPlayQueueStorage.insertQueue(playQueueItems, mUserId);
                return null;
            }
        }.executeOnThreadPool((Void[]) null);
    }

    public Uri getUri() {
        return mPlayQueueUri.uri;
    }

    /**
     * Handles the case where a local playlist has been sent to the API and has a new ID (URI) locally
     */
    public static void onPlaylistUriChanged(Context context, Uri oldUri, Uri newUri) {
        onPlaylistUriChanged(CloudPlaybackService.getPlaylistManager(),context,oldUri,newUri);
    }

    public static void onPlaylistUriChanged(PlayQueueManager playQueueManager, Context context, Uri oldUri, Uri newUri) {
        if (playQueueManager != null) {
            // update in memory
            playQueueManager.onPlaylistUriChanged(oldUri,newUri);

        } else {
            // update saved uri
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final String lastUri = preferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);
            if (!TextUtils.isEmpty(lastUri)){
                PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
                if (playQueueUri.uri.getPath().equals(oldUri.getPath())) {
                    Uri replacement = new PlayQueueUri(newUri).toUri(playQueueUri.getTrackId(), playQueueUri.getPos(), playQueueUri.getSeekPos(), playQueueUri.getPlaySourceInfo());
                    SharedPreferencesUtils.apply(preferences.edit().putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, replacement.toString()));
                }
            }
        }

    }

    private void onPlaylistUriChanged(Uri oldUri, Uri newUri) {
        final Uri loadedUri = getUri();
        if (loadedUri != null && loadedUri.getPath().equals(oldUri.getPath())) mPlayQueueUri = new PlayQueueUri(newUri);
    }

    public void clear() {
        mPlayQueue.clear();
        mCurrentPlaySourceInfo = PlaySourceInfo.EMPTY;
    }

    public void saveQueue(long seekPos) {
        saveQueue(seekPos, false);
    }

    public void saveQueue(long seekPos, boolean persistTracks) {
        final long currentTrackId = getCurrentTrackId();
        if (currentTrackId != -1 && SoundCloudApplication.getUserIdFromContext(mContext) >= 0) {
            if (persistTracks) persistPlayQueue();
            SharedPreferencesUtils.apply(PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, getPlayQueueState(seekPos, currentTrackId).toString()));
        }
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return mPlayQueueUri.toUri(currentTrackId, mPlayPos, seekPos, mCurrentPlaySourceInfo);
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public long reloadQueue() {
        // TODO : StrictMode policy violation; ~duration=139 ms: android.os.StrictMode$StrictModeDiskReadViolation: policy=23 violation=2
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String lastUri = preferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);

//        if (AndroidUtils.isTaskFinished(mLoadTask) && !TextUtils.isEmpty(lastUri)) {
//            PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
//            long seekPos      = playQueueUri.getSeekPos();
//            final long trackId = playQueueUri.getTrackId();
//            if (trackId > 0) {
//                loadUri(playQueueUri.uri, playQueueUri.getPos(), new long[]{trackId}, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo());
//                // adjust play position if it has changed
//                if (getCurrentTrack() != null && getCurrentTrackId() != trackId && playQueueUri.isCollectionUri()) {
//                    final int newPos = mPlayQueueStorage.getPlayQueuePositionFromUri(playQueueUri.uri, trackId);
//                    if (newPos == -1) seekPos = 0;
//                    setPosition(Math.max(newPos, 0));
//                }
//            }
//            return seekPos;
//        } else {
//            if (TextUtils.isEmpty(lastUri)) {
//                // this is so the player can finish() instead of display waiting to the user
//                broadcastPlayQueueChanged();
//            }
//            return -1; // seekpos
//        }
        return -1;
    }

    public void clearAllLocalState() {
        clear();
        clearLastPlayed(mContext);
        new PlayQueueStorage().clearState();
    }

    public void clearLastPlayed(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(Consts.PrefKeys.SC_PLAYQUEUE_URI)
                .commit();
    }

    public void onDestroy() {
        // nop
    }
}

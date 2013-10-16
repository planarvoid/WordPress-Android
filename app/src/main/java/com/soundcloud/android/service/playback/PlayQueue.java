package com.soundcloud.android.service.playback;


import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
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

public class PlayQueue {
    private List<Long> mTrackIds = new ArrayList<Long>();
    private PlayQueueUri mPlayQueueUri = new PlayQueueUri();

    private final ExploreTracksOperations mExploreTrackOperations;
    private final TrackStorage mTrackStorage;

    private int mPlayPos;
    private final Context mContext;

    private Subscription mLoadingSubscription = Subscriptions.empty();

    private PlayQueueState.AppendState mAppendingState = PlayQueueState.AppendState.IDLE;
    private Subscription mRelatedSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;
    @NotNull
    private PlaySourceInfo mCurrentPlaySourceInfo = PlaySourceInfo.EMPTY;

    public PlayQueue(Context context){
        this(context, SoundCloudApplication.getUserId(), new ExploreTracksOperations(), new TrackStorage());
    }

    @VisibleForTesting
    protected PlayQueue(Context context, long userId, ExploreTracksOperations exploreTracksOperations, TrackStorage trackStorage) {
        mContext = context;
        mExploreTrackOperations = exploreTracksOperations;
        mTrackStorage = trackStorage;

    }

    public List<Long> getTrackIds() {
        return mTrackIds;
    }

    public PlayQueueState getState() {
        return new PlayQueueState(mTrackIds, mPlayPos, mAppendingState);
    }

    public int length() {
        return mTrackIds.size();
    }
    public boolean isEmpty() {
        return mTrackIds.isEmpty();
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mTrackIds.size()) {
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
        return mTrackIds.get(playPos);
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
        if (mPlayPos < mTrackIds.size() - 1) {
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

    private Observable<Track> getTrackAt(int pos) {
        if (pos >= 0 && pos < mTrackIds.size()) {
            return mTrackStorage.getTrack(mTrackIds.get(pos));
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
                    mTrackIds.add(item.getId());
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

    public void setFromNewQueueState(PlayQueueState playQueueState) {
        mCurrentPlaySourceInfo = playQueueState.getPlaySourceInfo();
        mPlayQueueUri = new PlayQueueUri();
        mTrackIds = playQueueState.getCurrentTrackIds();
        mPlayPos = playQueueState.getPlayPosition();
    }

    public Uri getUri() {
        return mPlayQueueUri.uri;
    }

    /**
     * Handles the case where a local playlist has been sent to the API and has a new ID (URI) locally
     */
    public static void onPlaylistUriChanged(Context context, Uri oldUri, Uri newUri) {
        onPlaylistUriChanged(CloudPlaybackService.getPlayQueue(),context,oldUri,newUri);
    }

    public static void onPlaylistUriChanged(PlayQueue playQueue, Context context, Uri oldUri, Uri newUri) {
        if (playQueue != null) {
            // update in memory
            playQueue.onPlaylistUriChanged(oldUri,newUri);

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
        mTrackIds.clear();
        mCurrentPlaySourceInfo = PlaySourceInfo.EMPTY;
    }

    /* package */ Uri getPlayQueueState(long seekPos, long currentTrackId) {
        return mPlayQueueUri.toUri(currentTrackId, mPlayPos, seekPos, mCurrentPlaySourceInfo);
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
}

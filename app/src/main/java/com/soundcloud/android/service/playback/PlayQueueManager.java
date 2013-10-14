package com.soundcloud.android.service.playback;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.dao.PlayQueueManagerStore;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class PlayQueueManager {
    private List<PlayQueueItem> mPlayQueue = new ArrayList<PlayQueueItem>();
    private PlayQueueUri mPlayQueueUri = new PlayQueueUri();
    private final PlayQueueManagerStore mPlayQueueDAO;
    private final ExploreTracksOperations mExploreTrackOperations;

    private int mPlayPos;
    private final Context mContext;

    private long mUserId;
    private AsyncTask mLoadTask;
    private AppendState mAppendingState = AppendState.IDLE;
    private Subscription mRelatedSubscription;
    private Observable<RelatedTracksCollection> mRelatedTracksObservable;
    @NotNull
    private PlaySourceInfo mCurrentPlaySourceInfo = PlaySourceInfo.EMPTY;

    private enum AppendState {
        IDLE, LOADING, ERROR, EMPTY;

    }
    private static PlayQueueManager instance;

    public static PlayQueueManager get(Context context){
        return get(context, SoundCloudApplication.getUserId());
    }

    public static PlayQueueManager get(Context context, long userId) {
        return get(context, userId, new ExploreTracksOperations());
    }

    public static PlayQueueManager get(Context context, long userId, ExploreTracksOperations operations){
        if (instance == null){
            instance = new PlayQueueManager(context, userId, operations);
        }
        return instance;
    }

    @VisibleForTesting
    protected PlayQueueManager(Context context, long userId, ExploreTracksOperations exploreTracksOperations) {
        mContext = context;
        mUserId = userId;
        mExploreTrackOperations = exploreTracksOperations;
        mPlayQueueDAO = new PlayQueueManagerStore();

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

    public Track getCurrentTrack() {
        return getTrackAt(mPlayPos);
    }

    public long getCurrentTrackId() {
        final Track currentTrack = getCurrentTrack();
        return currentTrack == null ? -1 : currentTrack.getId();
    }

    /**
     * TODO : We need to figure out how to decouple event logger params from the playqueue
     */
    public String getCurrentEventLoggerParams() {
        final PlayQueueItem currentPlayQueueItem = getPlayQueueItem(mPlayPos);
        final TrackSourceInfo trackSourceInfo = currentPlayQueueItem == null ? TrackSourceInfo.EMPTY : currentPlayQueueItem.getTrackSourceInfo();
        return trackSourceInfo.createEventLoggerParams(getCurrentPlaySourceInfo());
    }

    public PlayQueueItem getPlayQueueItem(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos);
        } else {
            return null;
        }
    }

    public boolean prev() {
        if (mPlayPos > 0) {
            int newPos = mPlayPos - 1;
            Track newTrack = getTrackAt(newPos);
            while (newPos > 0 && (newTrack == null || !newTrack.isStreamable())) {
                newTrack = getTrackAt(--newPos);
            }
            if (newTrack != null && newTrack.isStreamable()) {
                mPlayPos = newPos;
                return true;
            }
        }
        return false;
    }

    public Boolean next() {
        if (mPlayPos < mPlayQueue.size() - 1) {
            int newPos = mPlayPos + 1;
            Track newTrack = getTrackAt(newPos);
            while (newPos < mPlayQueue.size() - 1 && (newTrack == null || !newTrack.isStreamable())) {
                newTrack = getTrackAt(++newPos);
            }
            if (newTrack != null && newTrack.isStreamable()) {
                mPlayPos = newPos;
                return true;
            }
        }

        return false;
    }

    public Track getPrev() {
        if (mPlayPos > 0) {
            return getTrackAt(mPlayPos - 1);
        } else {
            return null;
        }
    }

    public Track getNext() {
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
        mPlayQueue.add(new PlayQueueItem(toBePlayed, 0, TrackSourceInfo.manual()));
        mPlayQueueUri = new PlayQueueUri();
        mPlayPos = 0;

        broadcastPlayQueueChanged();
        if (saveQueue) saveQueue(0, true);
    }

    public void loadUri(Uri uri, int position, @Nullable Track initialTrack, PlaySourceInfo trackingInfo) {
        List<PlayableHolder> initialQueue;
        if (initialTrack != null) {
            initialQueue = Lists.<PlayableHolder>newArrayList(initialTrack);
        } else {
            initialQueue = Lists.newArrayList();
        }
        loadUri(uri, position, initialQueue, 0, trackingInfo);
    }

    /**
     * @param uri               the playqueue uri to load
     * @param position          position within playqueue
     * @param initialPlayQueue  initial queue, load full uri asynchronously.
     * @param initialPlayPos    initial play position for initial queue (play position might be different after loading)
     * @param trackingInfo      data with this current queue to be passed to the {@link com.soundcloud.android.tracking.eventlogger.PlayEventTracker}
     */
    public void loadUri(Uri uri, int position, List<? extends PlayableHolder> initialPlayQueue, int initialPlayPos, PlaySourceInfo trackingInfo) {
        stopLoadingTasks();

        setPlayQueue(initialPlayQueue, initialPlayPos, trackingInfo);
        mPlayQueueUri = new PlayQueueUri(uri);

        // if playlist, adjust load uri to request the tracks instead of meta_data
        if (Content.match(uri) == Content.PLAYLIST){
            uri = Content.PLAYLIST_TRACKS.forQuery(uri.getLastPathSegment());
        }
        if (uri != null) {
            mLoadTask = loadCursor(uri, position);
        }
    }

    private Track getTrackAt(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos).getTrack();
        } else {
            return null;
        }
    }

    private void stopLoadingTasks() {
        stopLoadingRelatedTracks();
        if (mLoadTask != null && !AndroidUtils.isTaskFinished(mLoadTask)){
            mLoadTask.cancel(false);
        }
    }

    public void fetchRelatedTracks(Track track){
        mRelatedTracksObservable = mExploreTrackOperations.getRelatedTracks(track);
        loadRelatedTracks();
    }

    public void retryRelatedTracksFetch(){
        loadRelatedTracks();
    }

    private void loadRelatedTracks() {
        mAppendingState = AppendState.LOADING;
        mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
        mRelatedSubscription = mRelatedTracksObservable.subscribe(new Observer<RelatedTracksCollection>() {

            private boolean mGotRelatedTracks;

            @Override
            public void onCompleted() {
                mAppendingState = mGotRelatedTracks ? AppendState.IDLE : AppendState.EMPTY;
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
            }

            @Override
            public void onError(Throwable e) {
                mAppendingState = AppendState.ERROR;
                mContext.sendBroadcast(new Intent(CloudPlaybackService.Broadcasts.RELATED_LOAD_STATE_CHANGED));
            }

            @Override
            public void onNext(RelatedTracksCollection relatedTracks) {
                final String recommenderVersion = relatedTracks.getSourceVersion();
                mCurrentPlaySourceInfo.setRecommenderVersion(recommenderVersion);
                for (TrackSummary item : relatedTracks) {
                    mPlayQueue.add(new PlayQueueItem(new Track(item), mPlayQueue.size(), TrackSourceInfo.fromRecommender(recommenderVersion)));
                }
                mGotRelatedTracks = true;
            }
        });
    }

    private void stopLoadingRelatedTracks() {
        mAppendingState = AppendState.IDLE;
        if (mRelatedSubscription != null){
            mRelatedSubscription.unsubscribe();
        }
    }

    public boolean isFetchingRelated() {
        return mAppendingState == AppendState.LOADING;
    }

    public boolean lastRelatedFetchFailed() {
        return mAppendingState == AppendState.ERROR;
    }

    public boolean lastRelatedFetchWasEmpty() {
        return mAppendingState == AppendState.EMPTY;
    }

    private AsyncTask loadCursor(final Uri uri, final int position) {
        return new ParallelAsyncTask<Uri,Void,List<Track>>() {
            @Override protected List<Track> doInBackground(Uri... params) {
                return new TrackStorage().getTracksForUri(params[0]);
            }

            @Override protected void onPostExecute(List<Track> newQueue) {
                if (newQueue != null && !isCancelled()){
                    final long playingId = getCurrentTrackId();
                    final boolean positionWithinBounds = position >= 0 && position < newQueue.size();

                    if (playingId == -1 || (positionWithinBounds && newQueue.get(position).getId() == playingId)){
                        setPlayQueueInternal(newQueue, position);
                    } else {
                        // correct play position as it has changed since it seems to be different than expected
                        setPlayQueueInternal(newQueue, getAdjustedTrackPosition(newQueue, playingId));
                    }
                }
            }

            private int getAdjustedTrackPosition(List<Track> newQueue, long playingId) {
                int adjustedPosition = -1;
                if (Content.match(uri).isCollectionItem()){
                    adjustedPosition = mPlayQueueDAO.getPlayQueuePositionFromUri(uri, playingId);
                } else {
                    /* adjust for deletions or new items and find the original track.
                    This is a really dumb sequential search. If there are duplicates in the list, it will probably
                     find the wrong one. */
                    adjustedPosition = 0;
                    while (adjustedPosition < newQueue.size() && newQueue.get(adjustedPosition).getId() != playingId) {
                        adjustedPosition++;
                    }
                }
                return adjustedPosition == -1 || adjustedPosition >= newQueue.size() ? 0 : adjustedPosition;
            }

        }.executeOnThreadPool(uri);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlayQueue(final List<? extends PlayableHolder> playQueue, int playPos, PlaySourceInfo trackingInfo) {
        mCurrentPlaySourceInfo = trackingInfo;
        mPlayQueueUri = new PlayQueueUri();
        setPlayQueueInternal(playQueue, playPos);
        saveQueue(0, true);
    }

    private void setPlayQueueInternal(List<? extends PlayableHolder> playQueue, int playPos) {
        mPlayQueue.clear();

        if (playQueue != null) {
            for (PlayableHolder playableHolder : playQueue) {
                final Playable playable = playableHolder.getPlayable();
                if (playable instanceof Track){
                    final TrackSourceInfo trackSourceInfo = mCurrentPlaySourceInfo.getTrackSourceById(playable.getId());
                    mPlayQueue.add(new PlayQueueItem((Track) playable, mPlayQueue.size(), trackSourceInfo));
                }
            }
        }

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
                final List<PlayQueueItem> playQueueItems = new ArrayList<PlayQueueItem>(mPlayQueue);
                mPlayQueueDAO.insertQueue(playQueueItems, mUserId);
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
        onPlaylistUriChanged(PlayQueueManager.get(context),context,oldUri,newUri);
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

        if (AndroidUtils.isTaskFinished(mLoadTask) && !TextUtils.isEmpty(lastUri)) {
            PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            long seekPos      = playQueueUri.getSeekPos();
            final int trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(trackId);
                loadUri(playQueueUri.uri, playQueueUri.getPos(), t, playQueueUri.getPlaySourceInfo());
                // adjust play position if it has changed
                if (getCurrentTrack() != null && getCurrentTrack().getId() != trackId && playQueueUri.isCollectionUri()) {
                    final int newPos = mPlayQueueDAO.getPlayQueuePositionFromUri(playQueueUri.uri, trackId);
                    if (newPos == -1) seekPos = 0;
                    setPosition(Math.max(newPos, 0));
                }
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

    public void clearState() {
        new PlayQueueManagerStore().clearState();
        clearLastPlayed(mContext);
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

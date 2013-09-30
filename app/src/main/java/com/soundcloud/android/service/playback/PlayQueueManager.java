package com.soundcloud.android.service.playback;


import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.dao.PlayQueueManagerStore;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;
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
    private Observable<Track> mRelatedTracksObservable;

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

    public PlayQueueItem getPlayQueueItem(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos);
        } else {
            return null;
        }
    }

    @Deprecated
    public Track getTrackAt(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos).getTrack();
        } else {
            return null;
        }
    }

    public long getTrackIdAt(int pos) {
        Track t = getTrackAt(pos);
        return t== null ? -1 : t.getId();
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

    public void loadTrack(Track toBePlayed, boolean saveQueue) {
        stopLoadingRelatedTracks();
        SoundCloudApplication.MODEL_MANAGER.cache(toBePlayed, ScResource.CacheUpdateMode.NONE);
        mPlayQueue.clear();
        mPlayQueue.add(new PlayQueueItem(toBePlayed, 0));
        mPlayQueueUri = new PlayQueueUri();
        mPlayPos = 0;

        broadcastPlayQueueChanged();
        if (saveQueue) saveQueue(0, true);
    }

    public void loadUri(Uri uri, int position, long initialTrackId) {
        Track t = null;
        if (initialTrackId != -1) {
            t = SoundCloudApplication.MODEL_MANAGER.getCachedTrack(initialTrackId);
            // ensure that we have an initial track to load, should be cached to avoid this db hit on the UI
            if (t == null) {
                t = SoundCloudApplication.MODEL_MANAGER.getTrack(initialTrackId);
            }
        }
        loadUri(uri, position, t);
    }

    public void loadUri(Uri uri, int position, @Nullable Track initialTrack) {
        List<PlayableHolder> initialQueue = new ArrayList<PlayableHolder>();
        if (initialTrack != null) initialQueue.add(initialTrack);
        loadUri(uri, position, initialQueue, 0);
    }

    /**
     * @param uri               the playqueue uri to load
     * @param position          position within playqueue
     * @param initialPlayQueue  initial queue, load full uri asynchronously.
     * @param initialPlayPos    initial play position for initial queue.
     */
    public void loadUri(Uri uri, int position, List<? extends PlayableHolder> initialPlayQueue, int initialPlayPos) {
        stopLoadingRelatedTracks();

        if (mLoadTask != null && !AndroidUtils.isTaskFinished(mLoadTask)){
            mLoadTask.cancel(false);
        }

        if (initialPlayQueue != null) {
            setPlayQueue(initialPlayQueue, initialPlayPos);
        } else {
            // no track yet, load async
            mPlayQueue.clear();
            mPlayPos = 0;
        }

        mPlayQueueUri = new PlayQueueUri(uri);

        // if playlist, adjust load uri to request the tracks instead of meta_data
        if (Content.match(uri) == Content.PLAYLIST){
            uri = Content.PLAYLIST_TRACKS.forQuery(uri.getLastPathSegment());
        }
        if (uri != null) {
            mLoadTask = loadCursor(uri, position);
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
        mRelatedSubscription = mRelatedTracksObservable.subscribe(new Observer<Track>() {

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
            public void onNext(Track track) {
                mPlayQueue.add(new PlayQueueItem(track, mPlayQueue.size()));
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
                    if (position >= 0 && position < newQueue.size() &&  newQueue.get(position).getId() == playingId){
                        setPlayQueueInternal(newQueue, position);
                    } else {
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
                return adjustedPosition == -1 || adjustedPosition >= newQueue.size() ? position : adjustedPosition;
            }

        }.executeOnThreadPool(uri);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlayQueue(final List<? extends PlayableHolder> playQueue, int playPos) {
        mPlayQueueUri = new PlayQueueUri();
        setPlayQueueInternal(playQueue, playPos);
        saveQueue(0, true);
    }

    private void setPlayQueueInternal(List<? extends PlayableHolder> playQueue, int playPos) {
        mPlayQueue.clear();
        if (playQueue != null) {
            for (PlayableHolder playable : playQueue) {
                if (playable.getPlayable() instanceof Track){
                    mPlayQueue.add(new PlayQueueItem((Track) playable.getPlayable(), mPlayQueue.size()));
                }
            }
        }
        mPlayPos = Math.max(0, Math.min(mPlayQueue.size() - 1, playPos));
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
                    Uri replacement = new PlayQueueUri(newUri).toUri(playQueueUri.getTrackId(), playQueueUri.getPos(), playQueueUri.getSeekPos());
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
    }

    public void saveQueue(long seekPos) {
        saveQueue(seekPos, false);
    }

    public void saveQueue(long seekPos, boolean persistTracks) {
        if (SoundCloudApplication.getUserIdFromContext(mContext) >= 0) {
            if (persistTracks) persistPlayQueue();
            SharedPreferencesUtils.apply(PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, getPlayQueueState(seekPos).toString()));
        }
    }

    /* package */ Uri getPlayQueueState(long seekPos) {
        return mPlayQueueUri.toUri(getCurrentTrack(), mPlayPos, seekPos);
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
                loadUri(playQueueUri.uri, playQueueUri.getPos(), t);
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

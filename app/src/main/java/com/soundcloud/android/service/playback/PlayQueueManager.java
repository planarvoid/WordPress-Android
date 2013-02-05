package com.soundcloud.android.service.playback;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.task.ParallelAsyncTask;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PlayQueueManager {
    private List<PlayQueueItem> mPlayQueue = new ArrayList<PlayQueueItem>();
    private PlayQueueUri mPlayQueueUri = new PlayQueueUri();

    private int mPlayPos;
    private final Context mContext;

    private long mUserId;

    public static class PlayQueueItem {
        public final Track track;
        public int position;
        public final boolean manuallyAdded;


        public PlayQueueItem(Track track, boolean manuallyAdded) {
            this.track = track;
            this.manuallyAdded = manuallyAdded;
        }
        public PlayQueueItem setPosition(int position){
            this.position = position;
            return this;
        }

    }

    public PlayQueueManager(Context context, long userId) {
        mContext = context;
        mUserId = userId;
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
        return currentTrack == null ? -1 : currentTrack.id;
    }

    public Track getTrackAt(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos).track;
        } else {
            return null;
        }
    }

    public PlayQueueItem getItemAt(int pos) {
        if (pos >= 0 && pos < mPlayQueue.size()) {
            return mPlayQueue.get(pos).setPosition(pos);
        } else {
            return null;
        }
    }

    public long getTrackIdAt(int pos) {
        Track t = getTrackAt(pos);
        return t== null ? -1 : t.id;
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

    public void setTrackById(long toBePlayed) {
        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(toBePlayed);
        if (t != null) {
            setTrack(t, true);
        }
    }

    public void setTrack(Track toBePlayed, boolean saveQueue) {
        SoundCloudApplication.MODEL_MANAGER.cache(toBePlayed, ScResource.CacheUpdateMode.NONE);
        mPlayQueue.clear();
        mPlayQueue.add(new PlayQueueItem(toBePlayed, false));
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
        if (initialPlayQueue != null) {
            setPlayQueue(initialPlayQueue, initialPlayPos);
        } else {
            // no track yet, load async
            mPlayQueue.clear();
            mPlayPos = 0;
        }

        mPlayQueueUri = new PlayQueueUri(uri);
        if (uri != null) {
            loadCursor(uri, position);
        }
    }

    private AsyncTask loadCursor(final Uri uri, final int position) {
        return new ParallelAsyncTask<Uri,Void,List<PlayQueueItem>>() {
            @Override protected List<PlayQueueItem> doInBackground(Uri... params) {
                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(params[0], null, null, null, null);
                } catch (IllegalArgumentException e) {
                    // in case we load a depracated URI, just don't load the playlist
                    Log.e(PlayQueueManager.class.getSimpleName(),"Tried to load an invalid uri " + uri);
                }
                ArrayList<PlayQueueItem> newQueue = null;
                if (cursor != null) {
                    newQueue = new ArrayList<PlayQueueItem>();
                    if (cursor.moveToFirst()){
                        do {
                            // tracks only, no playlists allowed past here
                            if (cursor.getInt(cursor.getColumnIndex(DBHelper.SoundView._TYPE)) == Playable.DB_TYPE_TRACK) {
                                newQueue.add(new PlayQueueItem(SoundCloudApplication.MODEL_MANAGER.getCachedTrackFromCursor(cursor), false));
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();
                }
                return newQueue;


            }
            @Override protected void onPostExecute(List<PlayQueueItem> newQueue) {
                // make sure this cursor is valid and still wanted
                if (newQueue != null){
                    long playingId = getCurrentTrackId();
                    mPlayQueue = newQueue;
                    final Track t = getTrackAt(position);

                    // adjust if the track has moved positions
                    int adjustedPosition = -1;
                    if (t != null && t.id != playingId) {
                        if (Content.match(uri).isCollectionItem()){
                            adjustedPosition = getPlayQueuePositionFromUri(mContext.getContentResolver(), uri, playingId);
                        } else {
                            /* adjust for deletions or new items. find the original track
                             this is a really dumb sequential search. If there are duplicates in the list, it will probably
                             find the wrong one. This should be more intelligent after refactoring for sets */
                            adjustedPosition = 0;
                            while (adjustedPosition < length() && getTrackIdAt(adjustedPosition) != playingId) adjustedPosition++;
                        }
                    }
                    mPlayPos = adjustedPosition == -1 || adjustedPosition >= length() ? position : adjustedPosition;


                    // adjust to within bounds
                    mPlayPos = Math.max(0, Math.min(mPlayPos, mPlayQueue.size()-1));
                    broadcastPlayQueueChanged();
                }
            }
        }.executeOnThreadPool(uri);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.PLAYQUEUE_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlayQueue(final List<? extends PlayableHolder> playQueue, int playPos) {
        mPlayQueue.clear();
        if (playQueue != null) {
            for (PlayableHolder playable : playQueue) {
                if (playable.getPlayable() instanceof Track){
                    mPlayQueue.add(new PlayQueueItem((Track) playable.getPlayable(), false));
                }
            }
        }
        mPlayQueueUri = new PlayQueueUri();
        mPlayPos = Math.max(0, Math.min(mPlayQueue.size(), playPos));
        // TODO, only do this on exit???
        saveQueue(0, true);
        broadcastPlayQueueChanged();
    }

    private void persistPlayQueue() {
        if (mUserId < 0) return;

        new ParallelAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final List<Track> tracks = new ArrayList<Track>();
                for (PlayQueueItem item : mPlayQueue){
                    tracks.add(item.track);
                }
                SoundCloudDB.insertCollection(mContext.getContentResolver(), tracks, Content.PLAY_QUEUE.uri, mUserId);
                return null;
            }
        }.executeOnThreadPool((Void[]) null);
    }

    public Uri getUri() {
        return mPlayQueueUri.uri;
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
     * @return last stored seek pos of the current track in queue
     */
    public long reloadQueue() {
        // TODO : StrictMode policy violation; ~duration=139 ms: android.os.StrictMode$StrictModeDiskReadViolation: policy=23 violation=2
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String lastUri = preferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);

        if (!TextUtils.isEmpty(lastUri)) {
            PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            long seekPos      = playQueueUri.getSeekPos();
            final int trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(trackId);
                loadUri(playQueueUri.uri, playQueueUri.getPos(), t);
                // adjust play position if it has changed
                if (getCurrentTrack() != null && getCurrentTrack().id != trackId && playQueueUri.isCollectionUri()) {
                    final int newPos = getPlayQueuePositionFromUri(mContext.getContentResolver(), playQueueUri.uri, trackId);
                    if (newPos == -1) seekPos = 0;
                    setPosition(Math.max(newPos, 0));
                }
            }
            return seekPos;
        } else {
            return 0; // seekpos
        }
    }

    public static int getPlayQueuePositionFromUri(ContentResolver resolver, Uri collectionUri, long itemId) {
        Cursor cursor = resolver.query(collectionUri,
                new String[]{ DBHelper.CollectionItems.POSITION },
                DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[] {String.valueOf(itemId)},
                null);

        int position = -1;
        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            position = cursor.getInt(0);
        }
        if (cursor != null) cursor.close();
        return position;
    }


    public static void clearState(Context context) {
        context.getContentResolver().delete(Content.PLAY_QUEUE.uri, null, null);
        clearLastPlayed(context);
    }

    public static void clearLastPlayed(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(Consts.PrefKeys.SC_PLAYQUEUE_URI)
                .commit();
    }

    public void onDestroy() {
        // nop
    }
}

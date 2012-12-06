package com.soundcloud.android.service.playback;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
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
import java.util.Collections;
import java.util.List;

public class PlayQueueManager {
    private Track[] mPlayQueue = new Track[0];
    private Cursor mTrackCursor;
    private PlayQueueUri mPlayQueueUri = new PlayQueueUri();

    private int mPlayPos;
    private final Context mContext;

    private long mUserId;

    public PlayQueueManager(Context context, long userId) {
        mContext = context;
        mUserId = userId;
    }

    public int length() {
        return mPlayQueue.length;
    }

    public boolean isEmpty() {
        return mPlayQueue.length == 0;
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mPlayQueue.length) {
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
        if (pos >= 0 && pos < mPlayQueue.length) {
            if (mPlayQueue[pos] == null) {
                if (mTrackCursor != null && !mTrackCursor.isClosed() && mTrackCursor.moveToPosition(pos)){
                    mPlayQueue[pos] = SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(mTrackCursor);
                }
            }
            return mPlayQueue[pos];

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
        if (mPlayPos < mPlayQueue.length - 1) {
            int newPos = mPlayPos + 1;
            Track newTrack = getTrackAt(newPos);
            while (newPos < mPlayQueue.length - 1 && (newTrack == null || !newTrack.isStreamable())) {
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

    public void setTrack(long toBePlayed) {
        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(toBePlayed);
        if (t != null) {
            setTrack(t, true);
        }
    }

    public void setTrack(Track toBePlayed, boolean saveQueue) {
        SoundCloudApplication.MODEL_MANAGER.cache(toBePlayed, ScResource.CacheUpdateMode.NONE);
        mPlayQueue = new Track[] { toBePlayed };
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

    /**
     * @param uri the playqueue uri to load
     * @param position position within playqueue
     * @param initialTrack first track of this playqueue, load rest asynchronously.
     */
    public void loadUri(Uri uri, int position, @Nullable Track initialTrack) {
        if (initialTrack != null) {
            setTrack(initialTrack, false);
        } else {
            // no track yet, load async
            mPlayQueue = new Track[0];
            mPlayPos = 0;
        }
        mPlayQueueUri = new PlayQueueUri(uri);

        if (uri != null) {
            loadCursor(uri, position);
        }
    }

    private AsyncTask loadCursor(final Uri uri, final int position) {
        return new AsyncTask<Uri,Void,Cursor>() {
            @Override protected Cursor doInBackground(Uri... params) {
                Cursor cursor = null;
                try {
                    cursor = mContext.getContentResolver().query(params[0], null, null, null, null);
                } catch (IllegalArgumentException e) {
                    // in case we load a depracated URI, just don't load the playlist
                    Log.e(PlayQueueManager.class.getSimpleName(),"Tried to load an invalid uri " + uri);
                }
                if (cursor != null) {
                    if (position >= 0 && position < cursor.getCount()) {
                        cursor.moveToPosition(position);
                    }
                }
                return cursor;


            }
            @Override protected void onPostExecute(Cursor cursor) {
                // make sure this cursor is valid and still wanted
                if (cursor != null) {
                    long playingId = getCurrentTrackId();
                    final int size = cursor.getCount();
                    mPlayQueue = new Track[size];
                    if (mTrackCursor != null && !mTrackCursor.isClosed()) mTrackCursor.close();
                    mTrackCursor = cursor;
                    final Track t = getTrackAt(position);
                    // adjust if the track has moved positions
                    if (t != null && t.id != playingId && mPlayQueueUri.isCollectionUri()) {
                        mPlayPos = getPlayQueuePositionFromUri(mContext.getContentResolver(), uri, playingId);
                    } else {
                        mPlayPos = position;
                    }

                    // adjust to within bounds
                    mPlayPos = Math.max(0, Math.min(mPlayPos, mPlayQueue.length-1));
                    broadcastPlayQueueChanged();
                }
            }
        }.execute(uri);
    }

    private void broadcastPlayQueueChanged() {
        Intent intent = new Intent(CloudPlaybackService.PLAYQUEUE_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlayQueue(final List<? extends Playable> playQueue, int playPos) {
        mPlayQueue = new Track[playQueue == null ? 0 : playQueue.size()];
        if (playQueue != null) {
            for (int i=0; i<playQueue.size(); i++) {
                mPlayQueue[i] = playQueue.get(i).getTrack();
            }
        }
        mPlayQueueUri = new PlayQueueUri();
        mPlayPos = Math.max(0, Math.min(mPlayQueue.length, playPos));
        // TODO, only do this on exit???
        saveQueue(0, true);
        broadcastPlayQueueChanged();
    }

    private void persistPlayQueue() {
        if (mUserId < 0) return;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Track> tracks = new ArrayList<Track>();
                Collections.addAll(tracks, mPlayQueue);
                mContext.getContentResolver().delete(Content.PLAY_QUEUE.uri, null, null);
                SoundCloudApplication.MODEL_MANAGER.writeCollection(tracks, Content.PLAY_QUEUE.uri,
                        mUserId, ScResource.CacheUpdateMode.NONE);
                return null;
            }
        }.execute((Void[]) null);
    }

    public Uri getUri() {
        return mPlayQueueUri.uri;
    }

    public void clear() {
        mPlayQueue = new Track[0];
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
        if (mTrackCursor != null && !mTrackCursor.isClosed()) mTrackCursor.close();
    }
}

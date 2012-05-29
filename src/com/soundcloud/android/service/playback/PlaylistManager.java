package com.soundcloud.android.service.playback;


import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.utils.SharedPreferencesUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.List;

public class PlaylistManager {

    // these will be stored as uri parameters
    private static final String PARAM_PLAYLIST_POS = "playlistPos";
    private static final String PARAM_SEEK_POS = "seekPos";
    private static final String PARAM_TRACK_ID = "trackId";

    private Track[] mPlaylist = new Track[0];
    private Cursor mTrackCursor;
    private Uri mPlaylistUri;

    private int mPlayPos;
    private Context mContext;
    private TrackCache mCache;
    private static final int DEFAULT_PLAYLIST = 0;
    static final Uri DEFAULT_PLAYLIST_URI = Content.PLAYLIST.forId(DEFAULT_PLAYLIST);
    private long mUserId;

    public PlaylistManager(Context context, TrackCache cache, long userId) {
        mCache = cache;
        mContext = context;
        mUserId = userId;
    }

    public int length() {
        return mPlaylist.length;
    }

    public boolean isEmpty() {
        return mPlaylist.length == 0;
    }

    public int getPosition() {
        return mPlayPos;
    }

    public boolean setPosition(int playPos) {
        if (playPos < mPlaylist.length) {
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
        if (pos >= 0 && pos < mPlaylist.length) {
            if (mPlaylist[pos] == null) {
                if (mTrackCursor != null && !mTrackCursor.isClosed() && mTrackCursor.moveToPosition(pos)){
                    mPlaylist[pos] = new Track(mTrackCursor);
                }
            }
            mCache.put(mPlaylist[pos], false);
            return mPlaylist[pos];

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
        if (mPlayPos < mPlaylist.length - 1) {
            int newPos = mPlayPos + 1;
            Track newTrack = getTrackAt(newPos);
            while (newPos < mPlaylist.length - 1 && (newTrack == null || !newTrack.isStreamable())) {
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

    public void setTrack(Track toBePlayed) {
        mCache.put(toBePlayed, false);
        mPlaylist = new Track[]{toBePlayed};
        mPlaylistUri = null;
        mPlayPos = 0;
        broadcastPlaylistChanged();
    }

    public void setUri(Uri uri, int position, long trackId) {
        Track t = null;
        if (trackId != -1) {
            t = mCache.get(trackId);
            // ensure that we have an initial track to load, should be cached to avoid this db hit on the UI
            if (t == null) {
                t = SoundCloudDB.getTrackById(mContext.getContentResolver(), trackId);
            }
        }
        setUri(uri, position, t);

    }

    public void setUri(Uri uri, int position, Track track) {
        if (track != null) {
            mCache.put(track, false);
            mPlaylist = new Track[]{ track };
            mPlayPos = 0;
            broadcastPlaylistChanged();
        } else {
            // no track yet, load async
            mPlaylist = new Track[0];
            mPlayPos = 0;
        }

        if (uri != null) {
            loadCursor(uri, position);
        }
    }

    private AsyncTask loadCursor(final Uri uri, final int position) {
        return new AsyncTask<Uri,Void,Cursor>() {
            @Override protected Cursor doInBackground(Uri... params) {
                Cursor cursor = mContext.getContentResolver().query(params[0], null, null, null, null);
                if (cursor != null) {
                    if (position >= 0 && position < cursor.getCount()) {
                        cursor.moveToPosition(position);
                    }
                }
                return cursor;
            }
            @Override protected void onPostExecute(Cursor cursor) {
                if (cursor != null) {
                    long playingId = getCurrentTrackId();
                    mPlaylist = new Track[cursor.getCount()];
                    if (mTrackCursor != null && !mTrackCursor.isClosed()) mTrackCursor.close();
                    mTrackCursor = cursor;
                    mPlaylistUri = uri;

                    final Track t = getTrackAt(position);
                    // adjust if the track has moved positions
                    if (t != null && t.id != playingId && Content.match(uri).isCollectionItem()) {
                        mPlayPos = getPlaylistPositionFromUri(mContext.getContentResolver(), uri, playingId);
                    } else {
                        mPlayPos = position;
                    }

                    // adjust to within bounds
                    mPlayPos = Math.max(0, Math.min(mPlayPos, mPlaylist.length-1));
                    broadcastPlaylistChanged();
                }
            }
        }.execute(uri);
    }

    private void broadcastPlaylistChanged() {
        Intent intent = new Intent(CloudPlaybackService.PLAYLIST_CHANGED)
            .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, mPlayPos);
        mContext.sendBroadcast(intent);
    }

    public void setPlaylist(final List<? extends Playable> playlist, int playPos) {
        // cache a new tracklist
        mPlaylist = new Track[playlist == null ? 0 : playlist.size()];
        if (playlist != null) {
            for (int i=0; i<playlist.size(); i++) {
                mPlaylist[i] = playlist.get(i).getTrack();
            }
        }

        mPlaylistUri = DEFAULT_PLAYLIST_URI;
        mPlayPos = Math.max(0, Math.min(mPlaylist.length, playPos));

        // TODO, only do this on exit???
        //noinspection unchecked
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (mUserId > -1) {
                    mContext.getContentResolver().delete(DEFAULT_PLAYLIST_URI, null, null);
                    SoundCloudDB.bulkInsertParcelables(mContext.getContentResolver(), playlist, DEFAULT_PLAYLIST_URI, mUserId);
                }
                return null;
            }
        }.execute();


        broadcastPlaylistChanged();
    }

    public Uri getUri() {
        return mPlaylistUri;
    }

    public void clear() {
        mPlaylist = new Track[0];
    }

    public void saveQueue(long seekPos) {
        if (SoundCloudApplication.getUserIdFromContext(mContext) >= 0) {
            SharedPreferencesUtils.apply(PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString(Consts.PrefKeys.SC_PLAYLIST_URI, getPlaylistState(seekPos).toString()));
        }
    }

    /* package */ Uri getPlaylistState(long seekPos) {
        Uri playlistState = mPlaylistUri == null ?
                DEFAULT_PLAYLIST_URI : mPlaylistUri;
        Uri.Builder builder = playlistState.buildUpon();
        final Track currentTrack = getCurrentTrack();
        if (currentTrack != null) {
            builder.appendQueryParameter(PARAM_TRACK_ID, String.valueOf(currentTrack.id));
        }
        builder.appendQueryParameter(PARAM_PLAYLIST_POS, String.valueOf(mPlayPos));
        builder.appendQueryParameter(PARAM_SEEK_POS, String.valueOf(seekPos));
        return builder.build();
    }

    public long reloadQueue() {
        // TODO : StrictMode policy violation; ~duration=139 ms: android.os.StrictMode$StrictModeDiskReadViolation: policy=23 violation=2
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final String lastUri = preferences.getString(Consts.PrefKeys.SC_PLAYLIST_URI, null);

        if (!TextUtils.isEmpty(lastUri)) {
            final Uri uri = Uri.parse(lastUri);
            final long trackId = extractValue(uri, PARAM_TRACK_ID, 0);
            long seekPos = extractValue(uri, PARAM_SEEK_POS, 0);
            if (trackId > 0) {
                Track t = SoundCloudDB.getTrackById(mContext.getContentResolver(), trackId);
                setUri(uri, extractValue(uri, PARAM_PLAYLIST_POS, 0), t);
                // adjust play position if it has changed
                if (getCurrentTrack() != null && getCurrentTrack().id != trackId && Content.match(uri).isCollectionItem()) {
                    final int newPos = getPlaylistPositionFromUri( mContext.getContentResolver(), uri, trackId);
                    if (newPos == -1) seekPos = 0;
                    setPosition(Math.max(newPos, 0));
                }
            }
            return seekPos;
        } else {
            return 0; // seekpos
        }
    }

    public static int getPlaylistPositionFromUri(ContentResolver resolver, Uri collectionUri, long itemId) {
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

    private static int extractValue(Uri uri, String parameter, final int defaultValue) {
        final String pos = uri.getQueryParameter(parameter);
        if (!TextUtils.isEmpty(pos)) {
            try {
                return Integer.parseInt(pos);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }

    public static void clearState(Context context) {
        context.getContentResolver().delete(Content.PLAYLISTS.uri, null, null);
        context.getContentResolver().delete(DEFAULT_PLAYLIST_URI, null, null);
        clearLastPlayed(context);
    }

    public static void clearLastPlayed(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(Consts.PrefKeys.SC_PLAYLIST_URI)
                .commit();
    }

    public void onDestroy() {
        if (mTrackCursor != null && !mTrackCursor.isClosed()) mTrackCursor.close();
    }
}

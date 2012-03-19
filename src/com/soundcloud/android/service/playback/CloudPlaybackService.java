package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.State.*;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.streaming.StreamProxy;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.tracking.Tracker;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.PlaybackRemoteViews;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;

public class CloudPlaybackService extends Service implements AudioManagerHelper.MusicFocusable, Tracker {
    public static final String TAG = "CloudPlaybackService";
    public static List<Playable> playlistXfer;

    public static final String PLAYSTATE_CHANGED  = "com.soundcloud.android.playstatechanged";
    public static final String META_CHANGED       = "com.soundcloud.android.metachanged";
    public static final String PLAYLIST_CHANGED   = "com.soundcloud.android.playlistchanged";
    public static final String PLAYBACK_COMPLETE  = "com.soundcloud.android.playbackcomplete";
    public static final String PLAYBACK_ERROR     = "com.soundcloud.android.trackerror";
    public static final String STREAM_DIED        = "com.soundcloud.android.streamdied";
    public static final String COMMENTS_LOADED    = "com.soundcloud.android.commentsloaded";
    public static final String FAVORITE_SET       = "com.soundcloud.android.favoriteset";
    public static final String SEEK_COMPLETE      = "com.soundcloud.android.seekcomplete";
    public static final String BUFFERING          = "com.soundcloud.android.buffering";
    public static final String BUFFERING_COMPLETE = "com.soundcloud.android.bufferingcomplete";
    public static final String SERVICECMD         = "com.soundcloud.android.musicservicecommand";

    public static final String TOGGLEPAUSE_ACTION = "com.soundcloud.android.musicservicecommand.togglepause";
    public static final String PAUSE_ACTION       = "com.soundcloud.android.musicservicecommand.pause";
    public static final String PREVIOUS_ACTION    = "com.soundcloud.android.musicservicecommand.previous";
    public static final String NEXT_ACTION        = "com.soundcloud.android.musicservicecommand.next";
    public static final String PLAY               = "com.soundcloud.android.musicservicecommand.play";
    public static final String RESET_ALL          = "com.soundcloud.android.musicservicecommand.resetall";
    public static final String STOP_ACTION        = "com.soundcloud.android.musicservicecommand.stop";

    public static final String EXTRA_FROM_NOTIFICATION  = "com.soundcloud.android.musicserviceextra.fromNotification";
    public static final String EXTRA_UNMUTE             = "com.soundcloud.android.musicserviceextra.unmute";

    public static final String ADD_FAVORITE       = "com.soundcloud.android.favorite.add";
    public static final String REMOVE_FAVORITE    = "com.soundcloud.android.favorite.remove";

    public static final String CMDNAME        = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP        = "stop";
    public static final String CMDPAUSE       = "pause";
    public static final String CMDPREVIOUS    = "previous";
    public static final String CMDNEXT        = "next";

    private static final int TRACK_ENDED      = 1;
    private static final int SERVER_DIED      = 2;
    private static final int FADE_IN          = 3;
    private static final int FADE_OUT         = 4;
    private static final int DUCK             = 5;
    private static final int CLEAR_LAST_SEEK  = 6;
    private static final int STREAM_EXCEPTION = 7;
    private static final int CHECK_TRACK_EVENT = 8;
    private static final int NOTIFY_META_CHANGED = 9;

    public static final int PLAYBACKSERVICE_STATUS_ID = 1;
    private static final int MINIMUM_SEEKABLE_SDK = Build.VERSION_CODES.ECLAIR_MR1; // 7, 2.1

    private static final float FADE_CHANGE = 0.02f; // change to fade faster/slower

    private MediaPlayer mMediaPlayer;
    private int mLoadPercent = 0;       // track buffer indicator
    private boolean mAutoPause = true;  // used when svc is first created and playlist is resumed on start
    private boolean mAutoAdvance = true;// automatically skip to next track
    protected NetworkConnectivityListener connectivityListener;
    /* package */ PlaylistManager mPlaylistManager;
    private Track mCurrentTrack;
    private AudioManager mAudioManager;

    private long mResumeTime = -1;      // time of played track
    private long mResumeTrackId = -1;   // id of last played track
    private long mSeekPos = -1;         // desired seek position
    private int mConnectRetries = 0;
    private long mLastRefresh;          // time last refresh hit was sent

    private int mServiceStartId = -1;
    private boolean mServiceInUse = false;
    private PlayerAppWidgetProvider mAppWidgetProvider = PlayerAppWidgetProvider.getInstance();

    private static final int IDLE_DELAY = 60*1000;  // interval after which we stop the service when idle
    private static final long CHECK_TRACK_EVENT_DELAY = Media.REFRESH_MIN; // check for track timestamp events at this frequency

    private boolean resumeSeeking;

    private StreamProxy mProxy;
    private TrackCache mCache;

    // audio focus related
    private AudioManagerHelper mFocus;
    private boolean mTransientFocusLoss;

    private State state = STOPPED;

    private final IBinder mBinder = new ServiceStub(this);
    public static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(false);
    private Notification status;

    public interface PlayExtras{
        String trackId = "track_id";
        String playPosition = "play_position";
        String playFromXferCache = "play_from_xfer_cache";
    }

    public interface BroadcastExtras{
        String id = "id";
        String title = "title";
        String user_id = "user_id";
        String username = "username";
        String isPlaying = "isPlaying";
        String isSupposedToBePlaying = "isSupposedToBePlaying";
        String isBuffering = "isBuffering";
        String position = "position";
        String queuePosition = "queuePosition";
        String isFavorite = "isFavorite";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCache = SoundCloudApplication.TRACK_CACHE;
        mPlaylistManager = new PlaylistManager(this, mCache, getApp().getCurrentUserId());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(ADD_FAVORITE);
        commandFilter.addAction(REMOVE_FAVORITE);
        commandFilter.addAction(RESET_ALL);
        commandFilter.addAction(STOP_ACTION);
        commandFilter.addAction(PLAYLIST_CHANGED);

        registerReceiver(mIntentReceiver, commandFilter);
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        mFocus = new AudioManagerHelper(this, this);
        if (!mFocus.isSupported()) {
            // setup call listening if not handled by audiofocus
            TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        scheduleServiceShutdownCheck();

        mResumeTime = mPlaylistManager.reloadQueue();
        mCurrentTrack = mPlaylistManager.getCurrentTrack();
        if (mCurrentTrack != null && mResumeTime > 0) {
            mResumeTrackId = mCurrentTrack.id;
        }

        try {
            mProxy = new StreamProxy(getApp()).init().start();
        } catch (IOException e) {
            Log.i(TAG, "Unable to start service " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);
        mPlaylistManager.onDestroy();

        mFocus.abandonMusicFocus(false);
        unregisterReceiver(mIntentReceiver);
        if (mProxy != null && mProxy.isRunning()) mProxy.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());

        if (state.isSupposedToBePlaying() || state == PAUSED_FOCUS_LOST) {
            // something is currently playing, or will be playing once
            // an in-progress call ends, so don't stop the service now.
            return true;

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        } else if (!mPlaylistManager.isEmpty() || mPlayerHandler.hasMessages(TRACK_ENDED)) {
            mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
            return true;
        } else {
            // No active playlist, OK to stop the service right now
            stopSelf(mServiceStartId);
            return true;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            mIntentReceiver.onReceive(this, intent);
        }
        scheduleServiceShutdownCheck();
        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        return START_STICKY;
    }

    @Override
    public void focusGained() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "focusGained");
        if ((state.isSupposedToBePlaying() || state == State.PAUSED_FOCUS_LOST) && mTransientFocusLoss) {
            mPlayerHandler.sendEmptyMessage(FADE_IN);
            mTransientFocusLoss = false;
        } else {
            setVolume(1.0f);
        }
    }

    @Override
    public void focusLost(boolean isTransient, boolean canDuck) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "focusLost("+isTransient+", canDuck="+canDuck+")");
        }
        if (state.isSupposedToBePlaying()) {
            if (isTransient){
                mPlayerHandler.sendEmptyMessage(canDuck ? DUCK : FADE_OUT);
            } else {
                pause();
            }
        }
        mTransientFocusLoss = isTransient;
    }

    private void scheduleServiceShutdownCheck() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "scheduleServiceShutdownCheck()");
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, IDLE_DELAY);
    }

    private void notifyChange(String what) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "notifyChange(" + what + ")");
        }
        Intent i = new Intent(what)
            .putExtra(BroadcastExtras.id, getTrackId())
            .putExtra(BroadcastExtras.title, getTrackName())
            .putExtra(BroadcastExtras.user_id, getUserId())
            .putExtra(BroadcastExtras.username, getUserName())
            .putExtra(BroadcastExtras.isPlaying, isPlaying())
            .putExtra(BroadcastExtras.isSupposedToBePlaying, state.isSupposedToBePlaying())
            .putExtra(BroadcastExtras.isBuffering, isBuffering())
            .putExtra(BroadcastExtras.position, getPosition())
            .putExtra(BroadcastExtras.queuePosition, mPlaylistManager.getPosition())
            .putExtra(BroadcastExtras.isFavorite, getIsFavorite());

        sendBroadcast(i);

        if (SoundCloudApplication.useRichNotifications()) {
            if (what.equals(PLAYSTATE_CHANGED)) {
                mFocus.setPlaybackState(isPlaying());
                if (status != null) ((PlaybackRemoteViews) status.contentView).setPlaybackStatus(isPlaying());
            } else if (what.equals(META_CHANGED)) {
                applyCurrentMetadata(mCurrentTrack);
            }
        }

        if (what.equals(META_CHANGED) || what.equals(PLAYBACK_ERROR) || what.equals(PLAYBACK_COMPLETE)) {
            mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());
        }


        // Share this notification directly with our widgets
        mAppWidgetProvider.notifyChange(this, i);
    }

    private void applyCurrentMetadata(final Track track){
        if (SoundCloudApplication.useRichNotifications()) {
            final String artworkUri = track.getListArtworkUrl(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                final Bitmap bmp = ImageLoader.get(this).getBitmap(artworkUri, null, new ImageLoader.Options(false));
                if (bmp != null) {
                    // use a copy of the bitmap because it is going to get recycled afterwards
                    try {
                        mFocus.applyRemoteMetadata(this, track, bmp.copy(Bitmap.Config.ARGB_8888, true));
                    } catch (OutOfMemoryError e) {
                        mFocus.applyRemoteMetadata(this, track, null);
                        System.gc();
                        // retry?
                    }
                } else {
                    mFocus.applyRemoteMetadata(this, track);
                    ImageLoader.get(this).getBitmap(artworkUri, new ImageLoader.BitmapCallback() {
                        public void onImageLoaded(Bitmap loadedBmp, String uri) {
                            if (track.equals(mCurrentTrack)) applyCurrentMetadata(track);
                        }
                        public void onImageError(String uri, Throwable error) {}
                    });
                }
            }
        }
    }



    /* package */ void openCurrent() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "openCurrent(state="+state+")");
        }

        final Track track = mPlaylistManager.getCurrentTrack();
        if (track != null) {
            if (mAutoPause) {
                mAutoPause = false;
            }
            mLoadPercent = 0;
            if (track.equals(mCurrentTrack) && track.isStreamable()) {
                notifyChange(META_CHANGED);
                startTrack(track);
            } else { // new track
                track(Media.fromTrack(mCurrentTrack), "stop");
                mCurrentTrack = track;
                notifyChange(META_CHANGED);
                mConnectRetries = 0; // new track, reset connection attempts

                if (track.isStreamable()) {
                    onStreamableTrack(track);
                } else if (!CloudUtils.isTaskFinished(track.load_info_task)) {
                    track.load_info_task.addListener(mInfoListener);
                } else {
                    onUnstreamableTrack(track.id);
                }
            }
        } else {
            Log.d(TAG, "playlist is empty");
            state = EMPTY_PLAYLIST;
        }
    }

    private FetchTrackTask.FetchTrackListener mInfoListener = new FetchTrackTask.FetchTrackListener() {
        @Override
        public void onSuccess(Track track, String action) {
            if (track.isStreamable()) {
                onStreamableTrack(track);
            } else {
                onUnstreamableTrack(track.id);
            }
        }

        @Override
        public void onError(long trackId) {
            onUnstreamableTrack(trackId);
        }
    };

    private void onUnstreamableTrack(long trackId){
        if (mCurrentTrack.id != trackId) return;

        mPlayerHandler.sendEmptyMessage(STREAM_EXCEPTION);
        gotoIdleState(STOPPED);
    }

    private void onStreamableTrack(Track track){
        if (mCurrentTrack.id != track.id) return;

        new Thread() {
            @Override
            public void run() {
                SoundCloudDB.markTrackAsPlayed(getContentResolver(), mCurrentTrack);
            }
        }.start();
        startTrack(track);


    }

    private void startTrack(Track track) {
        track(Page.Sounds_main, track);
        setPlayingNotification(track);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "startTrack("+track.title+")");
        }
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        switch (state) {
            case PREPARING:
                Log.w(TAG, "stuck in preparing state!");
                final MediaPlayer old = mMediaPlayer;
                new Thread() {
                    @Override
                    public void run() {
                        old.reset();
                        old.release();
                    }
                }.start();
                mMediaPlayer = new MediaPlayer();
                break;
            case PLAYING:
                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                try {
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.stop");
                    mMediaPlayer.stop();
                    state = STOPPED;
                } catch (IllegalStateException e) {
                    Log.w(TAG, e);
                }
                break;
        }
        state = PREPARING;
        try {
            if (mProxy == null) {
                mProxy = new StreamProxy(getApp()).init().start();
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.reset");
            mMediaPlayer.reset();
            mMediaPlayer.setWakeMode(CloudPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(preparedlistener);
            mMediaPlayer.setOnSeekCompleteListener(seekListener);
            mMediaPlayer.setOnCompletionListener(completionListener);
            mMediaPlayer.setOnErrorListener(errorListener);
            mMediaPlayer.setOnBufferingUpdateListener(bufferingListener);
            mMediaPlayer.setOnInfoListener(infolistener);
            Track next = mPlaylistManager.getNext();
            mMediaPlayer.setDataSource(mProxy.createUri(mCurrentTrack.stream_url, next == null ? null : next.stream_url).toString());
            mMediaPlayer.prepareAsync();
            notifyChange(BUFFERING);

        } catch (IllegalStateException e) {
            Log.e(TAG, "error", e);
            gotoIdleState(ERROR);
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            errorListener.onError(mMediaPlayer, 0, 0);
        }
    }


    /* package */ void play() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "play(state=" + state + ")");
        track(Media.fromTrack(mCurrentTrack), "play");
        mLastRefresh = System.currentTimeMillis();

        if (mCurrentTrack != null && mFocus.requestMusicFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (mMediaPlayer != null && state.isStartable()) {
                // resume
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "mp.start");

                mMediaPlayer.start();
                state = PLAYING;
                setPlayingNotification(mCurrentTrack);
                mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                notifyChange(PLAYSTATE_CHANGED);
            } else if (state != PLAYING) {
                // must have been a playback error
                openCurrent();
            }
        }
    }

    // Pauses playback (call play() to resume)
    /* package */ void pause() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "pause(state="+state+")");
        }
        track(Media.fromTrack(mCurrentTrack), "pause");

        if (mMediaPlayer != null && state != PAUSED) {
            if (state.isPausable() && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                gotoIdleState(PAUSED);
            } else {
                // get into a determined state
                stop();
            }
        }
        notifyChange(PLAYSTATE_CHANGED);
    }

    /* package */ void stop() {
        // this is not usually called due to errors, not user interaction
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stop(state="+state+")");
        }
        if (mMediaPlayer != null) {
            if (state.isStoppable()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        gotoIdleState(STOPPED);
    }


    private void gotoIdleState(State newState) {
        state = newState;
        mPlayerHandler.removeMessages(FADE_OUT);
        mPlayerHandler.removeMessages(FADE_IN);
        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
        scheduleServiceShutdownCheck();
        if (SoundCloudApplication.useRichNotifications()){
            stopForeground(false);
            if (status != null){
                ((PlaybackRemoteViews) status.contentView).setPlaybackStatus(isPlaying());
                NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mManager.notify(PLAYBACKSERVICE_STATUS_ID, status);
            }
        } else {
            stopForeground(true);
        }
    }

    /* package */ State getState() {
        return state;
    }

    /* package */ void prev() {
        if (mPlaylistManager.prev()) openCurrent();
    }

    /* package */ void next() {
        if (mPlaylistManager.next()) openCurrent();
    }

    private void setPlayingNotification(final Track track) {

        if (track == null) return;

        status = new Notification();
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.ic_status;

        Intent intent = new Intent(Actions.PLAYER);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

        if (!SoundCloudApplication.useRichNotifications()) {
            status.setLatestEventInfo(this, track.getUserName(), track.title, pi);
        } else {
            PlaybackRemoteViews view = new PlaybackRemoteViews(getPackageName(), R.layout.playback_status_v11,
                    android.R.drawable.ic_media_play,android.R.drawable.ic_media_pause);
            view.setCurrentTrack(track.title, track.user.username);
            view.linkButtons(this,track.id,track.user_id,track.user_favorite, EXTRA_FROM_NOTIFICATION);
            view.setPlaybackStatus(state.isSupposedToBePlaying());

            final String artworkUri = track.getListArtworkUrl(this);
            if (ImageUtils.checkIconShouldLoad(artworkUri)) {
                final Bitmap bmp = ImageLoader.get(this).getBitmap(artworkUri, null, ICON_OPTIONS);
                if (bmp != null) {
                    view.setIcon(bmp);
                } else {
                    view.clearIcon();
                    ImageLoader.get(this).getBitmap(artworkUri, new ImageLoader.BitmapCallback() {

                        public void onImageLoaded(Bitmap mBitmap, String uri) {
                            if (status.contentView instanceof PlaybackRemoteViews && mCurrentTrack == track) {
                                ((PlaybackRemoteViews) status.contentView).setIcon(bmp);
                            }
                        }

                        public void onImageError(String uri, Throwable error) {
                        }
                    });
                }
            } else {
                view.clearIcon();
            }
            status.contentView = view;
            status.contentIntent = pi;
        }
        startForeground(PLAYBACKSERVICE_STATUS_ID, status);
    }

    /* package */ void setQueuePosition(int pos) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setQueuePosition("+pos+")");

        if (mPlaylistManager.getPosition() != pos &&
            mPlaylistManager.setPosition(pos)) {
            openCurrent();
        }
    }

    /* package */
    @SuppressWarnings("UnusedParameters")
    void setClearToPlay(boolean clearToPlay) {
    }

    /* package */ void setFavoriteStatus(long trackId, boolean favoriteStatus) {
        if (mCurrentTrack != null && mCurrentTrack.id == trackId) {
            if (favoriteStatus) {
                addFavorite();
            } else {
                removeFavorite();
            }
        }
    }

    /* package */ Track getCurrentTrack() {
        return mCurrentTrack == null ? mPlaylistManager.getCurrentTrack() : mCurrentTrack;
    }

    /* package */ long getCurrentTrackId() {
        return mCurrentTrack == null ? mPlaylistManager.getCurrentTrackId() : mCurrentTrack.id;
    }

    /* package */ int getDuration() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.duration;
    }

    /* package */ boolean isBuffering() {
        return state == PAUSED_FOR_BUFFERING || state == PREPARING || resumeSeeking;
    }

    /*
     * Returns the current playback position in milliseconds
     */
    /* package */ long getPosition() {
        if (mMediaPlayer != null && !state.isError()) {
            return mMediaPlayer.getCurrentPosition();
        } else if (mCurrentTrack != null && mResumeTrackId == mCurrentTrack.id) {
            return mResumeTime; // either -1 or a valid resume time
        } else {
            return 0;
        }
    }

    /* package */ int loadPercent() {
        return mMediaPlayer != null && !state.isError() ? mLoadPercent : 0;
    }

    /* package */ boolean isSeekable() {
        return (Build.VERSION.SDK_INT >= MINIMUM_SEEKABLE_SDK
                && mMediaPlayer != null
                && state.isSeekable()
                && mCurrentTrack != null);
    }

    /* package */ boolean isNotSeekablePastBuffer() {
        // Some phones on 2.2 ship with broken opencore
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO && StreamProxy.isOpenCore();
    }

    /* package */ long seek(long pos, boolean performSeek) {
        if (isSeekable()) {
            if (pos <= 0) {
                pos = 0;
            }
            final long currentPos = getPosition();
            // workaround for devices which can't do content-range requests
            if (isNotSeekablePastBuffer() && isPastBuffer(pos)) {
                Log.d(TAG, "MediaPlayer bug: cannot seek past buffer");
                return currentPos;
            } else {
                long duration = getDuration();

                final long newPos;
                // don't go before the playhead if they are trying to seek
                // beyond, just maintain their current position
                if (pos > currentPos && currentPos > duration) {
                    newPos = currentPos;
                } else if (pos > duration) {
                    newPos = duration;
                } else {
                    newPos = pos;
                }

                if (performSeek && newPos != currentPos) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "seeking to "+newPos);
                    }
                    mMediaPlayer.seekTo((int) newPos);
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "NOT seeking to "+newPos);
                    }
                }
                return newPos;
            }
        } else {
            return -1;
        }
    }

    /* package */ boolean isPlaying() {
        try {
            return mMediaPlayer != null && mMediaPlayer.isPlaying() && state.isSupposedToBePlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /* package */ void setAutoAdvance(boolean autoAdvance) {
        mAutoAdvance = autoAdvance;
    }

    private String getUserName() {
        return mCurrentTrack != null && mCurrentTrack.user != null ? mCurrentTrack.user.username : null;
    }

    private long getUserId() {
        return mCurrentTrack != null ? mCurrentTrack.user_id : -1;
    }

    private long getTrackId() {
        return mCurrentTrack == null ? -1 : mCurrentTrack.id;
    }

    private String getTrackName() {
        return mCurrentTrack == null ? null : mCurrentTrack.title;
    }

    private boolean getIsFavorite() {
        return mCurrentTrack != null && mCurrentTrack.user_favorite;
    }

    private boolean isPastBuffer(long pos) {
        return (pos / (double) mCurrentTrack.duration) * 100 > mLoadPercent;
    }

    private void setVolume(float vol) {
        if (mMediaPlayer != null && !state.isError()) {
            try {
                mMediaPlayer.setVolume(vol, vol);
            } catch (IllegalStateException ignored) {
                Log.w(TAG, ignored);
            }
        }
    }

    private boolean isConnected() {
        return connectivityListener != null && connectivityListener.isConnected();
    }

    private void addFavorite() {
        onFavoriteStatusSet(mCurrentTrack.id, true);
        FavoriteAddTask f = new FavoriteAddTask(getApp());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, false);
                // failed, so it shouldn't be a favorite
            }
        });
        f.execute(mCurrentTrack);
    }

    private void removeFavorite() {
        onFavoriteStatusSet(mCurrentTrack.id, false);
        FavoriteRemoveTask f = new FavoriteRemoveTask(getApp());
        f.setOnFavoriteListener(new FavoriteTask.FavoriteListener() {
            @Override
            public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
                onFavoriteStatusSet(trackId, isFavorite);
            }

            @Override
            public void onException(long trackId, Exception e) {
                onFavoriteStatusSet(trackId, true); // failed, so it should still be a favorite
            }

        });
        f.execute(mCurrentTrack);
    }

    private void onFavoriteStatusSet(long trackId, boolean isFavorite) {
        Track track = SoundCloudDB.getTrackById(getContentResolver(), trackId);
        if (track != null) {
            track.user_favorite = isFavorite;
            SoundCloudDB.upsertTrack(getContentResolver(), track);

        }

        if (mCache.containsKey(trackId)) {
            mCache.get(trackId).user_favorite = isFavorite;
        }

        if (mCurrentTrack.id == trackId && mCurrentTrack.user_favorite != isFavorite) {
            mCurrentTrack.user_favorite = isFavorite;
            notifyChange(FAVORITE_SET);
        } else {
            final Intent intent = new Intent(FAVORITE_SET);
            sendBroadcast(intent
                .putExtra("id", trackId)
                .putExtra("isFavorite", isFavorite));
            // Share this notification directly with our widgets
            mAppWidgetProvider.notifyChange(this, intent);
        }
    }

    private SoundCloudApplication getApp() {
        return (SoundCloudApplication) getApplication();
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (!state.isSupposedToBePlaying()
                    && state != PAUSED_FOCUS_LOST
                    && !mServiceInUse
                    && !mPlayerHandler.hasMessages(TRACK_ENDED)) {

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "DelayedStopHandler: stopping service");
                }

                mPlaylistManager.saveQueue(mCurrentTrack == null ? 0 : getPosition());
                stopSelf(mServiceStartId);
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "BroadcastReceiver#onReceive("+action+", "+cmd+")");
            }
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                next();
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (state.isSupposedToBePlaying()) {
                    pause();
                } else if (mCurrentTrack != null) {
                    play();
                } else {
                    openCurrent();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
            } else if (PlayerAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
                // Someone asked us to executeRefreshTask a set of specific widgets,
                // probably because they were just added.
                int[] appWidgetIds = intent
                        .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

                mAppWidgetProvider.performUpdate(CloudPlaybackService.this, appWidgetIds,
                        new Intent(PLAYSTATE_CHANGED));

            } else if (ADD_FAVORITE.equals(action)) {
                setFavoriteStatus(intent.getLongExtra("trackId", -1), true);
            } else if (REMOVE_FAVORITE.equals(action)) {
                setFavoriteStatus(intent.getLongExtra("trackId", -1), false);
            } else if (PLAY.equals(action)) {
                handlePlayAction(intent);
            } else if (RESET_ALL.equals(action)) {
                stop();
                mPlaylistManager.clear();
            } else if (CMDSTOP.equals(cmd) || STOP_ACTION.equals(action)) {
                if (state.isSupposedToBePlaying()) pause();
                stop();
                if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
                    stopForeground(true);
                }
            } else if (PLAYLIST_CHANGED.equals(action)) {
                if (state == EMPTY_PLAYLIST) {
                    openCurrent();
                }
            }
        }
    };

    private void handlePlayAction(Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "handlePlayAction("+intent+")");
        }

        if (intent.getBooleanExtra(EXTRA_UNMUTE, false)) {
            final int volume = (int) Math.round(
                    mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    * 0.75d);
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "setting volume to "+volume);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        }

        Track track = intent.getParcelableExtra("track");
        if (track != null) {
            mPlaylistManager.setTrack(track);
            openCurrent();
        } else if (intent.getData() != null) {
            mPlaylistManager.setUri(intent.getData(),
                    intent.getIntExtra(PlayExtras.playPosition, 0),
                    intent.getLongExtra(PlayExtras.trackId, -1)
            );
            openCurrent();
        } else if (intent.getBooleanExtra(PlayExtras.playFromXferCache, false)) {
            mPlaylistManager.setPlaylist(playlistXfer,intent.getIntExtra(PlayExtras.playPosition, 0));
            playlistXfer = null;
            openCurrent();
        } else {
            Log.w(TAG, "invalid play action");
        }
    }

    private final Handler mPlayerHandler = new Handler() {
        private static final float DUCK_VOLUME = 0.1f;
        private float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage("+msg.what+", state="+state+")");
            }
            switch (msg.what) {
                case NOTIFY_META_CHANGED:
                    notifyChange(META_CHANGED);
                    break;
                case FADE_IN:
                    removeMessages(FADE_OUT);

                    if (!state.isSupposedToBePlaying()) {
                        mCurrentVolume = 0f;
                        setVolume(0f);
                        play();
                        sendEmptyMessageDelayed(FADE_IN, 10);
                    } else {
                        mCurrentVolume += FADE_CHANGE;
                        if (mCurrentVolume < 1.0f) {
                            sendEmptyMessageDelayed(FADE_IN, 10);
                        } else {
                            mCurrentVolume = 1.0f;
                        }
                        setVolume(mCurrentVolume);
                    }
                    break;
                case FADE_OUT:
                    removeMessages(FADE_IN);
                    if (isPlaying())  {
                        mCurrentVolume -= FADE_CHANGE;
                        if (mCurrentVolume > 0f) {
                            sendEmptyMessageDelayed(FADE_OUT, 10);
                        } else {
                            mCurrentVolume = 0f;
                            mMediaPlayer.pause();
                            state = PAUSED_FOCUS_LOST;
                        }
                        setVolume(mCurrentVolume);
                    } else {
                        setVolume(0f);
                    }
                    break;
                case DUCK:
                    removeMessages(FADE_IN);
                    removeMessages(FADE_OUT);
                    setVolume(DUCK_VOLUME);
                    break;
                case SERVER_DIED:
                    if (state == PLAYING && mAutoAdvance) next();
                    break;
                case TRACK_ENDED:
                    if (mAutoAdvance) {
                        next();
                    } else {
                        notifyChange(PLAYBACK_COMPLETE);
                        gotoIdleState(COMPLETED);
                    }
                    break;
                case CLEAR_LAST_SEEK:
                    mSeekPos = -1;
                    break;
                case CHECK_TRACK_EVENT:
                    if (mCurrentTrack != null) {
                        if (state.isSupposedToBePlaying()) {
                            int refresh = Media.refresh(mCurrentTrack.duration);
                            if (refresh > 0) {
                                long now = System.currentTimeMillis();
                                if (now - mLastRefresh > refresh) {
                                    track(Media.fromTrack(mCurrentTrack), "refresh");
                                    mLastRefresh = now;
                                }
                            }
                        }
                        mPlayerHandler.sendEmptyMessageDelayed(CHECK_TRACK_EVENT, CHECK_TRACK_EVENT_DELAY);
                    } else {
                        mPlayerHandler.removeMessages(CHECK_TRACK_EVENT);
                    }
                    break;
            }
        }
    };

    final MediaPlayer.OnInfoListener infolistener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onInfo("+what+","+extra+", state="+state+")");
            }

            switch (what) {
                case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                    state = PAUSED_FOR_BUFFERING;
                    notifyChange(BUFFERING);
                    break;

                case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                    state = PLAYING;
                    notifyChange(BUFFERING_COMPLETE);
                    break;
                default:
            }
            return true;
        }
    };

    final MediaPlayer.OnBufferingUpdateListener bufferingListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (mMediaPlayer == mp) {
                if (Log.isLoggable(TAG, Log.DEBUG) && mLoadPercent != percent) {
                    Log.d(TAG, "onBufferingUpdate("+percent+")");
                }
                mLoadPercent = percent;
            }
        }
    };

    final MediaPlayer.OnSeekCompleteListener seekListener = new MediaPlayer.OnSeekCompleteListener() {
        public void onSeekComplete(MediaPlayer mp) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSeekComplete(state="+state+")");
            }

            if (mMediaPlayer == mp) {
                // keep the last seek time for 3000 ms because getCurrentPosition will be incorrect at first
                mPlayerHandler.removeMessages(CLEAR_LAST_SEEK);
                mPlayerHandler.sendEmptyMessageDelayed(CLEAR_LAST_SEEK, 3000);
                resumeSeeking = false;
                notifyChange(SEEK_COMPLETE);
            }
        }
    };

    final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCompletion(state="+state+")");
            }
            // mediaplayer seems to reset itself to 0 before this is called in certain builds, so if so,
            // pretend it's finished
            final long targetPosition = (mp.getCurrentPosition() <= 0 && state == PLAYING) ? getDuration()
                                        : (mSeekPos == -1) ? mp.getCurrentPosition()  : mSeekPos;
            // premature track end ?
            if (isSeekable() && getDuration() - targetPosition > 3000) {
                Log.w(TAG, "premature end of track (targetpos="+targetPosition+")");
                // track ended prematurely (probably end of buffer, unreported IO error),
                // so try to resume at last time
                mResumeTrackId = mCurrentTrack.id;
                mResumeTime = targetPosition;
                errorListener.onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, Errors.STAGEFRIGHT_ERROR_BUFFER_EMPTY);
            } else if (!state.isError()) {
                track(Media.fromTrack(mCurrentTrack), "stop");
                mPlayerHandler.sendEmptyMessage(TRACK_ENDED);
            } else {
                // onComplete must have been called in error state
                stop();
            }
        }
    };

    MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            if (mp == mMediaPlayer) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onPrepared(state="+state+")");
                }
                if (state == PREPARING) {
                    state = PREPARED;

                    // do we need to resume a track position ?
                    if (mCurrentTrack.id == mResumeTrackId && mResumeTime > 0) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "resuming to "+mResumeTime);
                        }
                        seek(mResumeTime, true);
                        mResumeTime = mResumeTrackId -1;
                        resumeSeeking = true;
                        play();

                    // normal play, unless first start (autopause=true)
                    } else {
                        setVolume(0);
                        //  FADE_IN will call play()
                        notifyChange(BUFFERING_COMPLETE);
                        if (!mAutoPause && mFocus.requestMusicFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            mPlayerHandler.sendEmptyMessage(FADE_IN);
                        }
                    }
                } else {
                    stop();
                }
            }
        }
    };

    MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "onError("+what+ ", "+extra+", state="+state+")");

            if (mp == mMediaPlayer && state != STOPPED) {
                // when the proxy times out it will just close the connection - different implementations
                // return different error codes. try to reconnect at least twice before giving up.
                if (mConnectRetries++ < 4) {
                    Log.d(TAG, "stream disconnected, retrying (try=" + mConnectRetries + ")");
                    state = ERROR_RETRYING;
                    openCurrent();
                    return true;
                } else {
                    Log.d(TAG, "stream disconnected, giving up");
                    mConnectRetries = 0;
                }
                mMediaPlayer.release();
                mMediaPlayer = null;

                gotoIdleState(ERROR);
                notifyChange(isConnected() ? PLAYBACK_ERROR : STREAM_DIED);
                notifyChange(PLAYBACK_COMPLETE);
            }
            return true;
        }
    };

    // this is only used in pre 2.2 phones where there is no audio focus support
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int callState, String incomingNumber) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCallStateChanged(state="+callState+", playerState="+state+")");
            }
            if (callState == TelephonyManager.CALL_STATE_OFFHOOK ||
               (callState == TelephonyManager.CALL_STATE_RINGING &&
                 mAudioManager.getStreamVolume(AudioManager.STREAM_RING) > 0)) {
                focusLost(true, false);
            } else if (callState == TelephonyManager.CALL_STATE_IDLE) {
                focusGained();
            }
        }
    };


    public void track(Event event, Object... args) {
        getApp().track(event, args);
    }

    @Override
    public void track(Class<?> klazz, Object... args) {
        getApp().track(klazz, args);
    }
}

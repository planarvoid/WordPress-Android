
package com.soundcloud.android.activity;

import android.os.*;
import android.view.WindowManager;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.service.RemoteControlReceiver;
import com.soundcloud.android.view.PlayerTrackView;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.android.view.WorkspaceView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ScPlayer extends ScActivity implements WorkspaceView.OnScreenChangeListener {
    private static final String TAG = "ScPlayer";
    private static final int REFRESH = 1;
    public static final int REFRESH_DELAY = 1000;

    private long mSeekPos = -1;
    private long mLastSeekEventTime = -1;

    private boolean mWaveformLoaded, mActivityPaused, mIsCommenting, mIsPlaying;
    private Drawable mPlayState, mPauseState;
    private ImageButton mPauseButton, mFavoriteButton, mCommentButton;
    private Track mPlayingTrack;
    private RelativeLayout mContainer;
    private WorkspaceView mTrackWorkspace;
    private Drawable mFavoriteDrawable, mFavoritedDrawable;

    private ComponentName mRemoteControlResponder;
    private AudioManager mAudioManager;

    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;

    static {
        initializeRemoteControlRegistrationMethods();
    }

    public interface PlayerError {
        int PLAYBACK_ERROR = 0;
        int STREAM_ERROR = 1;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sc_player);

        mContainer = (RelativeLayout) findViewById(R.id.container);

        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);
        findViewById(R.id.prev).setOnClickListener(mPrevListener);
        findViewById(R.id.next).setOnClickListener(mNextListener);

        mTrackWorkspace = (WorkspaceView) findViewById(R.id.track_view);
        mTrackWorkspace.setOnScreenChangeListener(this);

        mCommentButton = (ImageButton) findViewById(R.id.btn_comment);
        if (mCommentButton != null) {

            mCommentButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleCommentMode(getCurrentTrackView().getPlayPosition());
                }
            });

            mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
            mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleFavorite(mPlayingTrack);
                }
            });
        }

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);

        final Object[] saved = (Object[]) getLastNonConfigurationInstance();
        if (saved != null && saved[0] != null) mPlayingTrack = (Track) saved[0];

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void toggleCommentMode(int playPos) {
        mIsCommenting = !mIsCommenting;
        getTrackView(playPos).setCommentMode(mIsCommenting);
        if (mCommentButton != null) {
            if (mIsCommenting) {
                mCommentButton.setImageResource(R.drawable.ic_commenting_states);
            } else {
                mCommentButton.setImageResource(R.drawable.ic_comment_states);
            }
        }

        if (mPlaybackService != null) try {
            mPlaybackService.setAutoAdvance(!mIsCommenting);
        } catch (RemoteException ignored) { }
    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    @Override public void onScreenChanging(View newScreen, int newScreenIndex) {}

    @Override public void onNextScreenVisible(View newScreen, int newScreenIndex) {}

    @Override
    public void onScreenChanged(View newScreen, int newScreenIndex) {

        if (newScreen == null) return;
        try {
            final int currentQueuePos = mPlaybackService.getQueuePosition();
            final int newQueuePos = ((PlayerTrackView) newScreen).getPlayPosition();

            if (newQueuePos != currentQueuePos) {
                mPlaybackService.setQueuePosition(newQueuePos);
            }

            final long prevTrackId = newQueuePos > 0
                    ? mPlaybackService.getTrackIdAt(newQueuePos -1) : -1;
            final long nextTrackId = newQueuePos < mPlaybackService.getQueueLength() - 1
                    ? mPlaybackService.getTrackIdAt(newQueuePos + 1) : -1;

            PlayerTrackView ptv;

            if (newScreenIndex == 0 && prevTrackId != -1) {
                final Track prevTrack = getAndCacheTrack(prevTrackId, newQueuePos -1);
                if (prevTrack != null){
                    if (mTrackWorkspace.getChildCount() > 2) {
                        ptv = (PlayerTrackView) mTrackWorkspace.getChildAt(2);
                        mTrackWorkspace.removeViewFromBack();
                    } else {
                        ptv = new PlayerTrackView(this);
                    }
                    mTrackWorkspace.addViewToFront(ptv);
                    ptv.setTrack(prevTrack, newQueuePos - 1, false);
                    mTrackWorkspace.setCurrentScreenNow(1, false);
                }

            } else if (newScreenIndex == mTrackWorkspace.getChildCount() - 1 && nextTrackId != -1) {
                final Track nextTrack = getAndCacheTrack(nextTrackId, newQueuePos + 1);
                if (nextTrack != null){
                    if (mTrackWorkspace.getChildCount() > 2) {
                        ptv = (PlayerTrackView) mTrackWorkspace.getChildAt(0);
                        mTrackWorkspace.removeViewFromFront();
                    } else {
                        ptv = new PlayerTrackView(this);
                    }
                    mTrackWorkspace.addViewToBack(ptv);
                    ptv.setTrack(nextTrack, newQueuePos + 1, false);
                    mTrackWorkspace.setCurrentScreenNow(1, false);
                }
            }

            for (int i = 0; i < mTrackWorkspace.getChildCount(); i++) {
                if (i != mTrackWorkspace.getCurrentScreen()) {
                    ((PlayerTrackView) mTrackWorkspace.getChildAt(i)).getWaveformController().reset();
                }
            }

        } catch (RemoteException ignored) {
        }

    }

    public long setSeekMarker(float seekPercent) {
        try {
            if (mPlaybackService != null) {

                if (!mPlaybackService.isSeekable()) {
                    mSeekPos = -1;
                    return mPlaybackService.position();
                } else {
                    if (mPlayingTrack != null) {
                        long now = SystemClock.elapsedRealtime();
                        if ((now - mLastSeekEventTime) > 250) {
                            mLastSeekEventTime = now;
                            try {
                                mSeekPos = mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent));
                            } catch (RemoteException e) {
                                Log.e(TAG, "error", e);
                            }
                        } else {
                            // where would we be if we had seeked
                            mSeekPos = mPlaybackService.getSeekResult((long) (mPlayingTrack.duration * seekPercent));
                        }
                        return mSeekPos;
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return 0;
    }

    public long sendSeek(float seekPercent) {
        try {
            if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
                return -1;
            }
            mSeekPos = -1;
            return mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent));

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return -1;
    }

    public void onWaveformLoaded(){
        mWaveformLoaded = true;

        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    public boolean isSeekable() {
        try {
            return !(mPlaybackService == null || !mPlaybackService.isSeekable());
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean toggleFavorite(Track track) {
        if (track == null) return false;
        try {
            if (track.user_favorite) {
                    mPlaybackService.setFavoriteStatus(track.id, false);
            } else {
                mPlaybackService.setFavoriteStatus(track.id, true);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            return false;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putBoolean("paused", mActivityPaused);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        mActivityPaused = state.getBoolean("paused");
        super.onRestoreInstanceState(state);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return new Object[] {mPlayingTrack};
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Consts.OptionsMenu.REFRESH:
                mPlayingTrack.info_loaded = false;
                mPlayingTrack.comments_loaded = false;
                mPlayingTrack.comments = null;
                getCurrentTrackView().onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();

        try {
            if (mPlaybackService.getTrackId() != -1) {
                updateTrackDisplay();
                setPauseButtonImage();
                long next = refreshNow();
                queueNextRefresh(next);
            } else {
                Intent intent = new Intent(this, Main.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (RemoteException ignored) {
            Log.e(TAG, "error", ignored);
        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (mPlayingTrack != null && isConnected) {
            if (mTrackWorkspace != null) {
                for (int i = 0; i < mTrackWorkspace.getChildCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getChildAt(i)).onDataConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < mTrackWorkspace.getChildCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getChildAt(i)).onDestroy();
                }
    }

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService == null) {
                return;
            }
            try {
                if (mPlaybackService.position() < 2000) {
                    mPlaybackService.prev();
                } else if (isSeekable()) {
                    mPlaybackService.seek(0);
                    // mService.play();
                } else {
                    mPlaybackService.restart();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService != null) {
                try {
                    mPlaybackService.next();
                } catch (RemoteException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
    };

    private void doPauseResume() {
        try {
            if (mPlaybackService != null) {
                if (mPlaybackService.isSupposedToBePlaying()) {
                    mPlaybackService.pause();
                } else {
                    mPlaybackService.play();
                    if (mWaveformLoaded) mPlaybackService.setClearToPlay(true);
                }
                long next = refreshNow();
                queueNextRefresh(next);
                setPauseButtonImage();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void setPauseButtonImage() {
        if (mPauseButton == null) return;

        try {
            if (mPlaybackService != null && mPlaybackService.isSupposedToBePlaying()) {
                mPauseButton.setImageDrawable(mPauseState);
            } else {
                mPauseButton.setImageDrawable(mPlayState);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
    }

    private void queueNextRefresh(long delay) {
        if (!mActivityPaused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        try {
            if (mPlaybackService == null)
                return REFRESH_DELAY;

            if (mPlaybackService.loadPercent() > 0 && !mIsPlaying) {
                mIsPlaying = true;
            }

            long pos = mPlaybackService.position();
            long remaining = REFRESH_DELAY - (pos % REFRESH_DELAY);

            final PlayerTrackView ptv = getTrackView(mPlaybackService.getQueuePosition());
            if (ptv != null){
                ptv.setProgress(pos, mPlaybackService.loadPercent(), Build.VERSION.SDK_INT >= WaveformController.MINIMUM_SMOOTH_PROGRESS_SDK &&
                        (mPlaybackService.isPlaying() && !mPlaybackService.isBuffering()));
            }

            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return !mPlaybackService.isPlaying() ? REFRESH_DELAY : remaining;

        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        return REFRESH_DELAY;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra("queuePosition", -1);
            String action = intent.getAction();
            if (action.equals(CloudPlaybackService.META_CHANGED)) {
                final int currentQueuePosition = getCurrentTrackView().getPlayPosition();
                if (currentQueuePosition != queuePos){
                    if (queuePos == currentQueuePosition + 1){
                        mTrackWorkspace.scrollRight();
                    } else if (queuePos == currentQueuePosition - 1){
                        mTrackWorkspace.scrollLeft();
                    } else {
                        updateTrackDisplay();
                    }
                }
                setPauseButtonImage();
                try {
                    mPlayingTrack = mPlaybackService.getTrack();
                } catch (RemoteException ignored) { }

            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPauseButtonImage();
                getWaveformController(queuePos).setPlaybackStatus(false, intent.getLongExtra("position", 0));
            } else if (action.equals(CloudPlaybackService.FAVORITE_SET) ||
                        action.equals(CloudPlaybackService.COMMENTS_LOADED) ||
                        action.equals(Consts.IntentActions.COMMENT_ADDED)) {
                for (int i = 0; i < mTrackWorkspace.getChildCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getChildAt(i)).handleIdBasedIntent(intent);
                }
                if (action.equals(CloudPlaybackService.FAVORITE_SET)) setFavoriteStatus();
            } else {
                if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                    setPauseButtonImage();
                }
                if (getTrackView(queuePos) != null) getTrackView(queuePos).handleStatusIntent(intent);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mActivityPaused = false;
        getApp().playerWaitForArtwork = true;

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.PLAYBACK_ERROR);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        f.addAction(Consts.IntentActions.COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        trackPage(Consts.Tracking.PLAYER);

        super.onResume();

        registerHeadphoneRemoteControl();

        updateTrackDisplay();
        setPauseButtonImage();

        long next = refreshNow();
        queueNextRefresh(next);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // no longer have to wait for artwork to load
        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        getApp().playerWaitForArtwork = false;


        for (int i = 0; i < mTrackWorkspace.getChildCount(); i++){
            ((PlayerTrackView) mTrackWorkspace.getChildAt(i)).onStop();
        }
        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    // http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
    private static void initializeRemoteControlRegistrationMethods() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "registerMediaButtonEventReceiver",
                        new Class[] { ComponentName.class });
            }
            if (mUnregisterMediaButtonEventReceiver == null) {
                mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
                        "unregisterMediaButtonEventReceiver",
                        new Class[] { ComponentName.class });
            }
        } catch (NoSuchMethodException ignored) {
            // Android < 2.2
        }
    }

    private void registerHeadphoneRemoteControl() {
        if (mRegisterMediaButtonEventReceiver == null ||
            mAudioManager == null ||
            mRemoteControlResponder == null) return;

        try {
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void unregisterRemoteControl() {
        if (mUnregisterMediaButtonEventReceiver == null) return;

        try {
            mUnregisterMediaButtonEventReceiver.invoke(mAudioManager, mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected", ie);
        }
    }

    private Track getAndCacheTrack(long trackId, int queuePos) {
        if (getApp().getTrackFromCache(trackId) == null) {
            Track t = SoundCloudDB.getTrackById(getContentResolver(), trackId, getCurrentUserId());
            try {
                getApp().cacheTrack(t != null ? t : mPlaybackService.getTrackAt(queuePos - 1));
            } catch (RemoteException ignored) {
            }
        }
        return getApp().getTrackFromCache(trackId);
    }

    private void updateTrackDisplay() {
        if (mPlaybackService == null)
            return;

        try {
            final long trackId = mPlaybackService.getTrackId();

            if (trackId == -1) {
                mPlayingTrack = null;
                return;
            }

            final int currentQueuePosition = mPlaybackService.getQueuePosition();

            mPlayingTrack = getAndCacheTrack(trackId,currentQueuePosition);
            final boolean first = mTrackWorkspace.getChildCount() == 0;

            setFavoriteStatus();

            int workspaceIndex = 0;
            final int queueLength = mPlaybackService.getQueueLength();
            for (int pos = currentQueuePosition -1; pos < currentQueuePosition + 2; pos++){
                if (pos >= 0 && pos < queueLength){
                    PlayerTrackView ptv;
                    if (mTrackWorkspace.getChildCount() > workspaceIndex){
                        ptv = ((PlayerTrackView) mTrackWorkspace.getChildAt(workspaceIndex));
                    } else {
                        ptv = new PlayerTrackView(this);
                        mTrackWorkspace.addViewAtPosition(ptv, workspaceIndex);
                    }
                    ptv.setTrack(getAndCacheTrack(mPlaybackService.getTrackIdAt(pos),pos), pos, false);

                    workspaceIndex++;
                }
            }

            for (int i = 0; i < mTrackWorkspace.getChildCount() - workspaceIndex; i++){
                mTrackWorkspace.removeViewFromBack();
            }

            if (first){
                mTrackWorkspace.initWorkspace(currentQueuePosition > 0 ? 1 : 0);
            } else {
                mTrackWorkspace.setCurrentScreenNow(currentQueuePosition > 0 ? 1 : 0, false);
            }

            if (mPlaybackService.isBuffering()){
                getCurrentTrackView().onBuffering();
            }
        } catch (RemoteException ignored) {}
    }

    private void setFavoriteStatus() {
        if (mPlayingTrack == null || mFavoriteButton == null) {
            return;
        }

        if (mPlayingTrack.user_favorite) {
            if (mFavoritedDrawable == null) mFavoritedDrawable = getResources().getDrawable(R.drawable.ic_liked_states);
            mFavoriteButton.setImageDrawable(mFavoritedDrawable);
        } else {
            if (mFavoriteDrawable == null) mFavoriteDrawable = getResources().getDrawable(R.drawable.ic_like_states);
            mFavoriteButton.setImageDrawable(mFavoriteDrawable);
        }
    }

    private PlayerTrackView getCurrentTrackView(){
        return ((PlayerTrackView) mTrackWorkspace.getChildAt(mTrackWorkspace.getCurrentScreen()));
    }

    private PlayerTrackView getTrackView(int playPos){
        for (int i = 0; i < mTrackWorkspace.getChildCount(); i++){
            if (((PlayerTrackView) mTrackWorkspace.getChildAt(i)).getPlayPosition() == playPos) {
                return ((PlayerTrackView) mTrackWorkspace.getChildAt(i));
            }
        }
        return ((PlayerTrackView) mTrackWorkspace.getChildAt(mTrackWorkspace.getCurrentScreen()));
    }

    private WaveformController getWaveformController(int playPos){
        return getTrackView(playPos).getWaveformController();
    }
}

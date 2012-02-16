
package com.soundcloud.android.activity;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.FocusHelper;
import com.soundcloud.android.view.PlayerTrackView;
import com.soundcloud.android.view.TransportBar;
import com.soundcloud.android.view.WaveformController;
import com.soundcloud.android.view.WorkspaceView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ScPlayer extends ScActivity implements WorkspaceView.OnScreenChangeListener, WorkspaceView.OnScrollListener {
    private static final String TAG = "ScPlayer";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;

    public static final int REFRESH_DELAY = 1000;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long mSeekPos = -1;
    private boolean mWaveformLoaded, mActivityPaused, mIsCommenting, mIsPlaying, mChangeTrackFast, mShouldShowComments;
    private Track mPlayingTrack;
    private RelativeLayout mContainer;
    private WorkspaceView mTrackWorkspace;
    private int mCurrentQueuePosition = -1;
    private TransportBar mTransportBar;

    public interface PlayerError {
        int PLAYBACK_ERROR = 0;
        int STREAM_ERROR = 1;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sc_player);

        mContainer = (RelativeLayout) findViewById(R.id.container);

        mTrackWorkspace = (WorkspaceView) findViewById(R.id.track_view);
        mTrackWorkspace.setVisibility(View.GONE);
        mTrackWorkspace.setOnScreenChangeListener(this);
        mTrackWorkspace.setOnScrollListener(this, false);

        mTransportBar = (TransportBar) findViewById(R.id.transport_bar);
        mTransportBar.setOnPrevListener(mPrevListener);
        mTransportBar.setOnNextListener(mNextListener);
        mTransportBar.setOnPauseListener(mPauseListener);
        mTransportBar.setOnCommentListener(mCommentListener);
        mTransportBar.setOnFavoriteListener(mFavoriteListener);

        mShouldShowComments = getApp().getAccountDataBoolean("playerShowingComments");
        final Object[] saved = (Object[]) getLastNonConfigurationInstance();
        if (saved != null && saved[0] != null) mPlayingTrack = (Track) saved[0];

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


    }

    public void toggleCommentMode(int playPos) {
        mIsCommenting = !mIsCommenting;

        PlayerTrackView ptv = getTrackView(playPos);
        if (ptv != null) {
            ptv.setCommentMode(mIsCommenting);
        }

        mTransportBar.setCommentMode(mIsCommenting);

        if (mPlaybackService != null) try {
            mPlaybackService.setAutoAdvance(!mIsCommenting);
        } catch (RemoteException ignored) {
        }

    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    @Override
    public void onScroll(float screenFraction) {
        if (screenFraction != Math.round(screenFraction)){
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        }
    }

    @Override public void onScreenChanging(View newScreen, int newScreenIndex) {}

    @Override public void onNextScreenVisible(View newScreen, int newScreenIndex) {
        ((PlayerTrackView) newScreen).setOnScreen(true);
    }

    @Override
    public void onScreenChanged(View newScreen, int newScreenIndex) {
        if (newScreen == null) return;
        final int newQueuePos = ((PlayerTrackView) newScreen).getPlayPosition();

        mCurrentQueuePosition = newQueuePos;
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
        mChangeTrackFast = false;

        final long prevTrackId;
        final long nextTrackId;

        try {
            prevTrackId = newQueuePos > 0
                    ? mPlaybackService.getTrackIdAt(newQueuePos - 1) : -1;
            nextTrackId = newQueuePos < mPlaybackService.getQueueLength() - 1
                    ? mPlaybackService.getTrackIdAt(newQueuePos + 1) : -1;
        } catch (RemoteException ignored) {
            return;
        }

        final PlayerTrackView ptv;
        if (newScreenIndex == 0 && prevTrackId != -1) {
            final Track prevTrack = getTrackById(prevTrackId);
            if (prevTrack != null) {
                if (mTrackWorkspace.getScreenCount() > 2) {
                    ptv = (PlayerTrackView) mTrackWorkspace.cycleBackViewToFront();
                    ptv.clear();
                } else {
                    ptv = new PlayerTrackView(this);
                    mTrackWorkspace.addViewToFront(ptv);
                }
                ptv.setTrack(prevTrack, newQueuePos - 1, false, false);
            }

        } else if (newScreenIndex == mTrackWorkspace.getScreenCount() - 1 && nextTrackId != -1) {
            final Track nextTrack = getTrackById(nextTrackId);
            if (nextTrack != null) {
                if (mTrackWorkspace.getScreenCount() > 2) {
                    ptv = (PlayerTrackView) mTrackWorkspace.cycleFrontViewToBack();
                    ptv.clear();
                } else {
                    ptv = new PlayerTrackView(this);
                    mTrackWorkspace.addViewToBack(ptv);
                }
                ptv.setTrack(nextTrack, newQueuePos + 1, false, false);
            }
        }
    }

    public void toggleShowingComments() {
        mShouldShowComments = !mShouldShowComments;
        getApp().setAccountData("playerShowingComments", mShouldShowComments);
    }

    public boolean shouldShowComments() {
        return mShouldShowComments;
    }


    public long setSeekMarker(float seekPercent) {
        try {
            if (mPlaybackService != null) {
                if (!mPlaybackService.isSeekable()) {
                    mSeekPos = -1;
                    return mPlaybackService.getPosition();
                } else {
                    if (mPlayingTrack != null) {
                        // where would we be if we had seeked
                        mSeekPos = mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent), false);
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
            return mPlaybackService.seek(
                    (long) (mPlayingTrack.duration * seekPercent),
                    true);

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
            mPlaybackService.setFavoriteStatus(track.id, !track.user_favorite);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
            return false;
        }
        return true;
    }

    public void onNewComment(Comment comment) {
        final PlayerTrackView ptv = getTrackViewById(comment.track_id);
        if (ptv != null){
            ptv.onNewComment(comment);
        }
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
                mPlayingTrack.full_track_info_loaded = false;
                mPlayingTrack.comments_loaded = false;
                mPlayingTrack.comments = null;
                final PlayerTrackView ptv = getCurrentTrackView();
                if (ptv != null) getCurrentTrackView().onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onServiceBound() {
        super.onServiceBound();

        long trackId = -1;
        try {
            trackId = mPlaybackService.getCurrentTrackId();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (trackId == -1) {
            // nothing to show, send them back to main
            Intent intent = new Intent(this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        } else {
            setTrackDisplayFromService();

            if (getIntent().getBooleanExtra("commentMode", false)) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleCommentMode(getCurrentTrackView().getPlayPosition());
                    }
                }, 200l);
                getIntent().putExtra("commentMode", false);
            }

        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (mPlayingTrack != null && isConnected) {
            if (mTrackWorkspace != null) {
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onDataConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++) {
            ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onDestroy();
        }
        super.onDestroy();
    }

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };

    private final View.OnClickListener mCommentListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleCommentMode(getCurrentTrackView().getPlayPosition());
        }
    };

    private final View.OnClickListener mFavoriteListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleFavorite(mPlayingTrack);
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService == null) return;
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
            try {
                if (mPlaybackService.getPosition() < 2000 && mCurrentQueuePosition > 0) {
                    mChangeTrackFast = true;
                    mTrackWorkspace.scrollLeft();
                } else if (isSeekable()) {
                    mPlaybackService.seek(0, true);
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
            if (mPlaybackService == null) return;
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
            try {
                if (mPlaybackService.getQueueLength() > mCurrentQueuePosition + 1) {
                        mChangeTrackFast = true;
                        mTrackWorkspace.scrollRight();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "error", e);
            }


            mTrackWorkspace.scrollRight();
        }
    };

    private void doPauseResume() {
        try {
            if (mPlaybackService != null) {
                mPlaybackService.toggle();
                if (mWaveformLoaded) mPlaybackService.setClearToPlay(true);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }
        setPlaybackState();
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

            long pos = mPlaybackService.getPosition();
            long remaining = REFRESH_DELAY - (pos % REFRESH_DELAY);
            int queuePos = mPlaybackService.getQueuePosition();

            final PlayerTrackView ptv = getTrackView(queuePos);
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
                case SEND_CURRENT_QUEUE_POSITION:
                    if (mPlaybackService != null) try {
                        mPlaybackService.setQueuePosition(mCurrentQueuePosition);
                    } catch (RemoteException ignore) {}
                    break;
                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra(CloudPlaybackService.BroadcastExtras.queuePosition, -1);
            String action = intent.getAction();

            if (action.equals(CloudPlaybackService.PLAYLIST_CHANGED)) {
                setTrackDisplayFromService();
            } else if (action.equals(CloudPlaybackService.META_CHANGED)) {
                if (mCurrentQueuePosition != queuePos) {
                    if (mCurrentQueuePosition != -1 && queuePos == mCurrentQueuePosition + 1 && !mTrackWorkspace.isScrolling()) { // auto advance
                        mTrackWorkspace.scrollRight();
                    } else {
                        setTrackDisplayFromService();
                    }
                }
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++) {
                    if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getPlayPosition() != queuePos) {
                        ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getWaveformController().reset(false);
                    }
                }

                setCurrentTrackDataFromService(intent.getLongExtra("id",-1));
                long next = refreshNow();
                queueNextRefresh(next);

            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPlaybackState();
                if (getTrackView(queuePos) != null) {
                    getTrackView(queuePos).setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
                }

            } else if (action.equals(CloudPlaybackService.FAVORITE_SET) ||
                        action.equals(CloudPlaybackService.COMMENTS_LOADED) ||
                        action.equals(Consts.IntentActions.COMMENT_ADDED)) {
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).handleIdBasedIntent(intent);
                }
                if (action.equals(CloudPlaybackService.FAVORITE_SET)) setFavoriteStatus();
            } else {
                if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                    setPlaybackState();
                }
                if (getTrackView(queuePos) != null) {
                    getTrackView(queuePos).handleStatusIntent(intent);
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mActivityPaused = false;

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYLIST_CHANGED);
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

        FocusHelper.registerHeadphoneRemoteControl(this);
        setPlaybackState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (mPlaybackService != null) mPlaybackService.setClearToPlay(true);
        } catch (RemoteException e) {
            Log.e(TAG, "error", e);
        }

        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).onStop(true);
        }
        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    private Track getTrackById(long trackId) {
        Track t = SoundCloudApplication.TRACK_CACHE.get(trackId);
        // TODO : StrictMode policy violation; ~duration=106 ms: android.os.StrictMode$StrictModeDiskReadViolation: policy=23 violation=2
        return t != null ? t : SoundCloudDB.getTrackById(getContentResolver(), trackId);
    }

    private void setCurrentTrackDataFromService() {
        try {
            setCurrentTrackDataFromService(mPlaybackService.getCurrentTrackId());
        } catch (RemoteException ignored) { }
    }

    private void setCurrentTrackDataFromService(long id) {
        if (mPlaybackService == null) return;

        try {
            mCurrentQueuePosition = mPlaybackService.getQueuePosition();
            mPlayingTrack = getTrackById(id);
            if (mPlayingTrack == null) {
                mPlayingTrack = mPlaybackService.getCurrentTrack();
            }
        } catch (RemoteException ignored) { }

        setFavoriteStatus();
        setPlaybackState();
    }

    private void setTrackDisplayFromService() {
        if (mPlaybackService == null)
            return;

        setCurrentTrackDataFromService();

        try {
            final long queueLength = mPlaybackService.getQueueLength();

            // setup initial workspace, reusing them if possible
            int workspaceIndex = 0;
            for (int pos = Math.max(0, mCurrentQueuePosition - 1); pos < Math.min(mCurrentQueuePosition + 2, queueLength); pos++) {
                PlayerTrackView ptv;
                if (mTrackWorkspace.getScreenCount() > workspaceIndex) {
                    ptv = ((PlayerTrackView) mTrackWorkspace.getScreenAt(workspaceIndex));
                } else {
                    ptv = new PlayerTrackView(this);
                    mTrackWorkspace.addViewAtScreenPosition(ptv, workspaceIndex);
                }

                final boolean priority = pos == mCurrentQueuePosition;
                ptv.setOnScreen(priority);

                final Track track = priority ? mPlayingTrack : getTrackById(mPlaybackService.getTrackIdAt(pos));
                ptv.setTrack(track, pos, false, priority);
                workspaceIndex++;
            }

            if (queueLength < mTrackWorkspace.getScreenCount()){
                while (queueLength < mTrackWorkspace.getScreenCount()){
                    ((PlayerTrackView) mTrackWorkspace.getLastScreen()).destroy();
                    mTrackWorkspace.removeViewFromBack();
                }
                mTrackWorkspace.resetScroll();
            }

            final int workspacePos = mCurrentQueuePosition > 0 ? 1 : 0;
            if (!mTrackWorkspace.isInitialized()){
                mTrackWorkspace.setVisibility(View.VISIBLE);
                mTrackWorkspace.setSeparator(R.drawable.track_view_seperator);
                mTrackWorkspace.initWorkspace(workspacePos);
            } else if (workspacePos != mTrackWorkspace.getCurrentScreen()){
                mTrackWorkspace.setCurrentScreenNow(mCurrentQueuePosition > 0 ? 1 : 0, false);
            }

            if (mPlaybackService.isBuffering() && getCurrentTrackView() != null){
                getCurrentTrackView().onBuffering();
            }

            if (mIsCommenting) toggleCommentMode(0);
            mTransportBar.setNavEnabled(queueLength > 1);

        } catch (RemoteException ignored) {}


    }

    private void setFavoriteStatus() {
        if (mPlayingTrack != null) mTransportBar.setFavoriteStatus(mPlayingTrack.user_favorite);
    }

    private PlayerTrackView getCurrentTrackView(){
        return ((PlayerTrackView) mTrackWorkspace.getScreenAt(mTrackWorkspace.getCurrentScreen()));
    }

    private PlayerTrackView getTrackView(int playPos){
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getPlayPosition() == playPos) {
                return ((PlayerTrackView) mTrackWorkspace.getScreenAt(i));
            }
        }
        return null;
    }

    private PlayerTrackView getTrackViewById(long track_id) {
        for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
            if (((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).getTrackId() == track_id) {
                return ((PlayerTrackView) mTrackWorkspace.getScreenAt(i));
            }
        }
        return null;
    }

     private void setPlaybackState() {
        final boolean showPlayState = isSupposedToBePlaying();

        if (showPlayState){
            long next = refreshNow();
            queueNextRefresh(next);
        }

         mTransportBar.setPlaybackState(showPlayState);
    }

    private boolean isSupposedToBePlaying() {
        try {
            if (mPlaybackService != null)
                return mPlaybackService.isSupposedToBePlaying();
        } catch (RemoteException ignored) {}
        return false;
    }
}


package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlaylistManager;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.WorkspaceView;
import com.soundcloud.android.view.play.PlayerTrackView;
import com.soundcloud.android.view.play.TransportBar;
import com.soundcloud.android.view.play.WaveformController;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ScPlayer extends ScListActivity implements WorkspaceView.OnScreenChangeListener, WorkspaceView.OnScrollListener {
    public static final String PLAYER_SHOWING_COMMENTS = "playerShowingComments";
    public static final int REFRESH_DELAY = 1000;

    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    protected CloudPlaybackService mPlaybackService;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mIsCommenting, mIsPlaying, mChangeTrackFast, mShouldShowComments;
    private Track mPlayingTrack;
    private RelativeLayout mContainer;
    private WorkspaceView mTrackWorkspace;
    private int mCurrentQueuePosition = -1;
    private TransportBar mTransportBar;

    public interface PlayerError {
        int PLAYBACK_ERROR    = 0;
        int STREAM_ERROR      = 1;
        int TRACK_UNAVAILABLE = 2;
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

        final PlayerTrackView ptv = getTrackView(playPos);
        if (ptv != null) {
            ptv.setCommentMode(mIsCommenting);
        }

        mTransportBar.setCommentMode(mIsCommenting);

        if (mPlaybackService != null) {
            mPlaybackService.setAutoAdvance(!mIsCommenting);
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
        if (newScreen instanceof PlayerTrackView) {
            ((PlayerTrackView) newScreen).setOnScreen(true);
        }
    }

    @Override
    public void onScreenChanged(View newScreen, int newScreenIndex) {

        if (newScreen == null) return;
        final int newQueuePos = ((PlayerTrackView) newScreen).getPlayPosition();

        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        if (mCurrentQueuePosition != newQueuePos){
            mCurrentQueuePosition = newQueuePos;
            mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                    mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
        }
        mChangeTrackFast = false;

        final long prevTrackId;
        final long nextTrackId;

        final PlaylistManager playlistManager = mPlaybackService.getPlaylistManager();
        prevTrackId = newQueuePos > 0
                ? playlistManager.getTrackIdAt(newQueuePos - 1) : -1;
        nextTrackId =  newQueuePos < playlistManager.length() - 1
                ? playlistManager.getTrackIdAt(newQueuePos + 1) : -1;

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
        getApp().setAccountData(PLAYER_SHOWING_COMMENTS, mShouldShowComments);
    }

    public boolean shouldShowComments() {
        return mShouldShowComments;
    }


    public long setSeekMarker(float seekPercent) {
        if (mPlaybackService != null) {
            if (!mPlaybackService.isSeekable()) {
                mSeekPos = -1;
                return mPlaybackService.getProgress();
            } else {
                if (mPlayingTrack != null) {
                    // where would we be if we had seeked
                    mSeekPos = mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent), false);
                    return mSeekPos;
                }
            }
        }
        return 0;
    }

    public long sendSeek(float seekPercent) {
        if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
            return -1;
        }
        mSeekPos = -1;
        return mPlaybackService.seek((long) (mPlayingTrack.duration * seekPercent),true);
    }

    public boolean isSeekable() {
        return !(mPlaybackService == null || !mPlaybackService.isSeekable());
    }

    public boolean toggleLike(Track track) {
        if (track == null) return false;
        mPlaybackService.setFavoriteStatus(track.id, !track.user_favorite);
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
                mPlayingTrack.comments = null;
                final PlayerTrackView ptv = getCurrentTrackView();
                if (ptv != null) ptv.onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            if (obj instanceof LocalBinder) {
                mPlaybackService = (CloudPlaybackService) ((LocalBinder)obj).getService();
                onPlaybackServiceBound();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName classname) {
            mPlaybackService = null;
        }
    };

    protected void onPlaybackServiceBound() {
        if (CloudPlaybackService.getCurrentTrackId() == -1) {
            // nothing to show, send them back to main
            Intent intent = new Intent(this, Main.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            setTrackDisplayFromService();

            if (getIntent().getBooleanExtra("commentMode", false)) {
                final PlayerTrackView view = getCurrentTrackView();
                if (view != null) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toggleCommentMode(view.getPlayPosition());
                        }
                    }, 200l);
                    getIntent().putExtra("commentMode", false);
                }
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
            final PlayerTrackView playerTrackView = getCurrentTrackView();
            if (playerTrackView != null) {
                toggleCommentMode(playerTrackView.getPlayPosition());
            }

        }
    };

    private final View.OnClickListener mFavoriteListener = new View.OnClickListener() {
        public void onClick(View v) {
            toggleLike(mPlayingTrack);
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService != null) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                if (mPlaybackService.getProgress() < 2000 && mCurrentQueuePosition > 0) {
                    if (mPlayingTrack != null) {
                        track(Media.fromTrack(mPlayingTrack), Media.Action.Backward);
                    }
                    mChangeTrackFast = true;
                    mTrackWorkspace.scrollLeft();
                } else if (isSeekable()) {
                    mPlaybackService.seek(0, true);
                } else {
                    mPlaybackService.restartTrack();
                }
            }
        }
    };


    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService != null) {
                if (mPlayingTrack != null) {
                    track(Media.fromTrack(mPlayingTrack), Media.Action.Forward);
                }

                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                if (mPlaybackService.getPlaylistManager().length() > mCurrentQueuePosition + 1) {
                    mChangeTrackFast = true;
                    mTrackWorkspace.scrollRight();
                }
                mTrackWorkspace.scrollRight();
            }
        }
    };

    private void doPauseResume() {
        if (mPlaybackService != null) mPlaybackService.togglePlayback();
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
        if (mPlaybackService == null)
            return REFRESH_DELAY;

        if (mPlaybackService.loadPercent() > 0 && !mIsPlaying) {
            mIsPlaying = true;
        }

        long progress = mPlaybackService.getProgress();
        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);
        int queuePos = mPlaybackService.getPlaylistManager().getPosition();

        final PlayerTrackView ptv = getTrackView(queuePos);
        if (ptv != null){
            ptv.setProgress(progress, mPlaybackService.loadPercent(), Build.VERSION.SDK_INT >= WaveformController.MINIMUM_SMOOTH_PROGRESS_SDK &&
                    (mPlaybackService.isPlaying() && !mPlaybackService.isBuffering()));
        }

        // return the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        return !mPlaybackService.isPlaying() ? REFRESH_DELAY : remaining;
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
                    if (mPlaybackService != null) mPlaybackService.setQueuePosition(mCurrentQueuePosition);
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
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                setTrackDisplayFromService();
            } else if (action.equals(CloudPlaybackService.META_CHANGED)) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                if (mCurrentQueuePosition != queuePos) {
                    if (mCurrentQueuePosition != -1
                            && queuePos == mCurrentQueuePosition + 1
                            && !mTrackWorkspace.isScrolling()
                            && mTrackWorkspace.isScrollerFinished()) {
                        // auto advance
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
                        action.equals(Actions.COMMENT_ADDED)) {
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

        AndroidUtils.bindToService(this, CloudPlaybackService.class, osc);

        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYLIST_CHANGED);
        f.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(CloudPlaybackService.META_CHANGED);
        f.addAction(CloudPlaybackService.PLAYBACK_ERROR);
        f.addAction(CloudPlaybackService.TRACK_UNAVAILABLE);
        f.addAction(CloudPlaybackService.STREAM_DIED);
        f.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        f.addAction(CloudPlaybackService.BUFFERING);
        f.addAction(CloudPlaybackService.BUFFERING_COMPLETE);
        f.addAction(CloudPlaybackService.COMMENTS_LOADED);
        f.addAction(CloudPlaybackService.SEEKING);
        f.addAction(CloudPlaybackService.SEEK_COMPLETE);
        f.addAction(CloudPlaybackService.FAVORITE_SET);
        f.addAction(Actions.COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPlaybackState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        AndroidUtils.unbindFromService(this, CloudPlaybackService.class);

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
        setCurrentTrackDataFromService(CloudPlaybackService.getCurrentTrackId());
    }

    private void setCurrentTrackDataFromService(long id) {
        if (mPlaybackService == null) return;

        mCurrentQueuePosition = mPlaybackService.getPlaylistManager().getPosition();
        mPlayingTrack = getTrackById(id);
        if (mPlayingTrack == null) {
            mPlayingTrack = CloudPlaybackService.getCurrentTrack();
        }

        setFavoriteStatus();
        setPlaybackState();
    }

    private void setTrackDisplayFromService() {
        if (mPlaybackService == null)
            return;

        setCurrentTrackDataFromService();

        final long queueLength = mPlaybackService.getPlaylistManager().length();

        // setup initial workspace, reusing them if possible
        int workspaceIndex = 0;
        for (int pos = Math.max(0, mCurrentQueuePosition - 1); pos < Math.min(mCurrentQueuePosition + 2, queueLength); pos++) {
            final PlayerTrackView ptv;
            if (mTrackWorkspace.getScreenCount() > workspaceIndex) {
                ptv = ((PlayerTrackView) mTrackWorkspace.getScreenAt(workspaceIndex));
            } else {
                ptv = new PlayerTrackView(this);
                mTrackWorkspace.addViewAtScreenPosition(ptv, workspaceIndex);
            }

            final boolean priority = pos == mCurrentQueuePosition;
            ptv.setOnScreen(priority);

            final Track track = priority ? mPlayingTrack : getTrackById(mPlaybackService.getPlaylistManager().getTrackIdAt(pos));
            if (track != null) {
                ptv.setTrack(track, pos, false, priority);
                workspaceIndex++;
            }
        }

        if (queueLength < mTrackWorkspace.getScreenCount()) {
            while (queueLength < mTrackWorkspace.getScreenCount()) {
                ((PlayerTrackView) mTrackWorkspace.getLastScreen()).destroy();
                mTrackWorkspace.removeViewFromBack();
            }
        }

        mTrackWorkspace.resetScroll();

        final int workspacePos = mCurrentQueuePosition > 0 ? 1 : 0;
        if (!mTrackWorkspace.isInitialized()) {
            mTrackWorkspace.setVisibility(View.VISIBLE);
            mTrackWorkspace.setSeparator(R.drawable.track_view_separator);
            mTrackWorkspace.initWorkspace(workspacePos);
        } else if (workspacePos != mTrackWorkspace.getCurrentScreen()) {
            mTrackWorkspace.setCurrentScreenNow(mCurrentQueuePosition > 0 ? 1 : 0, false);
        }

        PlayerTrackView currentTrackView = getCurrentTrackView();
        if (mPlaybackService.isBuffering() && currentTrackView != null) {
            currentTrackView.onBuffering();
        }

        if (mIsCommenting) toggleCommentMode(0);
        mTransportBar.setNavEnabled(queueLength > 1);

    }

    private void setFavoriteStatus() {
        if (mPlayingTrack != null) mTransportBar.setFavoriteStatus(mPlayingTrack.user_favorite);
    }

    private @Nullable PlayerTrackView getCurrentTrackView() {
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
        final boolean showPlayState = CloudPlaybackService.getState().isSupposedToBePlaying();

        if (showPlayState){
            long next = refreshNow();
            queueNextRefresh(next);
        }

         mTransportBar.setPlaybackState(showPlayState);
    }
}

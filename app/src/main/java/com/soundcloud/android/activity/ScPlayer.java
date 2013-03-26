
package com.soundcloud.android.activity;

import static com.soundcloud.android.service.playback.CloudPlaybackService.getPlaylistManager;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.service.playback.State;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.PlayerTrackPager;
import com.soundcloud.android.view.play.PlayerTrackView;
import com.soundcloud.android.view.play.TransportBar;
import com.soundcloud.android.view.play.WaveformController;
import org.jetbrains.annotations.NotNull;
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
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

public class ScPlayer extends ScActivity implements PlayerTrackPager.OnTrackPageListener {
    public static final int REFRESH_DELAY = 1000;

    private static final String STATE_PAGER_QUEUE_POSITION = "pager_queue_position";
    private static final String PLAYER_SHOWING_COMMENTS = "playerShowingComments";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;
    public static final boolean SMOOTH_PROGRESS = Build.VERSION.SDK_INT >= WaveformController.MINIMUM_SMOOTH_PROGRESS_SDK;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mChangeTrackFast, mShouldShowComments, mIgnoreServiceQueue;
    private PlayerTrackPager mTrackPager;
    private TransportBar mTransportBar;
    private @Nullable CloudPlaybackService mPlaybackService;
    private int mPendingPlayPosition = -1, mCommentingPosition = -1;

    public int getCommentPosition() {
        return mCommentingPosition;
    }

    public interface PlayerError {
        int PLAYBACK_ERROR    = 0;
        int STREAM_ERROR      = 1;
        int TRACK_UNAVAILABLE = 2;
    }

    public @Nullable static Comment pendingComment;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.sc_player);

        mTrackPager = (PlayerTrackPager) findViewById(R.id.track_view);
        mTrackPager.setPageMarginDrawable(R.drawable.track_view_separator);
        mTrackPager.setPageMargin((int) (5*getResources().getDisplayMetrics().density));
        mTrackPager.setListener(this);

        mTransportBar = (TransportBar) findViewById(R.id.transport_bar);
        mTransportBar.setOnPrevListener(mPrevListener);
        mTransportBar.setOnNextListener(mNextListener);
        mTransportBar.setOnPauseListener(mPauseListener);
        mTransportBar.setOnCommentListener(mCommentListener);

        mShouldShowComments = getApp().getAccountDataBoolean(PLAYER_SHOWING_COMMENTS);

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (bundle == null) {
            handleIntent(getIntent());
        } else {
            // orientation change, activity got recreated
            mIgnoreServiceQueue = false;
        }
    }

    @Override
    public void onPageDrag() {
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
    }

    @Override
    public void onPageSettling() {
        final PlayQueueManager playQueueManager = getPlaylistManager();
        if (playQueueManager != null) {
            if (playQueueManager.getPosition() != getCurrentDisplayedTrackPosition() // different track
                    && !mHandler.hasMessages(SEND_CURRENT_QUEUE_POSITION) // not already changing
                    && (mChangeTrackFast || CloudPlaybackService.getState().isSupposedToBePlaying()) // responding to transport click or already playing
                    ) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                        mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
            }
            mChangeTrackFast = false;
        }

        mTransportBar.setIsCommenting(getCurrentDisplayedTrackPosition() == mCommentingPosition);
    }

    public void toggleShowingComments() {
        mShouldShowComments = !mShouldShowComments;
        getApp().setAccountData(PLAYER_SHOWING_COMMENTS, mShouldShowComments);
    }

    public boolean shouldShowComments() {
        return mShouldShowComments;
    }

    public long setSeekMarker(int queuePosition, float seekPercent) {
        final PlayQueueManager playlistManager = getPlaylistManager();
        if (mPlaybackService != null && playlistManager != null) {
            if (playlistManager.getPosition() != queuePosition) {
                mPlaybackService.setQueuePosition(queuePosition);
            } else {
                if (!mPlaybackService.isSeekable()) {
                    mSeekPos = -1;
                    return mPlaybackService.getProgress();
                } else {
                    // returns where would we be if we had seeked
                    mSeekPos = mPlaybackService.seek(seekPercent, false);
                    return mSeekPos;
                }
            }
        }

        return -1;
    }

    public long sendSeek(float seekPercent) {

        if (mPlaybackService == null || !mPlaybackService.isSeekable()) {
            return -1;
        }
        mSeekPos = -1;
        return mPlaybackService.seek(seekPercent,true);
    }

    public boolean isSeekable() {
        return !(mPlaybackService == null || !mPlaybackService.isSeekable());
    }

    public void onNewComment(Comment comment) {
        final PlayerTrackView ptv = getTrackViewById(comment.track_id);
        if (ptv != null){
            ptv.onNewComment(comment);
        }
    }

    @Override
    public void onBackPressed() {
        final PlayerTrackView currentTrackView = mTrackPager.getCurrentTrackView();
        if (currentTrackView == null || !currentTrackView.onBackPressed() ){
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_PAGER_QUEUE_POSITION, getCurrentDisplayedTrackPosition());
        super.onSaveInstanceState(state);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        final int position = state.getInt(STATE_PAGER_QUEUE_POSITION, -1);
        if (position != -1 && position != getCurrentDisplayedTrackPosition()){
            mPendingPlayPosition = position;
        }
        super.onRestoreInstanceState(state);
    }

    public void closeCommentMode(){
        mCommentingPosition = -1;
        setCommentMode(true);
    }

    private void setCommentMode(boolean animate){
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()) {
            ptv.setCommentMode((ptv.getPlayPosition() == mCommentingPosition), animate);
        }
        mTransportBar.setIsCommenting(getCurrentDisplayedTrackPosition() == mCommentingPosition);
    }

    public void addNewComment(final Comment comment) {
        closeCommentMode();
        pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    protected void onPlaybackServiceBound(@NotNull CloudPlaybackService service) {
        if (mPendingPlayPosition != -1) {
            service.setQueuePosition(mPendingPlayPosition);
            mPendingPlayPosition = -1;
        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected) {
            if (mTrackPager != null) {
                for (PlayerTrackView ptv : mTrackPager.playerTrackViews()) {
                    ptv.onDataConnected();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()) {
            ptv.onDestroy();
        }
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        final String action = intent.getAction();
        Track displayTrack = null;
        if (!TextUtils.isEmpty(action)) {
            if (Actions.PLAY.equals(action)) {
                // play from a normal play intent (created by PlayUtils)
                startService(new Intent(CloudPlaybackService.PLAY_ACTION, intent.getData()).putExtras(intent));
                displayTrack = PlayUtils.getTrackFromIntent(intent);
            } else if (Intent.ACTION_VIEW.equals(action)) {
                // Play from a View Intent, this probably came from quicksearch
                if (intent.getData() != null) {
                    displayTrack = Track.fromUri(intent.getData(), getContentResolver(), true);
                    if (displayTrack != null) {
                        startService(new Intent(CloudPlaybackService.PLAY_ACTION).putExtra(Track.EXTRA, displayTrack));
                    }
                }
            }
        }
        if (displayTrack != null) {
            mTrackPager.configureFromTrack(this, displayTrack,
                    intent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition, 0));
            mIgnoreServiceQueue = true;
        }

        // only handle intent once for now (currently they are just one shot playback requests)
        final Bundle extras = intent.getExtras();
        if (extras != null) extras.clear();
        intent.setData(null);
    }

    private final ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            if (obj instanceof LocalBinder) {
                mPlaybackService = (CloudPlaybackService) ((LocalBinder)obj).getService();
                onPlaybackServiceBound(mPlaybackService);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName classname) {
            mPlaybackService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mActivityPaused = false;

        if (!mIgnoreServiceQueue) {

            // this will configure the playlist from the service
            final PlayQueueManager playQueueManager = CloudPlaybackService.getPlaylistManager();
            if (playQueueManager != null && !playQueueManager.isEmpty()) {
                // everything is fine, configure from service
                onMetaChanged(playQueueManager.getPosition());

            } else {
                /* service doesn't exist or playqueue is empty and not loading.
                   start it, it will reload queue and broadcast changes */
                startService(new Intent(this, CloudPlaybackService.class));
            }

        } else {
            // set to false for coming back from lock screen
            mIgnoreServiceQueue = false;
        }

        bindService(new Intent(this, CloudPlaybackService.class), osc, 0);
        IntentFilter f = new IntentFilter();
        f.addAction(CloudPlaybackService.PLAYQUEUE_CHANGED);
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
        f.addAction(Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED);
        f.addAction(Playable.ACTION_SOUND_INFO_UPDATED);
        f.addAction(Playable.ACTION_SOUND_INFO_ERROR);
        f.addAction(Playable.COMMENTS_UPDATED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPlaybackState();
        setBufferingState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(osc);
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            ptv.onStop(true);
        }

        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    private final View.OnClickListener mCommentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCommentingPosition = ((CompoundButton) v).isChecked() ? getCurrentDisplayedTrackPosition() : -1;
            setCommentMode(true);
        }
    };

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            final PlayQueueManager playQueueManager = getPlaylistManager();
            if (mPlaybackService != null && playQueueManager != null) {
                if (getCurrentDisplayedTrackPosition() != playQueueManager.getPosition()) {
                    mPlaybackService.setQueuePosition(getCurrentDisplayedTrackPosition());
                } else {
                    mPlaybackService.togglePlayback();
                }
            } else {
                startService(new Intent(CloudPlaybackService.TOGGLEPAUSE_ACTION));
            }

            setPlaybackState();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            final PlayQueueManager playQueueManager = getPlaylistManager();
            if (mPlaybackService != null && playQueueManager != null) {
                final int playPosition = playQueueManager.getPosition();
                if (mPlaybackService.getProgress() < 2000 && playPosition > 0) {

                    final Track currentTrack = CloudPlaybackService.getCurrentTrack();
                    if (currentTrack != null) {
                        track(Media.fromTrack(currentTrack), Media.Action.Backward);
                    }

                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        mChangeTrackFast = true;
                        mTrackPager.prev();
                    } else {
                        mPlaybackService.setQueuePosition(playPosition - 1);
                        setTrackDisplayFromService();
                    }

                } else if (isSeekable()) {
                    mPlaybackService.seek(0, true);

                } else {
                    mPlaybackService.restartTrack();
                }
            } else {
                startService(new Intent(CloudPlaybackService.PREVIOUS_ACTION));
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                final Track currentTrack = CloudPlaybackService.getCurrentTrack();
                if (currentTrack != null) {
                    track(Media.fromTrack(currentTrack), Media.Action.Forward);
                }

                final PlayQueueManager playQueueManager = getPlaylistManager();
                if (mPlaybackService != null && playQueueManager != null) {
                    final int playPosition = playQueueManager.getPosition();
                    if (playQueueManager.length() > playPosition + 1) {
                        if (getCurrentDisplayedTrackPosition() == playPosition) {
                            mChangeTrackFast = true;
                            mTrackPager.next();
                        } else {
                            mPlaybackService.setQueuePosition(playPosition + 1);
                            setTrackDisplayFromService();
                        }
                    }
                } else {
                    startService(new Intent(CloudPlaybackService.NEXT_ACTION));
                }

        }
    };

    private void queueNextRefresh(long delay) {
        if (!mActivityPaused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        long progress = CloudPlaybackService.getCurrentProgress();
        final PlayQueueManager playlistManager = CloudPlaybackService.getPlaylistManager();

        if (playlistManager != null){
            final PlayerTrackView ptv = getTrackView(playlistManager.getPosition());
            if (ptv != null) {
                ptv.setProgress(progress, CloudPlaybackService.getLoadingPercent(),
                        SMOOTH_PROGRESS && CloudPlaybackService.getState() == State.PLAYING);
            }
        } else return  REFRESH_DELAY;

        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);

        // return the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        return !CloudPlaybackService.getState().isSupposedToBePlaying() ? REFRESH_DELAY : remaining;
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
                    if (mPlaybackService != null) mPlaybackService.setQueuePosition(getCurrentDisplayedTrackPosition());
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
            if (action.equals(CloudPlaybackService.PLAYQUEUE_CHANGED)) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                setTrackDisplayFromService();
            } else if (action.equals(CloudPlaybackService.META_CHANGED)) {
                onMetaChanged(queuePos);

            } else {

                if (CloudPlaybackService.PLAYBACK_COMPLETE.equals(action) || action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {
                    setPlaybackState();
                    final PlayerTrackView trackView = getTrackView(queuePos);
                    if (trackView != null) {
                        if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)){
                            trackView.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
                        } else {
                            trackView.handleStatusIntent(intent);
                        }
                    }
                } else {
                    // unhandled here, pass along to trackviews who may be interested
                    for (PlayerTrackView ptv : mTrackPager.playerTrackViews()) {
                        ptv.handleIdBasedIntent(intent);
                    }
                }
            }

        }
    };

    private void onMetaChanged(int queuePos) {

        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        final int playPosition = getCurrentDisplayedTrackPosition();
        if (playPosition != queuePos) {
            if (playPosition != -1
                    && queuePos == playPosition + 1
                    && !mTrackPager.isScrolling()) {
                // auto advance
                mTrackPager.next();
            } else {
                setTrackDisplayFromService();
            }
        }

        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()) {
            if (ptv.getPlayPosition() != queuePos) {
                ptv.getWaveformController().reset(false);
            }
        }
        setPlaybackState();
        long next = refreshNow();
        if (CloudPlaybackService.getState().isSupposedToBePlaying()){
            queueNextRefresh(next);
        }
    }

    private void setTrackDisplayFromService() {
        setTrackDisplayFromService(-1);
    }

    private void setTrackDisplayFromService(int queuePosition) {
        final PlayQueueManager playQueueManager = getPlaylistManager();

        mTrackPager.configureFromService(this, playQueueManager, queuePosition);
        setBufferingState();

        setCommentMode(false);
        setPlaybackState();
    }

    private void setBufferingState() {
        final PlayerTrackView playerTrackView = getTrackViewById(CloudPlaybackService.getCurrentTrackId());
        if (playerTrackView != null) {
            // set buffering state of current track
            playerTrackView.setBufferingState(CloudPlaybackService.isBuffering());
        }
    }

    private int getCurrentDisplayedTrackPosition() {
        final PlayerTrackView currentTrackView = mTrackPager.getCurrentTrackView();
        return currentTrackView == null ? -1 : currentTrackView.getPlayPosition();
    }

    private @Nullable PlayerTrackView getTrackView(int playPos){
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            if (ptv.getPlayPosition() == playPos) {
                return ptv;
            }
        }
        return null;
    }

    private @Nullable PlayerTrackView getTrackViewById(long track_id) {
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            if (ptv.getTrackId() == track_id) {
                return ptv;
            }
        }
        return null;
    }

    private void setPlaybackState() {
        final boolean showPlayState = CloudPlaybackService.getState().isSupposedToBePlaying();

        if (showPlayState) {
            long next = refreshNow();
            queueNextRefresh(next);
        }

        mTransportBar.setPlaybackState(showPlayState);

        final PlayQueueManager playQueueManager = getPlaylistManager();

        if (playQueueManager != null) {
            int pos    = playQueueManager.getPosition();
            int length = playQueueManager.length();

            mTransportBar.setPreviousEnabled(pos > 0);
            mTransportBar.setNextEnabled(pos < (length - 1));
        }
    }
}

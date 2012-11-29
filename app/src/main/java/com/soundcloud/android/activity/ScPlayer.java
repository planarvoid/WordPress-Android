
package com.soundcloud.android.activity;

import static com.soundcloud.android.service.playback.CloudPlaybackService.getPlayQueueManager;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.PlayerTrackPager;
import com.soundcloud.android.view.play.PlayerTrackView;
import com.soundcloud.android.view.play.TransportBar;
import com.soundcloud.android.view.play.WaveformController;

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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ScPlayer extends ScActivity implements PlayerTrackPager.OnTrackPageListener {
    public static final int REFRESH_DELAY = 1000;

    private static final String STATE_PAGER_QUEUE_POSITION = "pager_queue_position";
    private static final String PLAYER_SHOWING_COMMENTS = "playerShowingComments";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mChangeTrackFast, mShouldShowComments, mConfigureFromService = true;
    private RelativeLayout mContainer;
    private PlayerTrackPager mTrackPager;
    private TransportBar mTransportBar;
    private CloudPlaybackService mPlaybackService;

    private int mPendingPlayPosition = -1;

    public interface PlayerError {
        int PLAYBACK_ERROR    = 0;
        int STREAM_ERROR      = 1;
        int TRACK_UNAVAILABLE = 2;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sc_player);
        setTitle("");

        mContainer = (RelativeLayout) findViewById(R.id.container);
        mTrackPager = (PlayerTrackPager) findViewById(R.id.track_view);
        mTrackPager.setPageMarginDrawable(R.drawable.track_view_separator);
        mTrackPager.setPageMargin((int) (5*getResources().getDisplayMetrics().density));
        mTrackPager.setListener(this);

        mTransportBar = (TransportBar) findViewById(R.id.transport_bar);
        mTransportBar.setOnPrevListener(mPrevListener);
        mTransportBar.setOnNextListener(mNextListener);
        mTransportBar.setOnPauseListener(mPauseListener);

        mShouldShowComments = getApp().getAccountDataBoolean(PLAYER_SHOWING_COMMENTS);

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (icicle == null){
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){
        final String action = intent.getAction();
        if (!TextUtils.isEmpty(action)){
            Track displayTrack = null;
            if (Actions.PLAY.equals(action)) {
                // play from a normal play intent (created by PlayUtils)
                startService(
                        new Intent(this, CloudPlaybackService.class)
                                .setAction(CloudPlaybackService.PLAY_ACTION)
                                .setData(intent.getData())
                                .putExtras(intent)
                );
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
            if (displayTrack != null) {
                mTrackPager.configureFromTrack(this, displayTrack,
                        intent.getIntExtra(CloudPlaybackService.PlayExtras.playPosition, 0));
                mConfigureFromService = false;
            }
        }
    }



    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    public void setCommentMode(boolean mIsCommenting) {
        if (mPlaybackService != null) {
            mPlaybackService.setAutoAdvance(!mIsCommenting);
        }
    }

    public ViewGroup getCommentHolder() {
        return mContainer;
    }

    @Override
    public void onPageBeingDragged() {
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
    }

    @Override
    public void onTrackPageChanged(PlayerTrackView newTrackView) {
        final PlayQueueManager playQueueManager = getPlayQueueManager();
        if (playQueueManager != null) {

            refreshCurrentViewedTrackData();
            // only respond by changing tracks if this wasn't a swipe or we are already playing
            if (mPlaybackService != null && (mChangeTrackFast || CloudPlaybackService.getState().isSupposedToBePlaying())) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                        mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
            }

            mChangeTrackFast = false;
        }
    }

    public void toggleShowingComments() {
        mShouldShowComments = !mShouldShowComments;
        getApp().setAccountData(PLAYER_SHOWING_COMMENTS, mShouldShowComments);
    }

    public boolean shouldShowComments() {
        return mShouldShowComments;
    }

    public long setSeekMarker(int queuePosition, float seekPercent) {
        if (mPlaybackService != null) {
            if (mPlaybackService.getPlaylistManager().getPosition() != queuePosition) {
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

    public boolean toggleLike(Track track) {
        if (track == null) return false;
        mPlaybackService.setLikeStatus(track.id, !track.user_like);
        return true;
    }

    public boolean toggleRepost(Track track) {
        if (track == null) return false;
        mPlaybackService.setRepostStatus(track.id, !track.user_repost);
        return true;
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

    public void addNewComment(final Comment comment) {
        getApp().pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
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
        if (CloudPlaybackService.getCurrentTrackId() == -1 && !mPlaybackService.configureLastPlaylist()) {
            // nothing to show, send them back to main
            Intent intent = new Intent(this, News.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (mPendingPlayPosition != -1){
            mPlaybackService.setQueuePosition(mPendingPlayPosition);
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

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService != null) {
                final PlayQueueManager playQueueManager = getPlayQueueManager();
                if (playQueueManager != null) {
                    if (getCurrentDisplayedTrackPosition() != playQueueManager.getPosition()) {
                        mPlaybackService.setQueuePosition(getCurrentDisplayedTrackPosition());
                    } else {
                        mPlaybackService.togglePlayback();
                    }
                }
            }
            setPlaybackState();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {

            if (mPlaybackService != null) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                final PlayQueueManager playQueueManager = getPlayQueueManager();
                if (playQueueManager != null) {
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
                }
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlaybackService != null) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                final Track currentTrack = CloudPlaybackService.getCurrentTrack();
                if (currentTrack != null) {
                    track(Media.fromTrack(currentTrack), Media.Action.Forward);
                }
                final PlayQueueManager playQueueManager = getPlayQueueManager();
                if (playQueueManager != null) {
                    final int playPosition = playQueueManager.getPosition();
                    if (mPlaybackService.getPlaylistManager().length() > playPosition + 1) {
                        if (getCurrentDisplayedTrackPosition() == playPosition) {
                            mChangeTrackFast = true;
                            mTrackPager.next();
                        } else {
                            mPlaybackService.setQueuePosition(playPosition + 1);
                            setTrackDisplayFromService();
                        }
                    }
                }
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

                refreshCurrentViewedTrackData();
                long next = refreshNow();
                queueNextRefresh(next);

            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPlaybackState();
                if (getTrackView(queuePos) != null) {
                    getTrackView(queuePos).setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
                }

            } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED) ||
                    action.equals(Sound.ACTION_TRACK_ASSOCIATION_CHANGED) ||
                    action.equals(Sound.ACTION_SOUND_INFO_UPDATED) ||
                    action.equals(Sound.ACTION_SOUND_INFO_ERROR) ||
                    action.equals(Sound.ACTION_COMMENT_ADDED)) {

                for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
                    ptv.handleIdBasedIntent(intent);
                }

                if (action.equals(Sound.ACTION_TRACK_ASSOCIATION_CHANGED) || action.equals(Sound.ACTION_COMMENT_ADDED)) {
                    invalidateOptionsMenu();

                }

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
        f.addAction(Sound.ACTION_TRACK_ASSOCIATION_CHANGED);
        f.addAction(Sound.ACTION_SOUND_INFO_UPDATED);
        f.addAction(Sound.ACTION_SOUND_INFO_ERROR);
        f.addAction(Sound.ACTION_COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));

        if (mConfigureFromService) {
            setTrackDisplayFromService(mPendingPlayPosition);
        }
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
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            ptv.onStop(true);
        }

        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    private void refreshCurrentViewedTrackData() {
        invalidateOptionsMenu();
        setPlaybackState();
    }

    private void setTrackDisplayFromService() {
        setTrackDisplayFromService(-1);
    }

    private void setTrackDisplayFromService(int queuePosition) {
        mTrackPager.configureFromService(this, queuePosition);

        final PlayQueueManager playQueueManager = getPlayQueueManager();
        final long queueLength = playQueueManager == null ? 1 :playQueueManager.length();
        mTransportBar.setNavEnabled(queueLength > 1);
        refreshCurrentViewedTrackData();
    }


    /**
     * Returns the track in the current track display (not necessarily the track that is currently playing)
     */
    private Track getCurrentDisplayedTrack() {
        final PlayerTrackView currentTrackView = mTrackPager.getCurrentTrackView();
        return currentTrackView == null ? null : currentTrackView.getTrack();
    }

    private int getCurrentDisplayedTrackPosition() {
        final PlayerTrackView currentTrackView = mTrackPager.getCurrentTrackView();
        return currentTrackView == null ? -1 : currentTrackView.getPlayPosition();
    }


    private PlayerTrackView getTrackView(int playPos){
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            if (ptv.getPlayPosition() == playPos) {
                return ptv;
            }
        }
        return null;
    }

    private PlayerTrackView getTrackViewById(long track_id) {
        for (PlayerTrackView ptv : mTrackPager.playerTrackViews()){
            if (ptv.getTrackId() == track_id) {
                return ptv;
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

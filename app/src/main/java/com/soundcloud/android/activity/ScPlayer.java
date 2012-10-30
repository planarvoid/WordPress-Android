
package com.soundcloud.android.activity;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.landing.News;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.tracking.Click;
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
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ScPlayer extends ScActivity implements WorkspaceView.OnScreenChangeListener, WorkspaceView.OnScrollListener {
    public static final String PLAYER_SHOWING_COMMENTS = "playerShowingComments";
    public static final int REFRESH_DELAY = 1000;

    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mIsCommenting, mChangeTrackFast, mShouldShowComments;
    private RelativeLayout mContainer;
    private WorkspaceView mTrackWorkspace;
    private TransportBar mTransportBar;
    private CloudPlaybackService mPlaybackService;

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

        mShouldShowComments = getApp().getAccountDataBoolean(PLAYER_SHOWING_COMMENTS);
        final Object[] saved = (Object[]) getLastCustomNonConfigurationInstance();

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    public void toggleCommentMode(int playPos) {
        setCommentMode(!mIsCommenting, playPos);
    }

    public void setCommentMode(boolean mIsCommenting, int playPos) {
        this.mIsCommenting = mIsCommenting;

        final PlayerTrackView ptv = getTrackView(playPos);
        if (ptv != null) {
            ptv.setCommentMode(mIsCommenting);
        }

        if (mPlaybackService != null) {
            mPlaybackService.setAutoAdvance(!mIsCommenting);
        }

        invalidateOptionsMenu();
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



        final PlayQueueManager playQueueManager = CloudPlaybackService.getPlayQueueManager();
        int currentQueuePosition = playQueueManager.getPosition();

        if (currentQueuePosition != newScreenIndex) {
            setCommentMode(false, newScreenIndex);
        }

        final int newQueuePos = ((PlayerTrackView) newScreen).getPlayPosition();

        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        if (currentQueuePosition != newQueuePos){
            currentQueuePosition = newQueuePos;
            mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                    mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
        }
        mChangeTrackFast = false;

        final long prevTrackId;
        final long nextTrackId;


        prevTrackId = newQueuePos > 0
                ? playQueueManager.getTrackIdAt(newQueuePos - 1) : -1;
        nextTrackId =  newQueuePos < playQueueManager.length() - 1
                ? playQueueManager.getTrackIdAt(newQueuePos + 1) : -1;

        final PlayerTrackView ptv;
        if (newScreenIndex == 0 && prevTrackId != -1) {
            final Track prevTrack = SoundCloudApplication.MODEL_MANAGER.getTrack(prevTrackId);
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
            final Track nextTrack = SoundCloudApplication.MODEL_MANAGER.getTrack(nextTrackId);
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

        invalidateOptionsMenu();
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
    public boolean onOptionsItemSelected(MenuItem item) {

        final PlayerTrackView currentTrackView = getCurrentTrackView();
        final Track displayedTrack = currentTrackView == null ? null : currentTrackView.getTrack();

        switch (item.getItemId()) {

            case R.id.action_bar_comment:
                if (displayedTrack != null){
                    toggleCommentMode(currentTrackView.getPlayPosition());
                    track(Click.Comment, displayedTrack);
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.action_bar_like:
                if (displayedTrack != null){
                    toggleLike(displayedTrack);
                    track(Click.Like, displayedTrack);
                }
                return true;

            case R.id.action_bar_repost:
                if (displayedTrack != null) {
                    toggleRepost(displayedTrack);
                    track(Click.Repost, displayedTrack);
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.action_bar_info:
                if (currentTrackView != null) {
                    currentTrackView.onTrackInfoFlip();
                }
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
        if (CloudPlaybackService.getCurrentTrackId() == -1 && !mPlaybackService.configureLastPlaylist()) {
            // nothing to show, send them back to main
            Intent intent = new Intent(this, News.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected) {
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

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {

            if (mPlaybackService != null) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                final int playPosition = CloudPlaybackService.getPlayQueueManager().getPosition();
                if (mPlaybackService.getProgress() < 2000 && playPosition > 0) {

                    final Track currentTrack = CloudPlaybackService.getCurrentTrack();
                    if (currentTrack != null) {
                        track(Media.fromTrack(currentTrack), Media.Action.Backward);
                    }

                    if (getCurrentDisplayedTrackPosition() == playPosition){
                        mChangeTrackFast = true;
                        mTrackWorkspace.scrollLeft();
                    } else {
                        setTrackDisplayFromService();
                    }

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
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                final Track currentTrack = CloudPlaybackService.getCurrentTrack();
                if (currentTrack != null) {
                    track(Media.fromTrack(currentTrack), Media.Action.Forward);
                }

                final int playPosition = CloudPlaybackService.getPlayQueueManager().getPosition();
                if (mPlaybackService.getPlaylistManager().length() > playPosition + 1) {
                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        mChangeTrackFast = true;
                        mTrackWorkspace.scrollRight();
                    } else {
                        setTrackDisplayFromService();
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

                setTrackDataById(intent.getLongExtra("id",-1));
                long next = refreshNow();
                queueNextRefresh(next);

            } else if (action.equals(CloudPlaybackService.PLAYBACK_COMPLETE)) {
                setPlaybackState();
                if (getTrackView(queuePos) != null) {
                    getTrackView(queuePos).setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
                }

            } else if (action.equals(CloudPlaybackService.TRACK_ASSOCIATION_CHANGED) ||
                        action.equals(CloudPlaybackService.COMMENTS_LOADED) ||
                        action.equals(Actions.COMMENT_ADDED)) {
                for (int i = 0; i < mTrackWorkspace.getScreenCount(); i++){
                    ((PlayerTrackView) mTrackWorkspace.getScreenAt(i)).handleIdBasedIntent(intent);
                }

                if (action.equals(CloudPlaybackService.TRACK_ASSOCIATION_CHANGED) || action.equals(Actions.COMMENT_ADDED)) {
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
        f.addAction(CloudPlaybackService.TRACK_ASSOCIATION_CHANGED);
        f.addAction(Actions.COMMENT_ADDED);
        registerReceiver(mStatusListener, new IntentFilter(f));

        setTrackDisplayFromService();
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

    private void setTrackData() {
        setTrackDataById(CloudPlaybackService.getCurrentTrackId());
    }

    private void setTrackDataById(long id) {

        invalidateOptionsMenu();
        setPlaybackState();
    }

    private void setTrackDisplayFromService() {

        final PlayQueueManager playQueueManager = CloudPlaybackService.getPlayQueueManager();

        final long queueLength = playQueueManager == null ? 1 :playQueueManager.length();
        final int playPosition = playQueueManager == null ? 0 :playQueueManager.getPosition();

        // setup initial workspace, reusing them if possible
        int workspaceIndex = 0;
        for (int pos = Math.max(0, playPosition - 1); pos < Math.min(playPosition + 2, queueLength); pos++) {
            final PlayerTrackView ptv;
            if (mTrackWorkspace.getScreenCount() > workspaceIndex) {
                ptv = ((PlayerTrackView) mTrackWorkspace.getScreenAt(workspaceIndex));
            } else {
                ptv = new PlayerTrackView(this);
                mTrackWorkspace.addViewAtScreenPosition(ptv, workspaceIndex);
            }

            final boolean priority = pos == playPosition;
            ptv.setOnScreen(priority);

            final long trackId = playQueueManager == null ? CloudPlaybackService.getCurrentTrackId() : playQueueManager.getTrackIdAt(pos);
            final Track track = SoundCloudApplication.MODEL_MANAGER.getTrack(trackId);

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

        final int workspacePos = playPosition > 0 ? 1 : 0;
        if (!mTrackWorkspace.isInitialized()) {
            mTrackWorkspace.setVisibility(View.VISIBLE);
            mTrackWorkspace.setSeparator(R.drawable.track_view_separator);
            mTrackWorkspace.initWorkspace(workspacePos);
        } else if (workspacePos != mTrackWorkspace.getCurrentScreen()) {
            mTrackWorkspace.setCurrentScreenNow(playPosition > 0 ? 1 : 0, false);
        }

        final PlayerTrackView currentTrackView = getCurrentTrackView();
        final CloudPlaybackService service = CloudPlaybackService.getInstance();
        if (currentTrackView != null && (service == null || service.isBuffering())) {
            currentTrackView.onBuffering();
        }

        if (mIsCommenting) toggleCommentMode(0);
        mTransportBar.setNavEnabled(queueLength > 1);
        setTrackData();

    }

    /**
     * Returns the currently displayed track view
     */
    private @Nullable PlayerTrackView getCurrentTrackView() {
        return ((PlayerTrackView) mTrackWorkspace.getScreenAt(mTrackWorkspace.getCurrentScreen()));
    }

    /**
     * Returns the track in the current track display (not necessarily the track that is currently playing)
     */
    private Track getCurrentDisplayedTrack() {
        final PlayerTrackView currentTrackView = getCurrentTrackView();
        return currentTrackView == null ? null : currentTrackView.getTrack();
    }

    private int getCurrentDisplayedTrackPosition() {
        final PlayerTrackView currentTrackView = getCurrentTrackView();
        return currentTrackView == null ? -1 : currentTrackView.getPlayPosition();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getSupportMenuInflater().inflate(R.menu.player, menu);

        final MenuItem likeItem = menu.findItem(R.id.action_bar_like);
        final MenuItem repostItem = menu.findItem(R.id.action_bar_repost);
        final MenuItem commentItem = menu.findItem(R.id.action_bar_comment);
        final MenuItem shareItem = menu.findItem(R.id.action_bar_share);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            menu.removeItem(R.id.action_bar_info);
        }

        Track track = getCurrentDisplayedTrack();
        if (track == null){ // possibly before layout
            track = CloudPlaybackService.getCurrentTrack();
        }

        if (track != null && track.user_like) {
            likeItem.setIcon(R.drawable.ic_like_orange);
        } else {
            likeItem.setIcon(R.drawable.ic_like_white);
        }

        if (track != null && track.user_repost) {
            repostItem.setIcon(R.drawable.ic_repost_orange);
        } else {
            repostItem.setIcon(R.drawable.ic_repost_white);
        }

        if (mIsCommenting){
            commentItem.setIcon(R.drawable.ic_comment_orange);
        } else {
            commentItem.setIcon(R.drawable.ic_comment_white);
        }


        if (track != null && track.isPublic()) {
            shareItem.setEnabled(true);

            ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();

            Intent shareIntent = track.getShareIntent();
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    track.title + (track.user != null ? " by " + track.user.username : "") + " on SoundCloud");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, track.permalink_url);

            shareActionProvider.setShareIntent(shareIntent);
        } else {
            shareItem.setEnabled(false);

            ShareActionProvider shareActionProvider = (ShareActionProvider) shareItem.getActionProvider();
            shareActionProvider.setShareIntent(null);
        }

        return true;
    }
}

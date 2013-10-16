
package com.soundcloud.android.activity;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.dialog.MyPlaylistsDialogFragment;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.player.PlayerTrackPagerAdapter;
import com.soundcloud.android.player.PlayerTrackView;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueue;
import com.soundcloud.android.service.playback.PlayQueueState;
import com.soundcloud.android.service.playback.State;
import com.soundcloud.android.tracking.Media;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.PlayerTrackPager;
import com.soundcloud.android.view.play.TransportBarView;
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

import javax.annotation.CheckForNull;
import java.lang.ref.WeakReference;

public class ScPlayer extends ScActivity implements PlayerTrackPager.OnTrackPageListener, PlayerTrackView.PlayerTrackViewListener {
    public static final int REFRESH_DELAY = 1000;

    private static final String STATE_PAGER_QUEUE_POSITION = "pager_queue_position";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;
    public static final boolean SMOOTH_PROGRESS = Build.VERSION.SDK_INT >= WaveformController.MINIMUM_SMOOTH_PROGRESS_SDK;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mChangeTrackFast;
    private PlayerTrackPager mTrackPager;
    private TransportBarView mTransportBar;
    private @CheckForNull CloudPlaybackService mPlaybackService;
    private int mPendingPlayPosition = -1;
    private PlayerTrackPagerAdapter mTrackPagerAdapter;
    private Track mIntentTrack;

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
        mTrackPager.setPageMargin((int) (5 * getResources().getDisplayMetrics().density));
        mTrackPager.setListener(this);

        mTrackPagerAdapter = new PlayerTrackPagerAdapter();
        mTrackPager.setAdapter(mTrackPagerAdapter);

        mTransportBar = (TransportBarView) findViewById(R.id.transport_bar);
        mTransportBar.setOnPrevListener(mPrevListener);
        mTransportBar.setOnNextListener(mNextListener);
        mTransportBar.setOnPauseListener(mPauseListener);
        mTransportBar.setOnCommentListener(mCommentListener);

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        if (bundle == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    public void onPageDrag() {
        for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
            ptv.onBeingScrolled();
        }
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
    }

    @Override
    public void onPageChanged() {
        for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
            ptv.onScrollComplete();
        }
        if (mPlaybackService.getPlayQueueManager().getPosition() != getCurrentDisplayedTrackPosition() // different track
                && !mHandler.hasMessages(SEND_CURRENT_QUEUE_POSITION) // not already changing
                && (mChangeTrackFast || CloudPlaybackService.getPlaybackState().isSupposedToBePlaying()) // responding to transport click or already playing
                ) {
            sendTrackChangeOnDelay();
        }
        mChangeTrackFast = false;

        mTransportBar.setIsCommenting(mTrackPager.getCurrentItem() == mTrackPagerAdapter.getCommentingPosition());
    }

    private void sendTrackChangeOnDelay() {
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
    }

    @Override
    public long sendSeek(float seekPercent) {
        if (CloudPlaybackService.isSeekable() && mPlaybackService != null) {
            mSeekPos = -1;
            return mPlaybackService.seek(seekPercent, true);
        } else {
            return -1;
        }
    }

    @Override
    public long setSeekMarker(int queuePosition, float seekPercent) {
        if (mPlaybackService != null) {
            if (mPlaybackService.getPlayQueueManager().getPosition() != queuePosition) {
                mPlaybackService.setQueuePosition(queuePosition);
            } else {
                if (CloudPlaybackService.isSeekable()) {
                    // returns where would we be if we had seeked
                    mSeekPos = mPlaybackService.seek(seekPercent, false);
                    return mSeekPos;
                } else {
                    mSeekPos = -1;
                    return mPlaybackService.getProgress();
                }
            }
        }
        return -1;
    }

    public void onNewComment(Comment comment) {
        final PlayerTrackView ptv = getTrackViewById(comment.track_id);
        if (ptv != null){
            ptv.onNewComment(comment);
        }
    }

    @Override
    public void onAddToPlaylist(Track track) {
        if (track != null && isForeground()) {
            MyPlaylistsDialogFragment.from(track).show(getSupportFragmentManager(), "playlist_dialog");
        }
    }

    @Override
    public void onCloseCommentMode() {
        setCommentMode(false, true);
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

    // TODO something else
    public void addNewComment(final Comment comment) {
        setCommentMode(false, true);
        pendingComment = comment;
        safeShowDialog(Consts.Dialogs.DIALOG_ADD_COMMENT);
    }

    private void setCommentMode(boolean isCommenting, boolean animate) {
        if (isCommenting) {
            mTrackPagerAdapter.setCommentingPosition(getCurrentDisplayedTrackPosition(), animate);
        } else {
            mTrackPagerAdapter.clearCommentingPosition(animate);
        }
        mTransportBar.setIsCommenting(isCommenting);
    }

    @Override
    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, mAndroidCloudAPI);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        refreshTrackPager();
    }

    @Override
    protected int getSelectedMenuId() {
        return -1;
    }

    protected void onPlaybackServiceBound(@NotNull CloudPlaybackService service) {
        final PlayQueueState playQueueState = mPlaybackService.getPlayQueueState();
        // this will configure the playlist from the service
        if (!playQueueState.isEmpty()) {
            // everything is fine, configure from service
            refreshTrackPagerFromPlayQueue(playQueueState);
        } else {
                /* service doesn't exist or playqueue is empty and not loading.
                   start it, it will reload queue and broadcast changes */
            startService(new Intent(this, CloudPlaybackService.class));
        }

        if (mPendingPlayPosition != -1) {
            service.setQueuePosition(mPendingPlayPosition);
            mPendingPlayPosition = -1;
        }
        setPlaybackState();
        setBufferingState();
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected) {
            mTrackPagerAdapter.onConnected();
        }
    }

    @Override
    protected void onDestroy() {
        mTrackPagerAdapter.onDestroy();
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        final String action = intent.getAction();

        mIntentTrack = PlayUtils.getTrackFromIntent(intent);
        if (!TextUtils.isEmpty(action)) {
            if (Actions.PLAY.equals(action)) {
                // play from a normal play intent (created by PlayUtils)
                startService(new Intent(CloudPlaybackService.Actions.PLAY_ACTION, intent.getData()).putExtras(intent));

            } else if (Intent.ACTION_VIEW.equals(action)) {
                // Play from a View Intent, this probably came from quicksearch
                if (intent.getData() != null) {
                    //FIXME: DB access on UI thread
                    mIntentTrack = new TrackStorage().getTrack(intent.getData());
                    if (mIntentTrack == null) {
                        mIntentTrack = SoundCloudApplication.MODEL_MANAGER.cache(new Track(UriUtils.getLastSegmentAsLong(intent.getData())));
                    }
                    startService(new Intent(CloudPlaybackService.Actions.PLAY_ACTION).putExtra(Track.EXTRA, mIntentTrack));
                }
            }
        }

        if (mIntentTrack != null) {
            // set whatever track was sent in with the intent on the viewpager to remove transition from old playqueue
            mTrackPagerAdapter.setPlaceholderTrack(mIntentTrack);
            mTrackPagerAdapter.notifyDataSetChanged();
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

        bindService(new Intent(this, CloudPlaybackService.class), osc, 0);
        IntentFilter f = new IntentFilter();
        f.addAction(Broadcasts.PLAYQUEUE_CHANGED);
        f.addAction(Broadcasts.RELATED_LOAD_STATE_CHANGED);
        f.addAction(Broadcasts.PLAYSTATE_CHANGED);
        f.addAction(Broadcasts.META_CHANGED);
        f.addAction(Broadcasts.PLAYBACK_ERROR);
        f.addAction(Broadcasts.TRACK_UNAVAILABLE);
        f.addAction(Broadcasts.STREAM_DIED);
        f.addAction(Broadcasts.PLAYBACK_COMPLETE);
        f.addAction(Broadcasts.BUFFERING);
        f.addAction(Broadcasts.BUFFERING_COMPLETE);
        f.addAction(Broadcasts.COMMENTS_LOADED);
        f.addAction(Broadcasts.SEEKING);
        f.addAction(Broadcasts.SEEK_COMPLETE);
        f.addAction(Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED);
        f.addAction(Playable.ACTION_SOUND_INFO_UPDATED);
        f.addAction(Playable.ACTION_SOUND_INFO_ERROR);
        f.addAction(Playable.COMMENTS_UPDATED);
        f.addAction(Comment.ACTION_CREATE_COMMENT);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(osc);
        mTrackPagerAdapter.onStop();

        mActivityPaused = true;
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        mPlaybackService = null;
    }

    private final View.OnClickListener mCommentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setCommentMode(((CompoundButton) v).isChecked(), true);
        }
    };

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            final CloudPlaybackService playbackService = mPlaybackService;
            final PlayQueue playQueue = CloudPlaybackService.getPlayQueue();
            if (playbackService != null && playQueue != null) {
                if (getCurrentDisplayedTrackPosition() != playQueue.getPosition()) {
                    playbackService.setQueuePosition(getCurrentDisplayedTrackPosition());
                } else {
                    playbackService.togglePlayback();
                }
            } else {
                startService(new Intent(CloudPlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
            }

            setPlaybackState();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mTrackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            if (mPlaybackService != null) {
                final int playPosition = mPlaybackService.getPlayQueueManager().getPosition();
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
                        refreshTrackPager();
                    }

                } else if (CloudPlaybackService.isSeekable()) {
                    mPlaybackService.seek(0, true);

                } else {
                    mPlaybackService.restartTrack();
                }
            } else {
                startService(new Intent(CloudPlaybackService.Actions.PREVIOUS_ACTION));
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mTrackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            final Track currentTrack = CloudPlaybackService.getCurrentTrack();
            if (currentTrack != null) {
                track(Media.fromTrack(currentTrack), Media.Action.Forward);
            }

            if (mPlaybackService != null) {
                final int playPosition = mPlaybackService.getPlayQueueManager().getPosition();
                if (mPlaybackService.getPlayQueueManager().length() > playPosition + 1) {
                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        mChangeTrackFast = true;
                        mTrackPager.next();
                    } else {
                        mPlaybackService.setQueuePosition(playPosition + 1);
                        refreshTrackPager();
                    }
                }
            } else {
                startService(new Intent(CloudPlaybackService.Actions.NEXT_ACTION));
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
        if (mPlaybackService != null){
            final PlayerTrackView ptv = getTrackView(mPlaybackService.getPlayQueueManager().getPosition());
            if (ptv != null) {
                ptv.setProgress(progress, CloudPlaybackService.getLoadingPercent(),
                        SMOOTH_PROGRESS && CloudPlaybackService.getPlaybackState() == State.PLAYING);
            }
        }
        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);

        // return the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        return !CloudPlaybackService.getPlaybackState().isSupposedToBePlaying() ? REFRESH_DELAY : remaining;
    }

    private static final class PlayerHandler extends Handler {
        private WeakReference<ScPlayer> mPlayerRef;

        private PlayerHandler(ScPlayer context) {
            this.mPlayerRef = new WeakReference<ScPlayer>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            final ScPlayer player = mPlayerRef.get();
            if (player == null) {
                return;
            }
            switch (msg.what) {
                case REFRESH:
                    long next = player.refreshNow();
                    player.queueNextRefresh(next);
                    break;
                case SEND_CURRENT_QUEUE_POSITION:
                    if (player.mPlaybackService != null) {
                        player.mPlaybackService.setQueuePosition(player.getCurrentDisplayedTrackPosition());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private final Handler mHandler = new PlayerHandler(this);

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra(CloudPlaybackService.BroadcastExtras.queuePosition, -1);
            String action = intent.getAction();
            if (action.equals(Broadcasts.PLAYQUEUE_CHANGED)) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

                PlayQueueState playQueueState = intent.getParcelableExtra(PlayQueueState.EXTRA);
                if (playQueueState.isEmpty()){
                    // Service has no playlist. Probably came from the widget. Kick them out to home
                    onHomePressed();
                } else {
                    refreshTrackPagerFromPlayQueue(playQueueState);
                }
            } else if (action.equals(Broadcasts.META_CHANGED)) {
                onMetaChanged(queuePos);
            } else if (action.equals(Broadcasts.RELATED_LOAD_STATE_CHANGED)) {
                boolean wasOnEmptyView = getCurrentDisplayedTrackView() == null;

                mTrackPagerAdapter.reloadEmptyView();
                // TODO, this needs a test. Running to demos :?
                if (wasOnEmptyView && getCurrentDisplayedTrackView() != null &&
                        CloudPlaybackService.getPlaybackState().isSupposedToBePlaying()){
                    sendTrackChangeOnDelay();
                }
                setPlaybackState();

            } else if (action.equals(Comment.ACTION_CREATE_COMMENT)) {
                addNewComment(intent.<Comment>getParcelableExtra(Comment.EXTRA));
            } else {

                if (Broadcasts.PLAYBACK_COMPLETE.equals(action) || action.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                    setPlaybackState();
                    final PlayerTrackView trackView = getTrackView(queuePos);
                    if (trackView != null) {
                        if (action.equals(Broadcasts.PLAYBACK_COMPLETE)){
                            trackView.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
                        } else {
                            trackView.handleStatusIntent(intent);
                        }
                    }
                } else {
                    // unhandled here, pass along to trackviews who may be interested
                    for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
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
                refreshTrackPager();
            }
        }

        for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
            if (ptv.getPlayPosition() != queuePos) {
                ptv.getWaveformController().reset(false);
            }
        }
        setPlaybackState();
        long next = refreshNow();
        if (CloudPlaybackService.getPlaybackState().isSupposedToBePlaying()){
            queueNextRefresh(next);
        }
    }

    private void refreshTrackPager() {
        if (mPlaybackService != null){
            refreshTrackPagerFromPlayQueue(mPlaybackService.getPlayQueueState());
        }
    }

    private void refreshTrackPagerFromPlayQueue(PlayQueueState playQueueState) {
        mTrackPagerAdapter.setPlayQueueState(playQueueState);
        mTrackPager.refreshAdapter();
        mTrackPager.setCurrentItem(playQueueState.getPlayPosition());

        setCommentMode(false, false);
        if (mPlaybackService != null){
            setBufferingState();
            setPlaybackState();
        }

    }

    private void setBufferingState() {
        final PlayerTrackView playerTrackView = getTrackViewById(CloudPlaybackService.getCurrentTrackId());
        if (playerTrackView != null) {
            // set buffering state of current track
            playerTrackView.setBufferingState(CloudPlaybackService.isBuffering());
        }
    }

    private int getCurrentDisplayedTrackPosition() {
        return mTrackPager.getCurrentItem();
    }

    private PlayerTrackView getCurrentDisplayedTrackView() {
        return mTrackPagerAdapter.getPlayerTrackViewByPosition(getCurrentDisplayedTrackPosition());
    }

    private @Nullable PlayerTrackView getTrackView(int playPos){
        return mTrackPagerAdapter.getPlayerTrackViewByPosition(playPos);
    }

    private @Nullable PlayerTrackView getTrackViewById(long id) {
        return mTrackPagerAdapter.getPlayerTrackViewById(id);
    }

    private void setPlaybackState() {
        final boolean showPlayState = CloudPlaybackService.getPlaybackState().isSupposedToBePlaying();

        if (showPlayState) {
            long next = refreshNow();
            queueNextRefresh(next);
        }

        mTransportBar.setPlaybackState(showPlayState);

        if (mPlaybackService != null){
            int pos    = mPlaybackService.getPlayQueueManager().getPosition();
            int length = mPlaybackService.getPlayQueueManager().length();
            mTransportBar.setNextEnabled(pos < (length - 1));
        }
    }
}

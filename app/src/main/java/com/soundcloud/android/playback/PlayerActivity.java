
package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.AddCommentDialog;
import com.soundcloud.android.playback.views.PlayableController;
import com.soundcloud.android.playback.views.PlayerTrackDetailsLayout;
import com.soundcloud.android.playback.views.PlayerTrackPager;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.playback.views.TransportBarView;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class PlayerActivity extends ScActivity implements PlayerTrackPager.OnTrackPageListener, PlayerTrackView.PlayerTrackViewListener, PlayableController.AddToPlaylistListener {

    public static final int REFRESH_DELAY = 1000;

    private static final String STATE_PAGER_QUEUE_POSITION = "pager_queue_position";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long mSeekPos = -1;
    private boolean mActivityPaused, mChangeTrackFast;
    private PlayerTrackPager mTrackPager;
    private TransportBarView mTransportBar;
    private @CheckForNull
    PlaybackService mPlaybackService;
    private int mPendingPlayPosition = -1;
    private boolean mIsFirstLoad;

    @Inject
    PlaybackStateProvider mPlaybackStateProvider;
    @Inject
    PlaybackOperations mPlaybackOperations;
    @Inject
    PlayerTrackPagerAdapter mTrackPagerAdapter;
    @Inject
    SoundAssociationOperations mSoundAssocicationOps;

    @NotNull
    private PlayQueueView mPlayQueue = PlayQueueView.EMPTY;
    private PlayerTrackDetailsLayout mTrackDetailsView;
    private PlayableController mPlayableController;

    public interface PlayerError {
        int PLAYBACK_ERROR    = 0;
        int STREAM_ERROR      = 1;
        int TRACK_UNAVAILABLE = 2;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.player_activity);

        setTitle(R.string.title_now_playing);

        new DaggerDependencyInjector().fromAppGraphWithModules(new PlaybackModule()).inject(this);

        mTrackPager = (PlayerTrackPager) findViewById(R.id.track_view);
        mTrackPager.setPageMarginDrawable(R.drawable.track_view_separator);
        mTrackPager.setPageMargin((int) (5 * getResources().getDisplayMetrics().density));
        mTrackPager.setListener(this);

        mTrackPager.setAdapter(mTrackPagerAdapter);

        mTransportBar = (TransportBarView) findViewById(R.id.transport_bar);
        mTransportBar.setOnPrevListener(mPrevListener);
        mTransportBar.setOnNextListener(mNextListener);
        mTransportBar.setOnPauseListener(mPauseListener);
        mTransportBar.setOnCommentListener(mCommentListener);


        // only exists in tablet layouts
        LinearLayout mPlayerInfoLayout = (LinearLayout) findViewById(R.id.player_info_view);
        if (mPlayerInfoLayout != null){
            mTrackDetailsView = (PlayerTrackDetailsLayout) mPlayerInfoLayout.findViewById(R.id.player_track_details);
            mPlayableController = new PlayableController(
                    this, mSoundAssocicationOps, mPlaybackStateProvider);

            mPlayableController.setTitleView((TextView) findViewById(R.id.playable_title))
                    .setUsernameView((TextView) findViewById(R.id.playable_user))
                    .setAvatarView((ImageView) findViewById(R.id.icon), ImageSize.getListItemImageSize(this), R.drawable.avatar_badge)
                    .setStatsView((StatsView) findViewById(R.id.stats), false)
                    .setCreatedAtView((TextView) findViewById(R.id.playable_created_at))
                    .setPrivacyIndicatorView((TextView) findViewById(R.id.playable_private_indicator))
                    .setLikeButton((ToggleButton) findViewById(R.id.toggle_like))
                    .setRepostButton((ToggleButton) findViewById(R.id.toggle_repost))
                    .setAddToPlaylistButton(findViewById(R.id.btn_addToPlaylist), this)
                    .setShareButton((ImageButton) findViewById(R.id.btn_share));

            mPlayableController.startListeningForChanges();

        }

        mIsFirstLoad = bundle == null;

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shouldTrackScreen()) {
            // we track whatever sound gets played first here, and then every subsequent sound through the view pager,
            // to accommodate for lazy loading of sounds
            EventBus.SCREEN_ENTERED.publish(Screen.PLAYER_MAIN.get());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (mPlaybackService != null) {
            Intent intent = mPlaybackOperations.getServiceBasedUpIntent(mPlaybackService);
            if (intent != null) {
                startActivity(intent);
                finish();
                return true;
            }
        }
        return super.onSupportNavigateUp();
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
        if (mPlaybackStateProvider.getPlayPosition() != getCurrentDisplayedTrackPosition() // different track
                && !mHandler.hasMessages(SEND_CURRENT_QUEUE_POSITION) // not already changing
                && (mChangeTrackFast || mPlaybackStateProvider.isSupposedToBePlaying()) // responding to transport click or already playing
                ) {
            sendTrackChangeOnDelay();
            EventBus.SCREEN_ENTERED.publish(Screen.PLAYER_MAIN.get());
        }
        mChangeTrackFast = false;
        mTransportBar.setIsCommenting(mTrackPager.getCurrentItem() == mTrackPagerAdapter.getCommentingPosition());
        updatePlayerInfoPanelFromTrackPager();
    }

    private void sendTrackChangeOnDelay() {
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                mChangeTrackFast ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
    }

    @Override
    public long sendSeek(float seekPercent) {
        if (mPlaybackStateProvider.isSeekable() && mPlaybackService != null) {
            mSeekPos = -1;
            return mPlaybackService.seek(seekPercent, true);
        } else {
            return -1;
        }
    }

    @Override
    public long setSeekMarker(int queuePosition, float seekPercent) {
        if (mPlaybackService != null) {
            if (mPlaybackStateProvider.getPlayPosition() != queuePosition) {
                mPlaybackService.setQueuePosition(queuePosition);
            } else {
                if (mPlaybackStateProvider.isSeekable()) {
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

    @Override
    public void onAddToPlaylist(Track track) {
        if (track != null && isForeground()) {
            AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track, mPlaybackService.getPlayQueueOriginScreen());
            from.show(getSupportFragmentManager(), "playlist_dialog");
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

    public void addNewComment(final Comment comment) {
        setCommentMode(false, true);
        AddCommentDialog.from(comment, mPlaybackService.getPlayQueueOriginScreen()).show(getSupportFragmentManager(), "comment_dialog");
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
        return new ActionBarController(this, mPublicCloudAPI);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final PlayQueueView playQueue = getPlayQueueFromIntent(intent);
        if (playQueue != null && !playQueue.isEmpty()){
            mPlayQueue = playQueue;
            refreshTrackPager();
        }
    }

    protected void onPlaybackServiceBound(@NotNull PlaybackService service) {
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
        if (mPlayableController != null) {
            mPlayableController.stopListeningForChanges();
        }
        super.onDestroy();
    }

    private PlayQueueView getPlayQueueFromIntent(Intent intent) {

        final String action = intent.getAction();
        PlayQueueView playQueue = PlayQueueView.EMPTY;

        if (Intent.ACTION_VIEW.equals(action)) {
            // Play from a View Intent, this probably came from quicksearch
            if (intent.getData() != null) {
                final long id = UriUtils.getLastSegmentAsLong(intent.getData());
                playQueue = new PlayQueueView(id);
                mPlaybackOperations.startPlayback(this, id, Screen.fromIntent(intent, Screen.DEEPLINK));
            }

        } else if (intent.hasExtra(Track.EXTRA_ID)) {
            playQueue = new PlayQueueView(intent.getLongExtra(Track.EXTRA_ID, -1l));

        } else if (intent.getParcelableExtra(Track.EXTRA) != null) {
            final Track track = intent.getParcelableExtra(Track.EXTRA);
            playQueue = new PlayQueueView(Lists.newArrayList(track.getId()), 0);

            if (Actions.PLAY.equals(action)){
                mPlaybackOperations.startPlayback(this, track, Screen.fromIntent(intent, Screen.DEEPLINK));
            }
        }
        return playQueue;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mActivityPaused = false;

        bindService(new Intent(this, PlaybackService.class), osc, 0);
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
        f.addAction(Playable.ACTION_SOUND_INFO_UPDATED);
        f.addAction(Playable.ACTION_SOUND_INFO_ERROR);
        f.addAction(Playable.COMMENT_ADDED);
        f.addAction(Playable.COMMENTS_UPDATED);
        f.addAction(Comment.ACTION_CREATE_COMMENT);
        registerReceiver(mStatusListener, new IntentFilter(f));

        /**
         * NOTE : Do not change this to use any form of {@link android.app.Activity#getChangingConfigurations()}
         * as it is not reliable and this will break the queue behavior by setting the intent queue at the wrong time
          */
        // get the intent playQueue, but only if this is the first load
        mPlayQueue = getInitialPlayQueue(mIsFirstLoad);

        if (!mPlayQueue.isEmpty()) {
            // everything is fine, configure from service
            refreshTrackPager();
        } else {
                /* service doesn't exist or playqueue is empty and not loading.
                   start it, it will reload queue and broadcast changes */
            startService(new Intent(this, PlaybackService.class));
        }
        mIsFirstLoad = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(osc);
        unregisterReceiver(mStatusListener);
        mHandler.removeMessages(REFRESH);
        mTrackPagerAdapter.onStop();

        mActivityPaused = true;
        mPlaybackService = null;
    }

    private void updatePlayerInfoPanelFromTrackPager() {
        final Track track = SoundCloudApplication.sModelManager.getTrack(getCurrentDisplayedTrackId());
        if (track != null) {
            if (mTrackDetailsView != null) {
                if (track.shouldLoadInfo()) {
                    startService(new Intent(PlaybackService.Actions.LOAD_TRACK_INFO).putExtra(Track.EXTRA_ID, track.getId()));
                    mTrackDetailsView.setTrack(track, true);
                } else {
                    mTrackDetailsView.setTrack(track);
                }
            }
            if (mPlayableController != null) {
                mPlayableController.setPlayable(track);
            }
        }
    }

    private final ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            if (obj instanceof LocalBinder) {
                mPlaybackService = (PlaybackService) ((LocalBinder)obj).getService();
                onPlaybackServiceBound(mPlaybackService);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName classname) {
            mPlaybackService = null;
        }
    };

    private final View.OnClickListener mCommentListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setCommentMode(((CompoundButton) v).isChecked(), true);
        }
    };

    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            final PlaybackService playbackService = mPlaybackService;
            if (playbackService != null && mPlayQueue != PlayQueueView.EMPTY) {

                if (!mPlaybackStateProvider.isSupposedToBePlaying()
                        && getCurrentDisplayedTrackPosition() != mPlayQueue.getPosition()) {
                    // play whatever track is currently on the screen
                    playbackService.setQueuePosition(getCurrentDisplayedTrackPosition());
                } else {
                    playbackService.togglePlayback();
                }
            } else {
                startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
            }

            setPlaybackState();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mTrackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            if (mPlaybackService != null) {
                final int playPosition = mPlayQueue.getPosition();
                if (mPlaybackService.getProgress() < 2000 && playPosition > 0) {

                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        mChangeTrackFast = true;
                        mTrackPager.prev();
                    } else {
                        mPlaybackService.setQueuePosition(playPosition - 1);
                        refreshTrackPager();
                    }

                } else if (mPlaybackStateProvider.isSeekable()) {
                    mPlaybackService.seek(0, true);

                } else {
                    mPlaybackService.restartTrack();
                }
            } else {
                startService(new Intent(PlaybackService.Actions.PREVIOUS_ACTION));
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mTrackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            if (mPlaybackService != null) {
                final int playPosition = mPlayQueue.getPosition();
                if (mPlayQueue.size() > playPosition + 1) {
                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        mChangeTrackFast = true;
                        mTrackPager.next();
                    } else {
                        mPlaybackService.setQueuePosition(playPosition + 1);
                        refreshTrackPager();
                    }
                }
            } else {
                startService(new Intent(PlaybackService.Actions.NEXT_ACTION));
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
        long progress = mPlaybackStateProvider.getPlayProgress();
        if (mPlaybackService != null){
            final PlayerTrackView ptv = getTrackView(mPlayQueue.getPosition());
            if (ptv != null) {
                ptv.setProgress(progress, mPlaybackStateProvider.getLoadingPercent(),
                        Consts.SdkSwitches.useSmoothProgress && mPlaybackStateProvider.isPlaying());
            }
        }
        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);

        // return the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        return !mPlaybackStateProvider.isSupposedToBePlaying() ? REFRESH_DELAY : remaining;
    }

    private static final class PlayerHandler extends Handler {

        private WeakReference<PlayerActivity> mPlayerRef;
        private PlayerHandler(PlayerActivity context) {
            this.mPlayerRef = new WeakReference<PlayerActivity>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            final PlayerActivity player = mPlayerRef.get();
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

    /**
     * Gets a playQueue based on either the playqueue in the starting intent or from the service.
     * The decision is based on whether the intent playqueue exists and whether the service has loaded that playqueue
     * already. If not, we show the temporary playqueue and wait for  {@link com.soundcloud.android.playback.service.PlaybackService.Broadcasts#PLAYQUEUE_CHANGED}
     */
    private PlayQueueView getInitialPlayQueue(boolean isFirstLoad) {
        final PlayQueueView intentPlayQueue = isFirstLoad ? getPlayQueueFromIntent(getIntent()) : PlayQueueView.EMPTY;

        final boolean waitingForServiceToLoadQueue = !intentPlayQueue.isEmpty()
                && intentPlayQueue.getCurrentTrackId() != mPlaybackStateProvider.getCurrentTrackId();

        if (waitingForServiceToLoadQueue){
            return intentPlayQueue;
        } else {
            return mPlaybackStateProvider.getPlayQueue();
        }
    }

    private final Handler mHandler = new PlayerHandler(this);

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra(PlaybackService.BroadcastExtras.QUEUE_POSITION, -1);
            String action = intent.getAction();
            if (action.equals(Broadcasts.PLAYQUEUE_CHANGED)) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                mPlayQueue = intent.getParcelableExtra(PlayQueueView.EXTRA);
                if (mPlayQueue.isEmpty()){
                    // Service has no playlist. Probably came from the widget. Kick them out to home
                    onSupportNavigateUp();
                } else {
                    refreshTrackPager();
                }
            } else if (action.equals(Broadcasts.META_CHANGED)) {
                mPlayQueue.setPosition(queuePos);
                onMetaChanged();
            } else if (action.equals(Broadcasts.RELATED_LOAD_STATE_CHANGED)) {
                if (mPlayQueue != PlayQueueView.EMPTY){

                    mPlayQueue = intent.getParcelableExtra(PlayQueueView.EXTRA);
                    mTrackPagerAdapter.setPlayQueueIfChanged(mPlayQueue);
                    mTrackPagerAdapter.reloadEmptyView(PlayerActivity.this);
                    mTrackPagerAdapter.notifyDataSetChanged();

                    boolean wasOnEmptyView = getCurrentDisplayedTrackView() == null;
                    if (wasOnEmptyView && getCurrentDisplayedTrackView() != null &&
                            mPlaybackStateProvider.isSupposedToBePlaying()){
                        sendTrackChangeOnDelay();
                    }
                    setPlaybackState();
                }

            } else if (action.equals(Comment.ACTION_CREATE_COMMENT)) {
                addNewComment(intent.<Comment>getParcelableExtra(Comment.EXTRA));
            } else if (action.equals(Playable.COMMENT_ADDED)) {
                Comment comment = intent.getParcelableExtra(Comment.EXTRA);
                final PlayerTrackView ptv = getTrackViewById(comment.track_id);
                if (ptv != null){
                    ptv.onNewComment(comment);
                }

            } else {
                if (Broadcasts.PLAYBACK_COMPLETE.equals(action) || action.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                    setPlaybackState();
                    final PlayerTrackView trackView = getTrackView(queuePos);
                    if (trackView != null) {
                        if (action.equals(Broadcasts.PLAYBACK_COMPLETE)){
                            trackView.setPlaybackStatus(false, intent.getLongExtra(PlaybackService.BroadcastExtras.POSITION, 0));
                        } else {
                            trackView.handleStatusIntent(intent);
                        }
                    }
                } else {
                    if (Playable.ACTION_SOUND_INFO_UPDATED.equals(action)
                            && intent.getLongExtra(PlaybackService.BroadcastExtras.ID, -1) == getCurrentDisplayedTrackId()){
                        updatePlayerInfoPanelFromTrackPager();
                    }
                    // unhandled here, pass along to trackviews who may be interested
                    for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
                        ptv.handleIdBasedIntent(intent);
                    }
                }
            }

        }
    };

    private void onMetaChanged() {

        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        final int playPosition = getCurrentDisplayedTrackPosition();
        if (playPosition != mPlayQueue.getPosition()) {
            if (playPosition != -1
                    && mPlayQueue.getPosition() == playPosition + 1
                    && !mTrackPager.isScrolling()) {
                // auto advance
                mTrackPager.next();
            } else {
                refreshTrackPager();
            }
        }

        for (PlayerTrackView ptv : mTrackPagerAdapter.getPlayerTrackViews()) {
            if (ptv.getPlayPosition() != mPlayQueue.getPosition()) {
                ptv.getWaveformController().reset(false);
            }
        }
        setPlaybackState();
        long next = refreshNow();
        if (mPlaybackStateProvider.isSupposedToBePlaying()) {
            queueNextRefresh(next);
        }
    }

    private void refreshTrackPager() {
        if (mTrackPagerAdapter.setPlayQueueIfChanged(mPlayQueue)) {
            mTrackPager.refreshAdapter();
        }
        mTrackPager.setCurrentItem(mPlayQueue.getPosition());

        setCommentMode(false, false);
        setBufferingState();
        setPlaybackState();
        updatePlayerInfoPanelFromTrackPager();
    }

    private void setBufferingState() {
        final PlayerTrackView playerTrackView = getTrackViewById(mPlaybackStateProvider.getCurrentTrackId());
        if (playerTrackView != null) {
            // set buffering state of current track
            playerTrackView.setBufferingState(mPlaybackStateProvider.isBuffering());
        }
    }

    private void setPlaybackState() {
        final boolean showPlayState = mPlaybackStateProvider.isSupposedToBePlaying();
        if (showPlayState) {
            long next = refreshNow();
            queueNextRefresh(next);
        }
        mTransportBar.setPlaybackState(showPlayState);
        mTransportBar.setNextEnabled(!mPlayQueue.isLastTrack());
    }

    private int getCurrentDisplayedTrackPosition() {
        return mTrackPager.getCurrentItem();
    }

    private PlayerTrackView getCurrentDisplayedTrackView() {
        return mTrackPagerAdapter.getPlayerTrackViewByPosition(getCurrentDisplayedTrackPosition());
    }

    private long getCurrentDisplayedTrackId() {
        final int currentDisplayedTrackPosition = getCurrentDisplayedTrackPosition();
        if (currentDisplayedTrackPosition >= 0 && currentDisplayedTrackPosition < mPlayQueue.size()){
            return mPlayQueue.getTrackIdAt(currentDisplayedTrackPosition);
        } else {
            return -1L;
        }
    }

    private @Nullable
    PlayerTrackView getTrackView(int playPos){
        return mTrackPagerAdapter.getPlayerTrackViewByPosition(playPos);
    }

    private @Nullable
    PlayerTrackView getTrackViewById(long id) {
        return mTrackPagerAdapter.getPlayerTrackViewById(id);
    }
}

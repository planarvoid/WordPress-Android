
package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.google.common.collect.Lists;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.api.legacy.model.Comment;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.AddCommentDialog;
import com.soundcloud.android.playback.views.LegacyPlayerTrackView;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.playback.views.PlayerTrackDetailsLayout;
import com.soundcloud.android.playback.views.PlayerTrackPager;
import com.soundcloud.android.playback.views.TransportBarView;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.service.LocalBinder;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class PlayerActivity extends ScActivity implements PlayerTrackPager.OnTrackPageListener, LegacyPlayerTrackView.PlayerTrackViewListener, EngagementsController.AddToPlaylistListener {

    public static final int REFRESH_DELAY = 1000;

    private static final String STATE_PAGER_QUEUE_POSITION = "pager_queue_position";
    private static final int REFRESH = 1;
    private static final int SEND_CURRENT_QUEUE_POSITION = 2;
    private static final long TRACK_SWIPE_UPDATE_DELAY = 1000;
    private static final long TRACK_NAV_DELAY = 500;

    private long seekPos = -1;
    private boolean activityPaused;
    private boolean transportBarTrackChange;
    private PlayerTrackPager trackPager;
    private TransportBarView transportBar;
    private @CheckForNull
    PlaybackService playbackService;
    private int pendingPlayPosition = -1;
    private boolean isFirstLoad;
    private boolean isTabletLandscapeLayout;

    @Inject PlayQueueManager playQueueManager;
    @Inject PlaybackStateProvider playbackStateProvider;
    @Inject PlaybackOperations playbackOperations;
    @Inject CommentingPlayerPagerAdapter trackPagerAdapter;
    @Inject SoundAssociationOperations soundAssocicationOps;
    @Inject EngagementsController engagementsController;

    @NotNull
    private PlayQueueView playQueue = PlayQueueView.EMPTY;
    private PlayerTrackDetailsLayout trackDetailsView;
    private PlayablePresenter playablePresenter;
    private View trackInfoBar;

    public interface PlayerError {
        int PLAYBACK_ERROR    = 0;
        int STREAM_ERROR      = 1;
        int TRACK_UNAVAILABLE = 2;
    }

    public PlayerActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.player_activity);

        setTitle(R.string.title_now_playing);

        trackPager = (PlayerTrackPager) findViewById(R.id.track_view);
        trackPager.setPageMarginDrawable(R.drawable.track_view_separator);
        trackPager.setPageMargin((int) (5 * getResources().getDisplayMetrics().density));
        trackPager.setListener(this);

        trackPager.setAdapter(trackPagerAdapter);

        transportBar = (TransportBarView) findViewById(R.id.transport_bar);
        transportBar.setOnPrevListener(mPrevListener);
        transportBar.setOnNextListener(mNextListener);
        transportBar.setOnPauseListener(mPauseListener);
        transportBar.setOnCommentListener(mCommentListener);

        LinearLayout playerInfoLayout = (LinearLayout) findViewById(R.id.player_info_view);
        isTabletLandscapeLayout = playerInfoLayout != null;
        if (isTabletLandscapeLayout) {
            setupFixedPlayableInfo(playerInfoLayout);
        }

        isFirstLoad = bundle == null;

        // this is to make sure keyboard is hidden after commenting
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void setupFixedPlayableInfo(LinearLayout playerInfoLayout) {
        trackInfoBar = playerInfoLayout.findViewById(R.id.playable_bar);
        trackDetailsView = (PlayerTrackDetailsLayout) playerInfoLayout.findViewById(R.id.player_track_details);
        playablePresenter = new PlayablePresenter(this);

        playablePresenter.setTitleView((TextView) findViewById(R.id.playable_title))
                .setUsernameView((TextView) findViewById(R.id.playable_user))
                .setAvatarView((ImageView) findViewById(R.id.icon), ApiImageSize.getListItemImageSize(this))
                .setStatsView((StatsView) findViewById(R.id.stats), false)
                .setCreatedAtView((TextView) findViewById(R.id.playable_created_at))
                .setPrivacyIndicatorView((TextView) findViewById(R.id.playable_private_indicator));

        engagementsController.bindView(playerInfoLayout, playQueueManager);
        engagementsController.setAddToPlaylistListener(this);
        engagementsController.startListeningForChanges();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shouldTrackScreen()) {
            // we track whatever sound gets played first here, and then every subsequent sound through the view pager,
            // to accommodate for lazy loading of sounds
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYER_MAIN.get());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (playbackService != null) {
            Intent intent = playbackOperations.getServiceBasedUpIntent();
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
        for (LegacyPlayerTrackView ptv : trackPagerAdapter.getPlayerTrackViews()) {
            ptv.onBeingScrolled();
        }
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
    }

    @Override
    public void onPageChanged() {
        for (LegacyPlayerTrackView ptv : trackPagerAdapter.getPlayerTrackViews()) {
            ptv.onScrollComplete();
        }

        if (playQueueManager.getCurrentPosition() != getCurrentDisplayedTrackPosition() // different track
                && !mHandler.hasMessages(SEND_CURRENT_QUEUE_POSITION) // not already changing
                && (transportBarTrackChange || playbackStateProvider.isSupposedToBePlaying()) // responding to transport click or already playing
                ) {
            sendTrackChangeOnDelay();
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYER_MAIN.get());
            trackPlayControlSwipe();
        }

        transportBarTrackChange = false;
        transportBar.setIsCommenting(trackPager.getCurrentItem() == trackPagerAdapter.getCommentingPosition());
        updatePlayerInfoPanelFromTrackPager();
    }

    private void trackPlayControlSwipe() {
        if (!transportBarTrackChange) {
            if (getCurrentDisplayedTrackPosition() > playQueueManager.getCurrentPosition()) {
                eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.playerSwipeSkip());
            }
            if (getCurrentDisplayedTrackPosition() < playQueueManager.getCurrentPosition()) {
                eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.playerSwipePrevious());
            }
        }
    }

    private void sendTrackChangeOnDelay() {
        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(SEND_CURRENT_QUEUE_POSITION),
                transportBarTrackChange ? TRACK_NAV_DELAY : TRACK_SWIPE_UPDATE_DELAY);
    }

    @Override
    public long sendSeek(float seekPercent) {
        if (playbackStateProvider.isSeekable() && playbackService != null) {
            seekPos = -1;
            return playbackService.seek(seekPercent, true);
        } else {
            return -1;
        }
    }

    @Override
    public long setSeekMarker(int queuePosition, float seekPercent) {
        if (playbackService != null) {
            if (playQueueManager.getCurrentPosition() != queuePosition) {
                playbackOperations.setPlayQueuePosition(queuePosition);
            } else {
                if (playbackStateProvider.isSeekable()) {
                    // returns where would we be if we had seeked
                    seekPos = playbackService.seek(seekPercent, false);
                    return seekPos;
                } else {
                    seekPos = -1;
                    return playbackService.getProgress();
                }
            }
        }
        return -1;
    }

    @Override
    public void onAddToPlaylist(PublicApiTrack track) {
        if (playbackService != null && track != null && isForeground()) {
            AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track, playQueueManager.getScreenTag());
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
            pendingPlayPosition = position;
        }
        super.onRestoreInstanceState(state);
    }

    public void addNewComment(final Comment comment) {
        setCommentMode(false, true);
        AddCommentDialog.from(comment, playQueueManager.getScreenTag()).show(getSupportFragmentManager(), "comment_dialog");
    }

    private void setCommentMode(boolean isCommenting, boolean animate) {
        if (isCommenting) {
            trackPagerAdapter.setCommentingPosition(getCurrentDisplayedTrackPosition(), animate);
        } else {
            trackPagerAdapter.clearCommentingPosition(animate);
        }
        transportBar.setIsCommenting(isCommenting);
    }

    @Override
    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, eventBus);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final PlayQueueView playQueue = getPlayQueueFromIntent(intent);
        if (playQueue != null && !playQueue.isEmpty()){
            this.playQueue = playQueue;
            refreshTrackPager();
        }
    }

    protected void onPlaybackServiceBound() {
        if (pendingPlayPosition != -1) {
            playbackOperations.setPlayQueuePosition(pendingPlayPosition);
            pendingPlayPosition = -1;
        }
        setPlaybackState();
        setBufferingState();
    }

    @Override
    protected void onDataConnectionChanged(boolean isConnected) {
        super.onDataConnectionChanged(isConnected);
        if (isConnected) {
            trackPagerAdapter.onConnected();
        }
    }

    @Override
    protected void onDestroy() {
        trackPagerAdapter.onDestroy();
        if (engagementsController != null) {
            engagementsController.stopListeningForChanges();
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
                playbackOperations.startPlaybackWithRecommendations(id, Screen.fromIntent(intent, Screen.DEEPLINK));
            }

        } else if (intent.hasExtra(PublicApiTrack.EXTRA_ID)) {
            playQueue = new PlayQueueView(intent.getLongExtra(PublicApiTrack.EXTRA_ID, -1l));

        } else if (intent.getParcelableExtra(PublicApiTrack.EXTRA) != null) {
            final PublicApiTrack track = intent.getParcelableExtra(PublicApiTrack.EXTRA);
            playQueue = new PlayQueueView(Lists.newArrayList(track.getId()), 0);

            if (Actions.PLAY.equals(action)){
                playbackOperations.startPlaybackWithRecommendations(track, Screen.fromIntent(intent, Screen.DEEPLINK));
            }
        }
        return playQueue;
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityPaused = false;

        bindService(new Intent(this, PlaybackService.class), osc, 0);
        IntentFilter f = new IntentFilter();
        f.addAction(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION);
        f.addAction(PlayQueueManager.RELATED_LOAD_STATE_CHANGED_ACTION);
        f.addAction(Broadcasts.PLAYSTATE_CHANGED);
        f.addAction(Broadcasts.META_CHANGED);
        f.addAction(Playable.COMMENT_ADDED);
        f.addAction(Playable.COMMENTS_UPDATED);
        f.addAction(Comment.ACTION_CREATE_COMMENT);
        registerReceiver(mStatusListener, new IntentFilter(f));

        /**
         * NOTE : Do not change this to use any form of {@link android.app.Activity#getChangingConfigurations()}
         * as it is not reliable and this will break the queue behavior by setting the intent queue at the wrong time
          */
        // get the intent playQueue, but only if this is the first load
        playQueue = getInitialPlayQueue(isFirstLoad);

        if (!playQueue.isEmpty()) {
            // everything is fine, configure from service
            refreshTrackPager();
        } else {
                /* service doesn't exist or playqueue is empty and not loading.
                   start it, it will reload queue and broadcast changes */
            startService(new Intent(this, PlaybackService.class));
        }
        isFirstLoad = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(osc);
        unregisterReceiver(mStatusListener);
        mHandler.removeMessages(REFRESH);
        trackPagerAdapter.onStop();

        activityPaused = true;
        playbackService = null;
    }

    private void updatePlayerInfoPanelFromTrackPager() {
        if (isTabletLandscapeLayout) {
            final long currentId = getCurrentDisplayedTrackId();
            final Observable<PublicApiTrack> trackObservable = trackPagerAdapter.getTrackObservable(currentId);

            if (trackObservable != null) {
                trackObservable.subscribe(playerInfoPanelSubscriber);
            }
        }
    }

    private DefaultSubscriber<PublicApiTrack> playerInfoPanelSubscriber = new DefaultSubscriber<PublicApiTrack>() {
        @Override
        public void onNext(final PublicApiTrack track) {
            playablePresenter.setPlayable(track);
            engagementsController.setPlayable(track);
            trackDetailsView.setTrack(track);
            trackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(PlayerActivity.this, track);
                }
            });
        }
    };

    private final ServiceConnection osc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            if (obj instanceof LocalBinder) {
                playbackService = (PlaybackService) ((LocalBinder)obj).getService();
                onPlaybackServiceBound();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName classname) {
            playbackService = null;
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

            final PlaybackService playbackService = PlayerActivity.this.playbackService;
            if (playbackService != null && playQueue != PlayQueueView.EMPTY) {

                if (!playbackStateProvider.isSupposedToBePlaying()
                        && getCurrentDisplayedTrackPosition() != playQueue.getPosition()) {
                    // play whatever track is currently on the screen
                    playbackOperations.setPlayQueuePosition(getCurrentDisplayedTrackPosition());
                    playbackOperations.playCurrent();
                } else {
                    playbackService.togglePlayback();
                }
            } else {
                startService(new Intent(PlaybackService.Actions.TOGGLEPLAYBACK_ACTION));
            }

            eventBus.publish(EventQueue.PLAY_CONTROL, playbackStateProvider.isSupposedToBePlaying()
                    ? PlayControlEvent.playerClickPlay()
                    : PlayControlEvent.playerClickPause());

            setPlaybackState();
        }
    };

    private final View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (trackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.playerClickPrevious());

            if (playbackService != null) {
                final int playPosition = playQueue.getPosition();
                if (playbackService.getProgress() < 2000 && playPosition > 0) {

                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        transportBarTrackChange = true;
                        trackPager.prev();
                    } else {
                        playbackOperations.setPlayQueuePosition(playPosition - 1);
                        refreshTrackPager();
                    }

                } else if (playbackStateProvider.isSeekable()) {
                    playbackService.seek(0, true);

                } else {
                    playbackService.restartTrack();
                }
            } else {
                playbackOperations.previousTrack();
            }
        }
    };

    private final View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (trackPager.isScrolling()) return;

            mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);

            eventBus.publish(EventQueue.PLAY_CONTROL, PlayControlEvent.playerClickSkip());

            if (playbackService != null) {
                final int playPosition = playQueue.getPosition();
                if (playQueue.size() > playPosition + 1) {
                    if (getCurrentDisplayedTrackPosition() == playPosition) {
                        transportBarTrackChange = true;
                        trackPager.next();
                    } else {
                        playbackOperations.setPlayQueuePosition(playPosition + 1);
                        refreshTrackPager();
                    }
                }
            } else {
                playbackOperations.nextTrack();
            }

        }
    };

    private void queueNextRefresh(long delay) {
        if (!activityPaused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        long progress = playbackStateProvider.getPlayProgress();
        if (playbackService != null){
            final LegacyPlayerTrackView ptv = getTrackView(playQueue.getPosition());
            if (ptv != null) {
                ptv.setProgress(progress, playbackStateProvider.getLoadingPercent());
            }
        }
        long remaining = REFRESH_DELAY - (progress % REFRESH_DELAY);

        // return the number of milliseconds until the next full second, so
        // the counter can be updated at just the right time
        return !playbackStateProvider.isSupposedToBePlaying() ? REFRESH_DELAY : remaining;
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
                    if (player.playbackService != null) {
                        player.playbackOperations.setPlayQueuePosition(player.getCurrentDisplayedTrackPosition());
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
     * already. If not, we show the temporary playqueue and wait for  {@link com.soundcloud.android.playback.service.PlayQueueManager#PLAYQUEUE_CHANGED_ACTION}
     */
    private PlayQueueView getInitialPlayQueue(boolean isFirstLoad) {
        final PlayQueueView intentPlayQueue = isFirstLoad ? getPlayQueueFromIntent(getIntent()) : PlayQueueView.EMPTY;

        final boolean waitingForServiceToLoadQueue = !intentPlayQueue.isEmpty()
                && intentPlayQueue.getCurrentTrackId() != playQueueManager.getCurrentTrackId();

        if (waitingForServiceToLoadQueue){
            return intentPlayQueue;
        } else {
            return playQueueManager.getPlayQueueView();
        }
    }

    private final Handler mHandler = new PlayerHandler(this);

    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int queuePos = intent.getIntExtra(PlaybackService.BroadcastExtras.QUEUE_POSITION, -1);
            String action = intent.getAction();
            if (action.equals(PlayQueueManager.PLAYQUEUE_CHANGED_ACTION)) {
                mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
                playQueue = intent.getParcelableExtra(PlayQueueView.EXTRA);
                if (playQueue.isEmpty()){
                    // Service has no playlist. Probably came from the widget. Kick them out to home
                    onSupportNavigateUp();
                } else {
                    refreshTrackPager();
                }
            } else if (action.equals(Broadcasts.META_CHANGED)) {
                playQueue.setPosition(queuePos);
                onMetaChanged();
            } else if (action.equals(PlayQueueManager.RELATED_LOAD_STATE_CHANGED_ACTION)) {
                if (playQueue != PlayQueueView.EMPTY){

                    playQueue = intent.getParcelableExtra(PlayQueueView.EXTRA);
                    trackPagerAdapter.setPlayQueue(playQueue);
                    trackPagerAdapter.notifyDataSetChanged();

                    boolean wasOnEmptyView = getCurrentDisplayedTrackView() == null;
                    if (wasOnEmptyView && getCurrentDisplayedTrackView() != null &&
                            playbackStateProvider.isSupposedToBePlaying()){
                        sendTrackChangeOnDelay();
                    }
                    setPlaybackState();
                }

            } else if (action.equals(Comment.ACTION_CREATE_COMMENT)) {
                addNewComment(intent.<Comment>getParcelableExtra(Comment.EXTRA));
            } else if (action.equals(Playable.COMMENT_ADDED)) {
                Comment comment = intent.getParcelableExtra(Comment.EXTRA);
                final LegacyPlayerTrackView ptv = getTrackViewById(comment.track_id);
                if (ptv != null){
                    ptv.onNewComment(comment);
                }

            } else {
                if (action.equals(Broadcasts.PLAYSTATE_CHANGED)) {
                    setPlaybackState();
                    final LegacyPlayerTrackView trackView = getTrackView(queuePos);
                    if (trackView != null) {
                        trackView.handleStatusIntent(intent);
                    }
                } else {
                    // unhandled here, pass along to trackviews who may be interested
                    for (LegacyPlayerTrackView ptv : trackPagerAdapter.getPlayerTrackViews()) {
                        ptv.handleIdBasedIntent(intent);
                    }
                }
            }

        }
    };

    private void onMetaChanged() {

        mHandler.removeMessages(SEND_CURRENT_QUEUE_POSITION);
        final int playPosition = getCurrentDisplayedTrackPosition();
        if (playPosition != playQueue.getPosition()) {
            if (playPosition != -1
                    && playQueue.getPosition() == playPosition + 1
                    && !trackPager.isScrolling()) {
                // auto advance
                trackPager.next();
            } else {
                refreshTrackPager();
            }
        }

        for (LegacyPlayerTrackView ptv : trackPagerAdapter.getPlayerTrackViews()) {
            if (ptv.getPlayPosition() != playQueue.getPosition()) {
                ptv.getWaveformController().reset(false);
            }
        }
        setPlaybackState();
        long next = refreshNow();
        if (playbackStateProvider.isSupposedToBePlaying()) {
            queueNextRefresh(next);
        }
    }

    private void refreshTrackPager() {
        trackPagerAdapter.setPlayQueue(playQueue);
        trackPager.refreshAdapter();
        trackPager.setCurrentItem(playQueue.getPosition(), false);

        setCommentMode(false, false);
        setBufferingState();
        setPlaybackState();
        updatePlayerInfoPanelFromTrackPager();
    }

    private void setBufferingState() {
        final LegacyPlayerTrackView playerTrackView = getTrackViewById(playbackStateProvider.getCurrentTrackId());
        if (playerTrackView != null) {
            // set buffering state of current track
            playerTrackView.setBufferingState(playbackStateProvider.isBuffering());
        }
    }

    private void setPlaybackState() {
        final boolean showPlayState = playbackStateProvider.isSupposedToBePlaying();
        long next = refreshNow();
        if (showPlayState) {
            queueNextRefresh(next);
        }
        transportBar.setPlaybackState(showPlayState);
        transportBar.setNextEnabled(!playQueue.isLastTrack());
    }

    private int getCurrentDisplayedTrackPosition() {
        return trackPager.getCurrentItem();
    }

    private LegacyPlayerTrackView getCurrentDisplayedTrackView() {
        return trackPagerAdapter.getPlayerTrackViewByPosition(getCurrentDisplayedTrackPosition());
    }

    private long getCurrentDisplayedTrackId() {
        final int currentDisplayedTrackPosition = getCurrentDisplayedTrackPosition();
        if (currentDisplayedTrackPosition >= 0 && currentDisplayedTrackPosition < playQueue.size()){
            return playQueue.getTrackIdAt(currentDisplayedTrackPosition);
        } else {
            return -1L;
        }
    }

    private @Nullable
    LegacyPlayerTrackView getTrackView(int playPos){
        return trackPagerAdapter.getPlayerTrackViewByPosition(playPos);
    }

    private @Nullable
    LegacyPlayerTrackView getTrackViewById(long id) {
        return trackPagerAdapter.getPlayerTrackViewById(id);
    }
}

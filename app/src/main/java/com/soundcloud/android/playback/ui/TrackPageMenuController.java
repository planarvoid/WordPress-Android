package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.comments.AddCommentDialogFragment;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.share.ShareOperations;
import com.soundcloud.android.stations.StartStationHandler;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class TrackPageMenuController
        implements ProgressAware, ScrubController.OnScrubListener, PopupMenuWrapper.PopupMenuWrapperListener {

    private static final String INFO_DIALOG_TAG = "info_dialog";
    private static final String ADD_COMMENT_DIALOG_TAG = "add_comment_dialog";

    private final FragmentActivity activity;
    private final PopupMenuWrapper popupMenuWrapper;
    private final PlayQueueManager playQueueManager;
    private final RepostOperations repostOperations;
    private final StartStationHandler stationHandler;
    private final EventBus eventBus;
    private final ShareOperations shareOperations;
    private final String commentAtUnformatted;

    private PlayerTrackState track = PlayerTrackState.EMPTY;
    private PlaybackProgress lastProgress = PlaybackProgress.empty();

    private long commentPosition;

    private TrackPageMenuController(PlayQueueManager playQueueManager,
                                    RepostOperations repostOperations,
                                    FragmentActivity context,
                                    PopupMenuWrapper popupMenuWrapper,
                                    StartStationHandler stationHandler,
                                    EventBus eventBus,
                                    ShareOperations shareOperations) {
        this.playQueueManager = playQueueManager;
        this.repostOperations = repostOperations;
        this.activity = context;
        this.popupMenuWrapper = popupMenuWrapper;
        this.stationHandler = stationHandler;
        this.eventBus = eventBus;
        this.shareOperations = shareOperations;
        this.commentAtUnformatted = activity.getString(R.string.comment_at_time);
        setupMenu();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        lastProgress = progress;
        updateCommentPosition(lastProgress.getPosition());
    }

    @Override
    public void clearProgress() {
        setProgress(PlaybackProgress.empty());
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        // We only care about position
    }

    @Override
    public void displayScrubPosition(float actualPosition, float boundedPosition) {
        updateCommentPosition((long) (boundedPosition * track.getFullDuration()));
    }

    private void updateCommentPosition(long position) {
        commentPosition = position;
        if (track.isCommentable()) {
            final String timestamp = ScTextUtils.formatTimestamp(position, TimeUnit.MILLISECONDS);
            popupMenuWrapper.setItemText(R.id.comment, String.format(commentAtUnformatted, timestamp));
        }
    }

    private void setupMenu() {
        popupMenuWrapper.inflate(R.menu.player_page_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
    }

    private void initStationsOption() {
        popupMenuWrapper.findItem(R.id.start_station).setTitle(activity.getText(R.string.stations_open_station));
        popupMenuWrapper.setItemEnabled(R.id.start_station, IOUtils.isConnected(activity));
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.share:
                handleShare(context);
                return true;
            case R.id.repost:
                handleRepostToggle(true, track.getUrn());
                return true;
            case R.id.unpost:
                handleRepostToggle(false, track.getUrn());
                return true;
            case R.id.info:
                TrackInfoFragment.create(track.getUrn()).show(activity.getSupportFragmentManager(), INFO_DIALOG_TAG);
                return true;
            case R.id.comment:
                handleComment();
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog(track);
                return true;
            case R.id.start_station:
                handleStartStation(context);
                return true;
            default:
                return false;
        }
    }

    private void handleStartStation(Context context) {
        stationHandler.startStationFromPlayer(context, track.getTrackUrn(), track.isBlocked());
    }

    void handleShare(Context context) {
        Urn trackUrn = track.getUrn();
        shareOperations.share(context,
                              track.getSource(),
                              getContextMetadata(trackUrn),
                              playQueueManager.getCurrentPromotedSourceInfo(trackUrn));
    }

    private void handleComment() {
        final AddCommentDialogFragment fragment = AddCommentDialogFragment.create(track.getSource(),
                                                                                  commentPosition,
                                                                                  playQueueManager.getScreenTag());
        fragment.show(activity.getFragmentManager(), ADD_COMMENT_DIALOG_TAG);
    }

    private void handleRepostToggle(boolean wasReposted, Urn trackUrn) {
        fireAndForget(repostOperations.toggleRepost(trackUrn, wasReposted));

        eventBus.publish(EventQueue.TRACKING,
                         UIEvent.fromToggleRepost(wasReposted,
                                                  trackUrn,
                                                  getContextMetadata(trackUrn),
                                                  playQueueManager.getCurrentPromotedSourceInfo(trackUrn),
                                                  EntityMetadata.from(track)));
    }

    private EventContextMetadata getContextMetadata(Urn trackUrn) {
        return EventContextMetadata.builder()
                                   .contextScreen(playQueueManager.getScreenTag())
                                   .pageName(Screen.PLAYER_MAIN.get())
                                   .pageUrn(trackUrn)
                                   .trackSourceInfo(playQueueManager.getCurrentTrackSourceInfo())
                                   .isFromOverflow(true)
                                   .build();
    }

    private void showAddToPlaylistDialog(PlayerTrackState track) {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(
                track.getUrn(), track.getTitle(), ScreenElement.PLAYER.get(), playQueueManager.getScreenTag());
        from.show(activity.getFragmentManager());
    }

    public void setTrack(PlayerTrackState track) {
        this.track = track;
        setIsUserRepost(track.isUserRepost());
        setMenuPrivacy(track.isPrivate());
        setCommentsVisibility(track.isCommentable());
    }

    private void setCommentsVisibility(boolean isCommentable) {
        popupMenuWrapper.setItemVisible(R.id.comment, isCommentable);
        updateCommentPosition(lastProgress.getPosition());
    }

    void setIsUserRepost(boolean isUserRepost) {
        popupMenuWrapper.setItemVisible(R.id.unpost, isUserRepost);
        popupMenuWrapper.setItemVisible(R.id.repost, !isUserRepost);
    }

    private void setMenuPrivacy(boolean isPrivate) {
        popupMenuWrapper.setItemEnabled(R.id.unpost, !isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.repost, !isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.share, !isPrivate);
    }

    public void show() {
        if (track != PlayerTrackState.EMPTY) {
            initStationsOption();
            popupMenuWrapper.show();
        }
    }

    public void dismiss() {
        popupMenuWrapper.dismiss();
    }

    @Override
    public void onDismiss() {
        // no-op
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;
        private final RepostOperations repostOperations;
        private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
        private final EventBus eventBus;
        private final StartStationHandler startStationHandler;
        private final ShareOperations shareOperations;

        @Inject
        Factory(PlayQueueManager playQueueManager,
                RepostOperations repostOperations,
                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                StartStationHandler startStationHandler,
                EventBus eventBus,
                ShareOperations shareOperations) {
            this.playQueueManager = playQueueManager;
            this.repostOperations = repostOperations;
            this.popupMenuWrapperFactory = popupMenuWrapperFactory;
            this.startStationHandler = startStationHandler;
            this.eventBus = eventBus;
            this.shareOperations = shareOperations;
        }

        TrackPageMenuController create(View anchorView) {
            return new TrackPageMenuController(
                    playQueueManager,
                    repostOperations,
                    getFragmentActivity(anchorView),
                    popupMenuWrapperFactory.build(getFragmentActivity(anchorView), anchorView),
                    startStationHandler,
                    eventBus,
                    shareOperations);
        }
    }

}

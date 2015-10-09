package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.associations.RepostOperations;
import com.soundcloud.android.comments.AddCommentDialogFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class TrackPageMenuController implements ProgressAware, ScrubController.OnScrubListener, PopupMenuWrapper.PopupMenuWrapperListener {

    public static final String INFO_DIALOG_TAG = "info_dialog";
    public static final String ADD_COMMENT_DIALOG_TAG = "add_comment_dialog";

    public static final String SHARE_TYPE = "text/plain";

    private final FragmentActivity activity;
    private final PopupMenuWrapper popupMenuWrapper;
    private final PlayQueueManager playQueueManager;
    private final RepostOperations repostOperations;
    private final EventBus eventBus;
    private final String commentAtUnformatted;

    private PlayerTrackState track = PlayerTrackState.EMPTY;
    private PlaybackProgress lastProgress = PlaybackProgress.empty();

    private long commentPosition;

    private TrackPageMenuController(PlayQueueManager playQueueManager,
                                    RepostOperations repostOperations,
                                    FragmentActivity context,
                                    PopupMenuWrapper popupMenuWrapper,
                                    EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.repostOperations = repostOperations;
        this.activity = context;
        this.popupMenuWrapper = popupMenuWrapper;
        this.eventBus = eventBus;
        this.commentAtUnformatted = activity.getString(R.string.comment_at);
        setupMenu();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        lastProgress = progress;
        updateCommentPosition(lastProgress.getPosition());
    }

    @Override
    public void scrubStateChanged(int newScrubState) {
        // We only care about position
    }

    @Override
    public void displayScrubPosition(float scrubPosition) {
        if (lastProgress.isEmpty()) {
            updateCommentPosition((long) (scrubPosition * track.getDuration()));
        } else {
            updateCommentPosition((long) (scrubPosition * lastProgress.getDuration()));
        }
    }

    private void updateCommentPosition(long position) {
        commentPosition = position;
        final String timestamp = ScTextUtils.formatTimestamp(position, TimeUnit.MILLISECONDS);
        popupMenuWrapper.setItemText(R.id.comment, String.format(commentAtUnformatted, timestamp));
    }

    private void setupMenu() {
        popupMenuWrapper.inflate(R.menu.player_page_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem, Context context) {
        switch (menuItem.getItemId()) {
            case R.id.share:
                handleShare(track);
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
            default:
                return false;
        }
    }

    private void handleComment() {
        final AddCommentDialogFragment fragment = AddCommentDialogFragment.create(track.getSource(), commentPosition, playQueueManager.getScreenTag());
        fragment.show(activity.getFragmentManager(), ADD_COMMENT_DIALOG_TAG);
    }

    private void handleShare(PlayerTrackState track) {
        if (!track.isPrivate()) {
            activity.startActivity(buildShareIntent(track));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShare(playQueueManager.getScreenTag(), track.getUrn(), PlayableMetadata.from(track)));
        }
    }

    private void handleRepostToggle(boolean wasReposted, Urn trackUrn) {
        fireAndForget(repostOperations.toggleRepost(trackUrn, wasReposted));

        eventBus.publish(EventQueue.TRACKING,
                UIEvent.fromToggleRepost(wasReposted,
                        playQueueManager.getScreenTag(),
                        Screen.PLAYER_MAIN.get(),
                        trackUrn,
                        trackUrn,
                        playQueueManager.getCurrentPromotedSourceInfo(trackUrn)));
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
        updateCommentPosition(lastProgress.getPosition());
    }

    public void setIsUserRepost(boolean isUserRepost) {
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

    private Intent buildShareIntent(PlayerTrackState track) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_subject, track.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildText(track));
        return shareIntent;
    }

    private String buildText(PlayerTrackState track) {
        if (Strings.isNotBlank(track.getUserName())) {
            return activity.getString(R.string.share_track_by_artist_on_soundcloud, track.getTitle(),
                    track.getUserName(), track.getPermalinkUrl());
        }
        return activity.getString(R.string.share_track_on_soundcloud, track.getTitle(), track.getPermalinkUrl());
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;
        private final RepostOperations repostOperations;
        private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
        private final EventBus eventBus;

        @Inject
        Factory(PlayQueueManager playQueueManager,
                RepostOperations repostOperations,
                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                EventBus eventBus) {
            this.playQueueManager = playQueueManager;
            this.repostOperations = repostOperations;
            this.popupMenuWrapperFactory = popupMenuWrapperFactory;
            this.eventBus = eventBus;
        }

        TrackPageMenuController create(View anchorView) {
            final FragmentActivity activityContext = (FragmentActivity) anchorView.getContext();
            return new TrackPageMenuController(playQueueManager, repostOperations,
                    activityContext, popupMenuWrapperFactory.build(activityContext, anchorView), eventBus);
        }
    }

}

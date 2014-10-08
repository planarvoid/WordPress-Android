package com.soundcloud.android.playback.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.comments.AddCommentDialogFragment;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class TrackMenuController implements ProgressAware, PopupMenuWrapper.OnMenuItemClickListener {

    public static final String INFO_DIALOG_TAG = "info_dialog";
    public static final String ADD_COMMENT_DIALOG_TAG = "add_comment_dialog";
    public static final String PLAYLIST_DIALOG_TAG = "playlist_dialog";

    public static final String SHARE_TYPE = "text/plain";

    private final FragmentActivity activity;
    private final PopupMenuWrapper popupMenuWrapper;
    private final PlayQueueManager playQueueManager;
    private final SoundAssociationOperations associationOperations;
    private final EventBus eventBus;
    private final String commentAtUnformatted;

    @Nullable private PlayerTrack track;
    @Nullable private PlaybackProgress lastProgress;

    private TrackMenuController(PlayQueueManager playQueueManager,
                                SoundAssociationOperations associationOperations,
                                FragmentActivity context,
                                PopupMenuWrapper popupMenuWrapper,
                                EventBus eventBus) {
        this.playQueueManager = playQueueManager;
        this.associationOperations = associationOperations;
        this.activity = context;
        this.popupMenuWrapper = popupMenuWrapper;
        this.eventBus = eventBus;
        this.commentAtUnformatted = activity.getString(R.string.comment_at);
        setupMenu();
    }

    @Override
    public void setProgress(PlaybackProgress progress) {
        lastProgress = progress;
        final String timestamp = ScTextUtils.formatTimestamp(progress.getPosition(), TimeUnit.MILLISECONDS);
        popupMenuWrapper.setItemText(R.id.comment, String.format(commentAtUnformatted, timestamp));
    }

    private void setupMenu() {
        popupMenuWrapper.inflate(R.menu.player_page_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        checkNotNull(track);
        switch (menuItem.getItemId()) {
            case R.id.share:
                handleShare(track);
                return true;
            case R.id.repost:
                handleRepost(track.getUrn());
                return true;
            case R.id.unpost:
                handleUnpost(track.getUrn());
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
        final AddCommentDialogFragment fragment = AddCommentDialogFragment.create(track.getSource(), lastProgress, playQueueManager.getScreenTag());
        fragment.show(activity.getSupportFragmentManager(), ADD_COMMENT_DIALOG_TAG);
    }

    private void handleShare(PlayerTrack track) {
        if (!track.isPrivate()) {
            activity.startActivity(buildShareIntent(track));
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromShare(playQueueManager.getScreenTag(), track.getUrn()));
        }
    }

    private void handleUnpost(Urn urn) {
        fireAndForget(associationOperations.toggleRepost(urn, false));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleRepost(false, playQueueManager.getScreenTag(), urn));
    }

    private void handleRepost(Urn urn) {
        fireAndForget(associationOperations.toggleRepost(urn, true));
        eventBus.publish(EventQueue.TRACKING, UIEvent.fromToggleRepost(true, playQueueManager.getScreenTag(), urn));
    }

    private void showAddToPlaylistDialog(PlayerTrack track) {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track.toPropertySet(), playQueueManager.getScreenTag());
        from.show(activity.getSupportFragmentManager(), PLAYLIST_DIALOG_TAG);
    }

    public void setTrack(PlayerTrack track) {
        this.track = track;
        setIsUserRepost(track.isUserRepost());
        setMenuPrivacy(track.isPrivate());
    }

    public void setIsUserRepost(boolean isUserRepost) {
        popupMenuWrapper.setItemVisible(R.id.unpost, isUserRepost);
        popupMenuWrapper.setItemVisible(R.id.repost, !isUserRepost);
    }

    private void setMenuPrivacy(boolean isPrivate){
        popupMenuWrapper.setItemEnabled(R.id.unpost, !isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.repost, !isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.share, !isPrivate);
    }

    public void show() {
        popupMenuWrapper.show();
    }

    public void dismiss() {
        popupMenuWrapper.dismiss();
    }

    private Intent buildShareIntent(PlayerTrack track) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.share_subject, track.getTitle()));
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildText(track));
        return shareIntent;
    }

    private String buildText(PlayerTrack track) {
        if (ScTextUtils.isNotBlank(track.getUserName())) {
            return activity.getString(R.string.share_track_by_artist_on_soundcloud, track.getTitle(), track.getUserName(), track.getPermalinkUrl());
        }
        return activity.getString(R.string.share_track_on_soundcloud, track.getTitle(), track.getPermalinkUrl());
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;
        private final SoundAssociationOperations associationOperations;
        private final PopupMenuWrapper.Factory popupMenuWrapperFactory;
        private final EventBus eventBus;

        @Inject
        Factory(PlayQueueManager playQueueManager,
                SoundAssociationOperations associationOperations,
                PopupMenuWrapper.Factory popupMenuWrapperFactory,
                EventBus eventBus) {
            this.playQueueManager = playQueueManager;
            this.associationOperations = associationOperations;
            this.popupMenuWrapperFactory = popupMenuWrapperFactory;
            this.eventBus = eventBus;
        }

        TrackMenuController create(View anchorView) {
            final FragmentActivity activityContext = (FragmentActivity) anchorView.getContext();
            return new TrackMenuController(playQueueManager, associationOperations,
                    activityContext, popupMenuWrapperFactory.build(activityContext, anchorView), eventBus);
        }
    }

}

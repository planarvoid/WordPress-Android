package com.soundcloud.android.playback.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class TrackMenuController implements PopupMenuWrapper.OnMenuItemClickListener {

    public static final String INFO_DIALOG_TAG = "info_dialog";
    public static final String PLAYLIST_DIALOG_TAG = "playlist_dialog";

    public static final String SHARE_TYPE = "text/plain";

    private final FragmentActivity activity;
    private final PopupMenuWrapper popupMenuWrapper;
    private final PlayQueueManager playQueueManager;
    private final SoundAssociationOperations associationOperations;

    @Nullable private PlayerTrack track;

    private TrackMenuController(View anchorView,
                                PlayQueueManager playQueueManager,
                                SoundAssociationOperations associationOperations,
                                FragmentActivity context,
                                PopupMenuWrapper popupMenuWrapper) {
        this.playQueueManager = playQueueManager;
        this.associationOperations = associationOperations;
        this.activity = context;
        this.popupMenuWrapper = popupMenuWrapper;
        setupMenu(anchorView);
    }

    private void setupMenu(View anchorView) {
        popupMenuWrapper.inflate(R.menu.player_page_actions);
        popupMenuWrapper.setOnMenuItemClickListener(this);
        anchorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuWrapper.show();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        checkNotNull(track);
        switch (menuItem.getItemId()) {
            case R.id.share:
                if (!track.isPrivate()) {
                    activity.startActivity(buildShareIntent(track));
                }
                return true;
            case R.id.repost:
                fireAndForget(associationOperations.toggleRepost(track.getUrn(), true));
                return true;
            case R.id.unpost:
                fireAndForget(associationOperations.toggleRepost(track.getUrn(), false));
                return true;
            case R.id.info:
                TrackInfoFragment.create(track.getUrn()).show(activity.getSupportFragmentManager(), INFO_DIALOG_TAG);
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog();
                return true;
            default:
                return false;
        }
    }

    private void showAddToPlaylistDialog() {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track.toPropertySet(), playQueueManager.getScreenTag());
        from.show(activity.getSupportFragmentManager(), PLAYLIST_DIALOG_TAG);
    }

    public void setTrack(PlayerTrack track) {
        this.track = track;
        setIsUserRepost(track.isUserRepost());
        setMenuPrivacy(track.isPrivate());
    }

    public void setIsUserRepost(boolean isUserRepost){
        popupMenuWrapper.setItemVisible(R.id.unpost, isUserRepost);
        popupMenuWrapper.setItemVisible(R.id.repost, !isUserRepost);
    }

    private void setMenuPrivacy(boolean isPrivate){
        popupMenuWrapper.setItemEnabled(R.id.unpost, isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.repost, !isPrivate);
        popupMenuWrapper.setItemEnabled(R.id.share, !isPrivate);
    }

    public void dismiss() {
        popupMenuWrapper.dismiss();
    }

    private Intent buildShareIntent(PlayerTrack track) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, buildSubject(track));
        shareIntent.putExtra(Intent.EXTRA_TEXT, track.getPermalinkUrl());
        return shareIntent;
    }

    private String buildSubject(PlayerTrack track) {
        final StringBuilder sb = new StringBuilder(track.getTitle()).append(" ");
        if (ScTextUtils.isNotBlank(track.getUserName())) {
            sb.append(activity.getString(R.string.share_by, track.getUserName())).append(" ");
        }
        sb.append(activity.getString(R.string.share_on_soundcloud));
        return sb.toString();
    }

    static class Factory {
        private final PlayQueueManager playQueueManager;
        private final SoundAssociationOperations associationOperations;
        private final PopupMenuWrapper.Factory popupMenuWrapperFactory;

        @Inject
        Factory(PlayQueueManager playQueueManager, SoundAssociationOperations associationOperations, PopupMenuWrapper.Factory popupMenuWrapperFactory) {
            this.playQueueManager = playQueueManager;
            this.associationOperations = associationOperations;
            this.popupMenuWrapperFactory = popupMenuWrapperFactory;
        }

        TrackMenuController create(View anchorView) {
            final FragmentActivity activityContext = (FragmentActivity) anchorView.getContext();
            return new TrackMenuController(anchorView, playQueueManager, associationOperations,
                    activityContext, popupMenuWrapperFactory.build(activityContext, anchorView));
        }
    }

}

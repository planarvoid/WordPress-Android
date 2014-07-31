package com.soundcloud.android.playback.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class TrackMenuController implements PopupMenu.OnMenuItemClickListener {

    public static final String SHARE_TYPE = "text/plain";

    private final FragmentActivity activity;
    private final PopupMenu popupMenu;
    private final TrackPageListener trackPageListener;
    private Bundle infoArgs;
    private final PlayQueueManager playQueueManager;
    private final SoundAssociationOperations associationOperations;
    @Nullable private Intent shareIntent;
    @Nullable private PlayerTrack track;

    private TrackMenuController(View anchorView,
                                PlayQueueManager playQueueManager,
                                SoundAssociationOperations associationOperations,
                                FragmentActivity context,
                                TrackPageListener trackPageListener) {
        this.playQueueManager = playQueueManager;
        this.associationOperations = associationOperations;
        this.activity = context;
        this.popupMenu = new PopupMenu(activity, anchorView);
        this.trackPageListener = trackPageListener;
        setupMenu(anchorView);
    }

    private void setupMenu(View anchorView) {
        popupMenu.inflate(R.menu.player_page_actions);
        popupMenu.setOnMenuItemClickListener(this);
        anchorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.share:
                if (shareIntent != null) {
                    activity.startActivity(shareIntent);
                }
                return true;
            case R.id.repost:
                checkNotNull(track);
                fireAndForget(associationOperations.toggleRepost(track.getUrn(), true));
                return true;
            case R.id.unpost:
                checkNotNull(track);
                fireAndForget(associationOperations.toggleRepost(track.getUrn(), false));
                return true;
            case R.id.info:
                TrackInfoFragment.create(infoArgs).show(((FragmentActivity) context).getSupportFragmentManager(), "frage");
            case R.id.add_to_playlist:
                showAddToPlaylistDialog();
                return true;
            default:
                return false;
        }
    }

    private void showAddToPlaylistDialog() {
        checkNotNull(track);
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track.toPropertySet(), playQueueManager.getScreenTag());
        from.show(activity.getSupportFragmentManager(), "playlist_dialog");
    }

    public void setTrack(PlayerTrack track) {
        this.track = track;
        setIsUserRepost(track.isUserRepost());
        infoArgs = TrackInfoFragment.createArgs(track.getUrn());

        if (track.isPrivate()) {
            setMenuPrivacy(true);
            shareIntent = null;
        } else {
            setMenuPrivacy(false);
            buildShareIntent(track);
        }
    }

    public void setIsUserRepost(boolean isUserRepost){
        popupMenu.getMenu().findItem(R.id.unpost).setVisible(isUserRepost);
        popupMenu.getMenu().findItem(R.id.repost).setVisible(!isUserRepost);
    }

    private void setMenuPrivacy(boolean isPrivate){
        popupMenu.getMenu().findItem(R.id.share).setEnabled(!isPrivate);
        popupMenu.getMenu().findItem(R.id.repost).setEnabled(!isPrivate);
        popupMenu.getMenu().findItem(R.id.unpost).setEnabled(!isPrivate);
    }

    public void dismiss() {
        popupMenu.dismiss();
    }

    private void buildShareIntent(PlayerTrack track) {
        shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType(SHARE_TYPE);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, buildSubject(track));
        shareIntent.putExtra(Intent.EXTRA_TEXT, track.getPermalinkUrl());
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
        private final TrackPageListener trackPageListener
                ;

        @Inject
        Factory(PlayQueueManager playQueueManager, SoundAssociationOperations associationOperations, TrackPageListener trackPageListener) {
            this.playQueueManager = playQueueManager;
            this.associationOperations = associationOperations;
            this.trackPageListener = trackPageListener;
        }

        TrackMenuController create(View anchorView) {
            return new TrackMenuController(anchorView, playQueueManager, associationOperations, (FragmentActivity) anchorView.getContext(), trackPageListener);
        }
    }
}

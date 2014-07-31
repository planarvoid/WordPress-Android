package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class TrackMenuController implements PopupMenu.OnMenuItemClickListener {

    public static final String SHARE_TYPE = "text/plain";

    private final Context context;
    private final PopupMenu popupMenu;
    private final TrackPageListener trackPageListener;
    @Nullable private Intent shareIntent;

    private TrackMenuController(Context context, View anchorView, TrackPageListener trackPageListener) {
        this.context = context;
        this.popupMenu = new PopupMenu(context, anchorView);
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
                    context.startActivity(shareIntent);
                }
                return true;
            case R.id.repost:
                trackPageListener.onToggleRepost(true);
                return true;
            case R.id.unpost:
                trackPageListener.onToggleRepost(false);
                return true;
            default:
                return false;
        }
    }

    public void setTrack(PlayerTrack track) {
        setIsUserRepost(track.isUserRepost());

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
            sb.append(context.getString(R.string.share_by, track.getUserName())).append(" ");
        }
        sb.append(context.getString(R.string.share_on_soundcloud));
        return sb.toString();
    }

    static class Factory {

        @Inject
        Factory() { }

        TrackMenuController create(Context context, View anchorView, TrackPageListener trackPageListener) {
            return new TrackMenuController(context, anchorView, trackPageListener);
        }
    }


}

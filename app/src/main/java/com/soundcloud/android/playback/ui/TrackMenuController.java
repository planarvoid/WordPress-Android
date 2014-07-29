package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import javax.annotation.Nullable;

public class TrackMenuController implements PopupMenu.OnMenuItemClickListener {

    public static final String SHARE_TYPE = "text/plain";

    private final Context context;
    private final PopupMenu popupMenu;
    @Nullable private Intent shareIntent;

    TrackMenuController(Context context, View anchorView) {
        this.context = context;
        this.popupMenu = new PopupMenu(context, anchorView);
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
        if (menuItem.getItemId() == R.id.share) {
            if (shareIntent != null) {
                context.startActivity(shareIntent);
            }
            return true;
        }
        return false;
    }

    public void setTrack(PlayerTrack track) {
        MenuItem shareItem = popupMenu.getMenu().findItem(R.id.share);
        if (track.isPrivate()) {
            shareIntent = null;
            shareItem.setEnabled(false);
        } else {
            buildShareIntent(track);
            shareItem.setEnabled(true);
        }
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
        final StringBuilder sb = new StringBuilder(track.getTitle());
        if (ScTextUtils.isNotBlank(track.getUserName())) {
            sb.append(context.getString(R.string.share_by, track.getUserName()));
        }
        sb.append(context.getString(R.string.share_on_soundcloud));
        return sb.toString();
    }

}

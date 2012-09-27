package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class TrackAdapter extends ScBaseAdapter<Track> {
    private QuickAction mQuickActionMenu;

    public TrackAdapter(Context context, Uri uri) {
        super(context, uri);
        mQuickActionMenu = new QuickTrackMenu(context, this);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new TrackInfoBar(mContext,this);
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return mQuickActionMenu;
    }

    @Override
    public void handleListItemClick(int position, long id) {
        mContext.startService(new Intent(CloudPlaybackService.PLAY_ACTION).putExtra(Track.EXTRA, getItem(position)));
    }
}

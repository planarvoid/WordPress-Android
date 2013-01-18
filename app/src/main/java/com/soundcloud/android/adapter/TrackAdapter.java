package com.soundcloud.android.adapter;


import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import android.content.Context;
import android.net.Uri;

public class TrackAdapter extends ScBaseAdapter<Track> implements PlayableAdapter{
    private QuickAction mQuickActionMenu;

    public TrackAdapter(Context context, Uri uri) {
        super(context, uri);
        mQuickActionMenu = new QuickTrackMenu(context, this);
    }

    @Override
    protected IconLayout createRow(int position) {
        return new PlayableRow(mContext);
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return mQuickActionMenu;
    }

    @Override
    public int handleListItemClick(int position, long id) {
        PlayUtils.playFromAdapter(mContext, this, mData, position);
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return mContent.isMine() ? mContentUri : null;
    }
}

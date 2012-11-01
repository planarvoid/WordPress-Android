package com.soundcloud.android.adapter;


import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;
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
    public boolean isEmpty() {
        return super.isEmpty();
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
    public int handleListItemClick(int position, long id) {
        PlayUtils.playFromAdapter(mContext, this, mData, position, id);
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return mContent.isMine() ? mContentUri : null;
    }
}

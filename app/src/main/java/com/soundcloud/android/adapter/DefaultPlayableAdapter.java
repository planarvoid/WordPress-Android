package com.soundcloud.android.adapter;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import android.content.Context;
import android.net.Uri;

public class DefaultPlayableAdapter extends ScBaseAdapter<Playable> implements PlayableAdapter {
    private QuickAction mQuickActionMenu;

    public DefaultPlayableAdapter(Context context, Uri uri) {
        super(context, uri);
        mQuickActionMenu = new QuickTrackMenu(context, this);
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context);
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return mQuickActionMenu;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        PlayUtils.playFromAdapter(context, this, mData, position);
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return mContent.isMine() ? mContentUri : null;
    }

    @Override
    public Playable getPlayable(int position) {
        return getItem(position);
    }
}

package com.soundcloud.android.adapter;


import com.soundcloud.android.model.Playable;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class DefaultPlayableAdapter extends ScBaseAdapter<Playable> {

    public DefaultPlayableAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected IconLayout createRow(int position) {
        return new PlayableRow(mContext);
    }

    @Override
    public int handleListItemClick(int position, long id) {
        Uri streamUri = mContent.isMine() ? mContentUri : null;
        PlayUtils.playFromAdapter(mContext, mData, position, streamUri);
        return ItemClickResults.LEAVING;
    }
}

package com.soundcloud.android.adapter;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class SoundAssociationAdapter extends ScBaseAdapter<SoundAssociation> implements PlayableAdapter {
    public SoundAssociationAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected IconLayout createRow(int position) {
        return new PlayableRow(mContext);
    }

    @Override
    public int handleListItemClick(int position, long id) {
        PlayUtils.playFromAdapter(mContext, this, mData, position);
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return null;
    }
}

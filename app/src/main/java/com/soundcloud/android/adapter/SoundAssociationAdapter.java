package com.soundcloud.android.adapter;

import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class SoundAssociationAdapter extends ScBaseAdapter<SoundAssociation> {

    private PlayUtils mPlayUtils;

    public SoundAssociationAdapter(Uri uri) {
        super(uri);
        mPlayUtils = new PlayUtils();
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        Uri streamUri = Content.match(mContentUri).isMine() ? mContentUri : null;
        mPlayUtils.playFromAdapter(context, mData, position, streamUri);
        return ItemClickResults.LEAVING;
    }

}

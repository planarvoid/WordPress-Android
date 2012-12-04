package com.soundcloud.android.adapter;

import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.content.Context;
import android.net.Uri;

import java.util.Iterator;

public class SoundAssociationAdapter extends ScBaseAdapter<SoundAssociation> implements PlayableAdapter {
    public SoundAssociationAdapter(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new TrackInfoBar(mContext,this);
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

    @Override
    public void addItems(CollectionHolder<SoundAssociation> newItems) {
        // filter out playlists
        for (Iterator<SoundAssociation> it = newItems.iterator(); it.hasNext();) {
            if (it.next().playlist != null) {
                it.remove();
            }
        }
        super.addItems(newItems);
    }
}

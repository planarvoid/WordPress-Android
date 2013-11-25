package com.soundcloud.android.associations;

import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.playback.service.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class SoundAssociationAdapter extends ScBaseAdapter<SoundAssociation> {

    private PlaybackOperations mPlaybackOperations;

    public SoundAssociationAdapter(Uri uri) {
        super(uri);
        mPlaybackOperations = new PlaybackOperations();
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        Uri streamUri = Content.match(mContentUri).isMine() ? mContentUri : null;
        mPlaybackOperations.playFromAdapter(context, mData, position, streamUri);
        return ItemClickResults.LEAVING;
    }

}

package com.soundcloud.android.associations;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.provider.Content;

import android.content.Context;
import android.net.Uri;

public class SoundAssociationAdapter extends ScBaseAdapter<SoundAssociation> {

    private PlaybackOperations mPlaybackOperations;
    private ImageOperations mImageOperations;

    public SoundAssociationAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        mPlaybackOperations = new PlaybackOperations();
        mImageOperations = imageOperations;
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context, mImageOperations);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        Uri streamUri = Content.match(mContentUri).isMine() ? mContentUri : null;
        mPlaybackOperations.playFromAdapter(context, mData, position, streamUri, screen);
        return ItemClickResults.LEAVING;
    }

}

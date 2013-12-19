package com.soundcloud.android.collections;


import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class DefaultPlayableAdapter extends ScBaseAdapter<Playable> {

    private PlaybackOperations mPlaybackOperations;
    private ImageOperations mImageOperations;

    public DefaultPlayableAdapter(Uri uri, ImageOperations imageOperations) {
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
        Uri streamUri = mContent.isMine() ? mContentUri : null;
        mPlaybackOperations.playFromAdapter(context, mData, position, streamUri, screen);
        return ItemClickResults.LEAVING;
    }
}

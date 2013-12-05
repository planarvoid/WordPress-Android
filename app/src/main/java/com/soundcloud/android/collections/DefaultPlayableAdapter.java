package com.soundcloud.android.collections;


import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;

import android.content.Context;
import android.net.Uri;

public class DefaultPlayableAdapter extends ScBaseAdapter<Playable> {

    private PlaybackOperations mPlaybackOperations;

    public DefaultPlayableAdapter(Uri uri) {
        super(uri);
        mPlaybackOperations = new PlaybackOperations();
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        Uri streamUri = mContent.isMine() ? mContentUri : null;
        mPlaybackOperations.playFromAdapter(context, mData, position, streamUri, screen);
        return ItemClickResults.LEAVING;
    }
}

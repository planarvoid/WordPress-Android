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

    private final PlaybackOperations playbackOperations;
    private final ImageOperations imageOperations;

    public DefaultPlayableAdapter(Uri uri, ImageOperations imageOperations) {
        super(uri);
        playbackOperations = new PlaybackOperations();
        this.imageOperations = imageOperations;
    }

    @Override
    protected IconLayout createRow(Context context, int position) {
        return new PlayableRow(context, imageOperations);
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        Uri streamUri = content.isMine() ? contentUri : null;
        playbackOperations.playFromAdapter(context, data, position, streamUri, screen);
        return ItemClickResults.LEAVING;
    }
}

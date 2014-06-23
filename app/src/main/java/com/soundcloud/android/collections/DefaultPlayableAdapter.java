package com.soundcloud.android.collections;


import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;

public class DefaultPlayableAdapter extends ScBaseAdapter<Playable> {

    @Inject ImageOperations imageOperations;

    @Inject PlaybackOperations playbackOperations;

    public DefaultPlayableAdapter(Uri uri) {
        super(uri);
        SoundCloudApplication.getObjectGraph().inject(this);
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

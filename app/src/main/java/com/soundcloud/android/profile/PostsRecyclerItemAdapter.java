package com.soundcloud.android.profile;

import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;

import javax.inject.Inject;

public class PostsRecyclerItemAdapter extends MixedPlayableRecyclerItemAdapter {

    @Inject
    public PostsRecyclerItemAdapter(PostedTrackItemRenderer trackRenderer,
                                    PostedPlaylistItemRenderer playlistRenderer) {
        super(trackRenderer, playlistRenderer);
    }
}

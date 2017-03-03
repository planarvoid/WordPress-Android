package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.view.collection.CollectionRenderer;

public class DefaultEmptyStateProvider implements CollectionRenderer.EmptyStateProvider {
    @Override
    public int waitingView() {
        return R.layout.emptyview_progress;
    }

    @Override
    public int connectionErrorView() {
        return R.layout.emptyview_connection_error;
    }

    @Override
    public int serverErrorView() {
        return R.layout.emptyview_server_error;
    }

    @Override
    public int emptyView() {
        return R.layout.emptyview_playlist_no_tracks;
    }
}

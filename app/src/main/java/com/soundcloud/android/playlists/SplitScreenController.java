package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.EmptyView;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

class SplitScreenController implements PlaylistDetailsController {

    private final PlaylistTracksAdapter adapter;
    private EmptyView emptyView;
    private View listViewContainer;

    SplitScreenController(ImageOperations imageOperations) {
        adapter = new PlaylistTracksAdapter(imageOperations);
    }

    @Override
    public ItemAdapter<Track> getAdapter() {
        return adapter;
    }

    @Override
    public boolean hasContent() {
        return !adapter.isEmpty();
    }

    @Override
    public void onViewCreated(View layout, Resources resources) {
        emptyView = (EmptyView) layout.findViewById(android.R.id.empty);
        emptyView.setMessageText(resources.getString(R.string.empty_playlist_description));

        listViewContainer = layout.findViewById(R.id.container);
        ListView listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setEmptyView(emptyView);
    }

    @Override
    public void setListShown(boolean show) {
        listViewContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        emptyView.setStatus(status);
    }
}

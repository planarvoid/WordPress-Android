package com.soundcloud.android.playlists;

import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

class DefaultController implements PlaylistDetailsController {

    private final InlinePlaylistTracksAdapter mAdapter;
    private ListView mListView;

    DefaultController(ImageOperations imageOperations) {
        this(new InlinePlaylistTracksAdapter(imageOperations));
    }

    DefaultController(InlinePlaylistTracksAdapter itemAdapter) {
        mAdapter = itemAdapter;
    }

    @Override
    public ItemAdapter<Track> getAdapter() {
        return mAdapter;
    }

    @Override
    public boolean hasContent() {
        return mAdapter.hasContentItems();
    }

    @Override
    public void onViewCreated(View layout, Resources resources) {
        mListView = (ListView) layout.findViewById(android.R.id.list);
    }

    @Override
    public void setListShown(boolean show) {
        mListView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mAdapter.setEmptyViewStatus(status);
    }
}

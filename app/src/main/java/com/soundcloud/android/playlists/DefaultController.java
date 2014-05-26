package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapters.ItemAdapter;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

class DefaultController implements PlaylistDetailsController {

    private final InlinePlaylistTracksAdapter adapter;
    private final InlinePlaylistTrackPresenter presenter;
    private ListView listView;

    @Inject
    DefaultController(InlinePlaylistTracksAdapter itemAdapter, InlinePlaylistTrackPresenter presenter) {
        this.adapter = itemAdapter;
        this.presenter = presenter;
    }

    @Override
    public ItemAdapter<Track> getAdapter() {
        return adapter;
    }

    @Override
    public boolean hasContent() {
        return adapter.hasContentItems();
    }

    @Override
    public void onViewCreated(View layout, Resources resources) {
        listView = (ListView) layout.findViewById(android.R.id.list);
    }

    @Override
    public void setListShown(boolean show) {
        listView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        presenter.setEmptyViewStatus(status);
        adapter.notifyDataSetChanged();
    }
}

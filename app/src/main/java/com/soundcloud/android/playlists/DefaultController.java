package com.soundcloud.android.playlists;


import com.soundcloud.android.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

class DefaultController extends PlaylistDetailsController {

    private ListView listView;
    private InlinePlaylistTracksAdapter adapter;

    @Inject
    DefaultController(InlinePlaylistTracksAdapter itemAdapter, EventBus eventBus) {
        super(itemAdapter.getTrackPresenter(), itemAdapter, eventBus);
        this.adapter = itemAdapter;
    }

    @Override
    public boolean hasContent() {
        return adapter.hasContentItems();
    }

    @Override
    public void onViewCreated(View layout, Resources resources) {
        super.onViewCreated(layout, resources);
        listView = (ListView) layout.findViewById(android.R.id.list);
    }

    @Override
    public void setListShown(boolean show) {
        listView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(int status) {
        adapter.setEmptyViewStatus(status);
        adapter.notifyDataSetChanged();
    }
}

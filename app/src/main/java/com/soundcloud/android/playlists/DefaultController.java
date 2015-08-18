package com.soundcloud.android.playlists;


import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

class DefaultController extends PlaylistDetailsController {

    private final InlinePlaylistTracksAdapter adapter;

    @Inject
    DefaultController(InlinePlaylistTracksAdapter itemAdapter, EventBus eventBus) {
        super(itemAdapter.getPlaylistItemRenderer(), itemAdapter, eventBus);
        this.adapter = itemAdapter;
    }

    @Override
    public boolean hasContent() {
        return adapter.hasContentItems();
    }

    @Override
    public void onViewCreated(View layout, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);
        listView = (ListView) layout.findViewById(android.R.id.list);
    }

    @Override
    public void setListShown(boolean show) {
        listView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(EmptyView.Status status) {
        adapter.setEmptyViewStatus(status);
        adapter.notifyDataSetChanged();
    }
}

package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

class SplitScreenController extends PlaylistDetailsController {

    private EmptyView emptyView;
    private View listViewContainer;

    @Inject
    SplitScreenController(PlaylistTrackItemRenderer trackRenderer, EventBus eventBus) {
        super(trackRenderer, new SplitScreenItemAdapter(trackRenderer), eventBus);
    }

    @Override
    public boolean hasContent() {
        return !getAdapter().isEmpty();
    }

    @Override
    public void onViewCreated(View layout, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);
        emptyView = (EmptyView) layout.findViewById(android.R.id.empty);
        emptyView.setMessageText(layout.getContext().getString(R.string.empty_playlist_description));

        listViewContainer = layout.findViewById(R.id.container);
        listView = (ListView) layout.findViewById(android.R.id.list);
        listView.setEmptyView(emptyView);
    }

    @Override
    public void setListShown(boolean show) {
        listViewContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setEmptyViewStatus(EmptyView.Status status) {
        emptyView.setStatus(status);
    }

    private static class SplitScreenItemAdapter extends ListItemAdapter<TrackItem> {
        public SplitScreenItemAdapter(PlaylistTrackItemRenderer trackRenderer) {
            super(trackRenderer);
        }

        @Override
        public boolean isEnabled(int position) {
            return !getItem(position).isBlocked();
        }
    }
}

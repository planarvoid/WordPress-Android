package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
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
    SplitScreenController(PlaylistTrackItemRenderer trackRenderer,
                          PlaylistUpsellOperations playlistUpsellOperations,
                          PlaylistUpsellItemRenderer upsellItemRenderer,
                          EventBus eventBus,
                          Navigator navigator) {
        super(trackRenderer,
              upsellItemRenderer,
              new SplitScreenItemAdapter(trackRenderer, upsellItemRenderer),
              playlistUpsellOperations,
              eventBus,
              navigator);
    }

    @Override
    void setEmptyStateMessage(String title, String description) {
        emptyView.setMessageText(description);
    }

    @Override
    public boolean hasContent() {
        return !getAdapter().isEmpty();
    }

    @Override
    public void onViewCreated(View layout, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(layout, savedInstanceState);
        emptyView = (EmptyView) layout.findViewById(android.R.id.empty);

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

    private static class SplitScreenItemAdapter extends ListItemAdapter<TypedListItem> {

        private static final int TRACK_ITEM_TYPE = 0;
        private static final int UPSELL_ITEM_TYPE = 1;

        public SplitScreenItemAdapter(PlaylistTrackItemRenderer trackRenderer, PlaylistUpsellItemRenderer upsellItemRenderer) {
            super(new CellRendererBinding<>(DEFAULT_VIEW_TYPE, trackRenderer),
                  new CellRendererBinding<>(UPSELL_ITEM_TYPE, upsellItemRenderer));
        }

        @Override
        public int getItemViewType(int position) {
            TypedListItem item = getItem(position);
            if (item.getUrn().isTrack()) {
                return TRACK_ITEM_TYPE;
            } else {
                return UPSELL_ITEM_TYPE;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == UPSELL_ITEM_TYPE || !((TrackItem) getItem(position)).isBlocked();
        }
    }
}

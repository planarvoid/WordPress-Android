package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class EmptyPlaylistTracksRenderer implements CellRenderer<TrackItem>, EmptyViewAware {

    private EmptyView.Status emptyViewStatus = EmptyView.Status.WAITING;
    private EmptyViewBuilder viewBuilder;

    @Inject
    EmptyPlaylistTracksRenderer() {
        this.viewBuilder = new EmptyViewBuilder()
                .withImage(R.drawable.empty_playlists);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final Context context = parent.getContext();
        final EmptyView emptyView = viewBuilder.build(context);
        emptyView.setPadding(0, ViewUtils.dpToPx(context, 48), 0, ViewUtils.dpToPx(context, 48));
        return emptyView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> items) {
        ((EmptyView) itemView).setStatus(emptyViewStatus);
    }

    public void setEmptyViewStatus(EmptyView.Status emptyViewStatus) {
        this.emptyViewStatus = emptyViewStatus;
    }

    public void setEmptyStateMessage(String title, String description) {
        this.viewBuilder.withMessageText(title).withSecondaryText(description);
    }
}

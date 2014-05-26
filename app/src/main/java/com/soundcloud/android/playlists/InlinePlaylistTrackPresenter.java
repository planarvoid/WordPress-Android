package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import javax.inject.Inject;
import java.util.List;

class InlinePlaylistTrackPresenter implements CellPresenter<Track>, EmptyViewAware {

    private final ImageOperations imageOperations;

    private int emptyViewStatus = EmptyView.Status.WAITING;

    @Inject
    InlinePlaylistTrackPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent, int itemViewType) {
        Context context = parent.getContext();
        if (itemViewType == Adapter.IGNORE_ITEM_VIEW_TYPE) {
            EmptyView emptyView = new EmptyViewBuilder()
                    .withImage(R.drawable.empty_playlists)
                    .withMessageText(context.getString(R.string.empty_playlist_title))
                    .withSecondaryText(context.getString(R.string.empty_playlist_description))
                    .build(context);
            emptyView.setPadding(0, ViewUtils.dpToPx(context, 48), 0, ViewUtils.dpToPx(context, 48));
            return emptyView;
        } else {
            return new PlayableRow(context, imageOperations);
        }
    }

    @Override
    public void bindItemView(int position, View itemView, int itemViewType, List<Track> tracks) {
        if (itemViewType == Adapter.IGNORE_ITEM_VIEW_TYPE) {
            ((EmptyView) itemView).setStatus(emptyViewStatus);
        } else {
            ((PlayableRow) itemView).display(position, tracks.get(position));
        }
    }

    public void setEmptyViewStatus(int emptyViewStatus){
        this.emptyViewStatus = emptyViewStatus;
    }

}

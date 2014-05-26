package com.soundcloud.android.playlists;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlaylistTrackPresenter implements CellPresenter<Track, PlayableRow> {

    private final ImageOperations imageOperations;

    @Inject
    PlaylistTrackPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public PlayableRow createItemView(int position, ViewGroup parent, int itemViewType) {
        return new PlayableRow(parent.getContext(), imageOperations);
    }

    @Override
    public void bindItemView(int position, PlayableRow itemView, List<Track> items) {
        itemView.display(position, items.get(position));
    }
}

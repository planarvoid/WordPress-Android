package com.soundcloud.android.view.adapters;

import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

// wraps the legacy playable presentation logic in a cell presenter
public class LegacyPlayableRowPresenter<T extends ScResource> implements CellPresenter<T> {

    public static final int TYPE_PLAYABLE = LegacyPlayableRowPresenter.class.hashCode();

    private final ImageOperations imageOperations;

    public LegacyPlayableRowPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public PlayableRow createItemView(int position, ViewGroup parent) {
        return new PlayableRow(parent.getContext(), imageOperations);
    }

    @Override
    public void bindItemView(int position, View itemView, List<T> items) {
        ((PlayableRow) itemView).display(position, items.get(position));
    }

    @Override
    public int getItemViewType() {
        return TYPE_PLAYABLE;
    }
}

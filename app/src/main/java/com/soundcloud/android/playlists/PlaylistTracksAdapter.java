package com.soundcloud.android.playlists;

import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistTracksAdapter extends ItemAdapter<Track> {

    private static final int INITIAL_SIZE = 10;

    private final ImageOperations mImageOperations;

    @Inject
    PlaylistTracksAdapter(ImageOperations imageOperations) {
        super(INITIAL_SIZE);
        mImageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        return new PlayableRow(parent.getContext(), mImageOperations);
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ((PlayableRow) itemView).display(position, mItems.get(position));
    }
}

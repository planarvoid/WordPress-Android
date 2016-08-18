package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class PlayHistoryTrackRenderer implements CellRenderer<PlayHistoryItemTrack> {

    private final DownloadableTrackItemRenderer renderer;

    @Inject
    public PlayHistoryTrackRenderer(DownloadableTrackItemRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return renderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlayHistoryItemTrack> items) {
        PlayHistoryItemTrack track = items.get(position);
        renderer.bindItemView(0, itemView, Collections.singletonList(track.trackItem()));
    }
}

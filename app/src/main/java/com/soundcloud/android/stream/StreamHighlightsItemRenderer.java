package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionPreviewView;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.Lists;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StreamHighlightsItemRenderer implements CellRenderer<StreamItem> {

    public interface Listener {
        void onStreamHighlightsClicked(List<Urn> tracks);
    }

    private final ImageOperations imageOperations;
    private final Resources resources;

    private Listener listener;

    @Inject
    StreamHighlightsItemRenderer(ImageOperations imageOperations, Resources resources) {
        // everything for dagger
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(R.layout.stream_highlights_card, parent, false);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<StreamItem> notifications) {
        StreamItem.StreamHighlights streamHighlights = (StreamItem.StreamHighlights) notifications.get(position);
        itemView.setOnClickListener(view -> {
            listener.onStreamHighlightsClicked(Lists.transform(streamHighlights.suggestedTrackItems(), TrackItem.TO_URN));
        });

        setThumbnails(streamHighlights.suggestedTrackItems(),
                      (CollectionPreviewView) itemView.findViewById(R.id.stream_highlights_preview));
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageOperations, imageResources,
                                      resources.getInteger(R.integer.stream_preview_highlights_thumbnail_count));
    }
}

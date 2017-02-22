package com.soundcloud.android.stream;

import butterknife.ButterKnife;
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
import android.widget.TextView;

import javax.inject.Inject;
import java.util.HashSet;
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

        setThumbnails(streamHighlights.suggestedTrackItems(), ButterKnife.findById(itemView, R.id.stream_highlights_preview));
        setHighlightsDescription(itemView, streamHighlights);
    }

    private void setHighlightsDescription(View itemView, StreamItem.StreamHighlights streamHighlights) {
        List<String> topCreators = getTopCreators(streamHighlights.suggestedTrackItems());
        ButterKnife.<TextView>findById(itemView, R.id.stream_highlights_description)
                .setText(resources.getString(getDescriptionResource(topCreators.size()), topCreators.toArray()));
    }

    private int getDescriptionResource(int count) {
        if (count == 1) {
            return R.string.stream_highlights_description_one;
        } else if (count == 2) {
            return R.string.stream_highlights_description_two;
        } else {
            return R.string.stream_highlights_description_other;
        }
    }

    private List<String> getTopCreators(List<TrackItem> trackItems) {
        HashSet<String> strings = new HashSet<>(Lists.transform(trackItems, (trackItem) -> trackItem.creatorName()));
        return Lists.newArrayList(strings).subList(0, Math.min(strings.size(), 3));
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageOperations, imageResources,
                                      resources.getInteger(R.integer.stream_preview_highlights_thumbnail_count));
    }
}

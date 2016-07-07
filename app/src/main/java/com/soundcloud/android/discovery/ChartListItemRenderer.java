package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionPreviewView;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class ChartListItemRenderer implements CellRenderer<ChartListItem> {
    private final Resources resources;

    @Inject
    public ChartListItemRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartListItem> items) {
        final ChartListItem chartListItem = items.get(position);
        final CollectionPreviewView chartListItemView = ButterKnife.findById(itemView, R.id.chart_list_item);
        chartListItemView.setTitle(chartListItem.getGenre().getStringId());
        setThumbnails(chartListItem.getTrackArtworks(), chartListItemView);
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageResources,
                                      resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }
}

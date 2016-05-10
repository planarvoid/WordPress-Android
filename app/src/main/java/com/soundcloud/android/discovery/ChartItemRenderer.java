package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import butterknife.OnClick;
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

class ChartItemRenderer implements CellRenderer<ChartItem> {
    private final ChartsPresenter chartsPresenter;
    private final Resources resources;

    @Inject
    public ChartItemRenderer(ChartsPresenter chartsPresenter, Resources resources) {
        this.chartsPresenter = chartsPresenter;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.charts_preview_item, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartItem> list) {
        ButterKnife.bind(this, itemView);
        final ChartItem chartItem = list.get(position);
        setThumbnails(chartItem.getNewAndHotTracks(), ButterKnife.<CollectionPreviewView>findById(itemView, R.id.charts_new_and_hot_preview));
        setThumbnails(chartItem.getTopFiftyTracks(), ButterKnife.<CollectionPreviewView>findById(itemView, R.id.charts_top_fifty_preview));
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageResources, resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }

    @OnClick(R.id.charts_new_and_hot_preview)
    public void onNewAndHotClicked() {
        chartsPresenter.onNewAndHotClicked();
    }

    @OnClick(R.id.charts_top_fifty_preview)
    public void onTopFiftyClicked() {
        chartsPresenter.onTopFiftyClicked();
    }
}

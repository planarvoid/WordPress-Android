package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionPreviewView;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class ChartsItemRenderer implements CellRenderer<ChartsItem> {
    private final Resources resources;

    @Inject
    ChartsItemRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                                      .inflate(R.layout.charts_preview_item, viewGroup, false);
        ButterKnife.bind(this, itemView);
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartsItem> list) {
        final ChartsItem chartsItem = list.get(position);
        initChart(findCollectionPreviewView(itemView, R.id.charts_new_and_hot_preview), chartsItem.newAndHotChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_top_fifty_preview), chartsItem.topFiftyChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_first_genre), chartsItem.firstGenreChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_second_genre), chartsItem.secondGenreChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_third_genre), chartsItem.thirdGenreChart());
    }

    private CollectionPreviewView findCollectionPreviewView(View itemView, int charts_new_and_hot_preview) {
        return ButterKnife.findById(itemView, charts_new_and_hot_preview);
    }

    private void initChart(CollectionPreviewView preview, Optional<Chart> firstGenreChartItem) {
        if (firstGenreChartItem.isPresent()) {
            preview.setVisibility(View.VISIBLE);

            final Chart chart = firstGenreChartItem.get();
            setThumbnails(chart.tracks(), preview);
            preview.setTag(chart.genre());
        } else {
            preview.setVisibility(View.GONE);
        }
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageResources,
                                      resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }

    @OnClick(R.id.charts_new_and_hot_preview)
    void onNewAndHotClicked() {
        Log.d("charts", "new and hot");
    }

    @OnClick(R.id.charts_top_fifty_preview)
    void onTopFiftyClicked() {
        Log.d("charts", "top 50");
    }

    @OnClick(R.id.charts_first_genre)
    void onFirstGenreClicked(View v) {
        onGenreClicked(((Urn) v.getTag()));
    }

    @OnClick(R.id.charts_second_genre)
    void onSecondGenreClicked(View v) {
        onGenreClicked(((Urn) v.getTag()));
    }

    @OnClick(R.id.charts_third_genre)
    void onThirdGenreClicked(View v) {
        onGenreClicked(((Urn) v.getTag()));
    }

    @OnClick(R.id.charts_genre_view_all)
    void onViewAllClicked() {
        Log.d("charts", "view all");
    }

    private void onGenreClicked(Urn urn) {
        Log.d("charts", "genre: " + urn.toString());
    }
}

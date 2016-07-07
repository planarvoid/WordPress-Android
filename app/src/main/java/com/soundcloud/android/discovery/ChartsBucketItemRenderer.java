package com.soundcloud.android.discovery;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
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

class ChartsBucketItemRenderer implements CellRenderer<ChartsBucketItem> {
    private final Resources resources;
    private final Navigator navigator;

    @Inject
    ChartsBucketItemRenderer(Resources resources, Navigator navigator) {
        this.resources = resources;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                                      .inflate(R.layout.charts_preview_item, viewGroup, false);
        ButterKnife.bind(this, itemView);
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartsBucketItem> list) {
        final ChartsBucketItem chartsBucketItem = list.get(position);
        initChart(findCollectionPreviewView(itemView, R.id.charts_new_and_hot_preview), chartsBucketItem.newAndHotChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_top_fifty_preview), chartsBucketItem.topFiftyChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_first_genre), chartsBucketItem.firstGenreChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_second_genre), chartsBucketItem.secondGenreChart());
        initChart(findCollectionPreviewView(itemView, R.id.charts_third_genre), chartsBucketItem.thirdGenreChart());
    }

    private CollectionPreviewView findCollectionPreviewView(View itemView, int charts_new_and_hot_preview) {
        return ButterKnife.findById(itemView, charts_new_and_hot_preview);
    }

    private void initChart(CollectionPreviewView preview, Optional<Chart> firstGenreChartItem) {
        if (firstGenreChartItem.isPresent()) {
            preview.setVisibility(View.VISIBLE);

            final Chart chart = firstGenreChartItem.get();
            setThumbnails(chart.trackArtworks(), preview);
            preview.setTag(chart);
        } else {
            preview.setVisibility(View.GONE);
        }
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageResources,
                                      resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }

    @OnClick(R.id.charts_new_and_hot_preview)
    void onNewAndHotClicked(View item) {
        navigator.openChart(item.getContext(), (Chart) item.getTag());
    }

    @OnClick(R.id.charts_top_fifty_preview)
    void onTopFiftyClicked(View item) {
        navigator.openChart(item.getContext(), (Chart) item.getTag());
    }

    @OnClick(R.id.charts_first_genre)
    void onFirstGenreClicked(View v) {
        onGenreClicked(((Chart) v.getTag()).genre());
    }

    @OnClick(R.id.charts_second_genre)
    void onSecondGenreClicked(View v) {
        onGenreClicked(((Chart) v.getTag()).genre());
    }

    @OnClick(R.id.charts_third_genre)
    void onThirdGenreClicked(View v) {
        onGenreClicked(((Chart) v.getTag()).genre());
    }

    @OnClick(R.id.charts_genre_view_all)
    void onViewAllClicked(View item) {
        navigator.openAllGenres(item.getContext());
    }

    private void onGenreClicked(Urn urn) {
        Log.d("charts", "genre: " + urn.toString());
    }
}

package com.soundcloud.android.discovery.charts;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class ChartsBucketItemRenderer implements CellRenderer<ChartsBucketItem> {
    private final Navigator navigator;
    private final ChartListItemRenderer chartListItemRenderer;

    @Inject
    ChartsBucketItemRenderer(Navigator navigator, ChartListItemRenderer chartListItemRenderer) {
        this.navigator = navigator;
        this.chartListItemRenderer = chartListItemRenderer;
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

        initChart(itemView, R.id.charts_new_and_hot_preview, chartsBucketItem.newAndHotChart());
        initChart(itemView, R.id.charts_top_fifty_preview, chartsBucketItem.topFiftyChart());
        initChart(itemView, R.id.charts_first_genre, chartsBucketItem.firstGenreChart());
        initChart(itemView, R.id.charts_second_genre, chartsBucketItem.secondGenreChart());
        initChart(itemView, R.id.charts_third_genre, chartsBucketItem.thirdGenreChart());
    }

    private void initChart(View itemView, int id, Optional<ChartListItem> chartOptional) {
        if (chartOptional.isPresent()) {
            itemView.setVisibility(View.VISIBLE);
            chartListItemRenderer.bindChartListItem(itemView, chartOptional.get(), id);
        } else {
            itemView.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.charts_genre_view_all)
    void onViewAllClicked(View item) {
        navigator.openAllGenres(item.getContext());
    }
}

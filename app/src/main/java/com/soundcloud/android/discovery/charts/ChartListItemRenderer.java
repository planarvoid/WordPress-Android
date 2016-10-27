package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.discovery.charts.ChartBucketType.GLOBAL;
import static com.soundcloud.android.utils.ScTextUtils.toResourceKey;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionPreviewView;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class ChartListItemRenderer implements CellRenderer<ChartListItem> {
    private final Resources resources;
    private final Navigator navigator;
    private final ImageOperations imageOperations;

    @Inject
    ChartListItemRenderer(Resources resources, Navigator navigator, ImageOperations imageOperations) {
        this.resources = resources;
        this.navigator = navigator;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.chart_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<ChartListItem> items) {
        bindChartListItem(itemView, items.get(position), R.id.chart_list_item);
        ButterKnife.findById(itemView, R.id.section_divider).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        ButterKnife.findById(itemView, R.id.item_divider).setVisibility(position != 0 ? View.VISIBLE : View.GONE);
    }

    void bindChartListItem(View itemView, final ChartListItem chartListItem, int id) {
        final CollectionPreviewView chartListItemView = ButterKnife.findById(itemView, id);
        chartListItemView.setTitle(headingFor(chartListItem, itemView, chartListItem.getChartType().value()));
        chartListItemView.refreshThumbnails(imageOperations, chartListItem.getTrackArtworks(),
                                            resources.getInteger(R.integer.collection_preview_thumbnail_count));
        chartListItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.openChart(view.getContext(),
                                    chartListItem.getGenre(),
                                    chartListItem.getChartType(),
                                    chartListItem.getChartCategory(),
                                    appendCharts(headingFor(chartListItem, view, "soundcloud")));
            }
        });
    }

    private String headingFor(ChartListItem chartListItem, View view, String globalSuffix) {
        final String genreSuffix = chartListItem.getGenre().getStringId();
        final String suffix = chartListItem.getChartBucketType() == GLOBAL ? globalSuffix : genreSuffix;
        final String headingKey = toResourceKey("charts_", suffix);
        final int headingResourceId = resources.getIdentifier(headingKey, "string", view.getContext().getPackageName());
        return (headingResourceId != 0) ? resources.getString(headingResourceId) : chartListItem.getDisplayName();
    }

    private String appendCharts(String heading) {
        return resources.getString(R.string.charts_page_header, heading);
    }

}

package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class ChartListItemAdapter extends PagingRecyclerItemAdapter<ChartListItem, RecyclerView.ViewHolder> {
    private static final int CHART_LIST_ITEM_TYPE = 0;

    @Inject
    ChartListItemAdapter(ChartListItemRenderer chartListItemRenderer) {
        super(chartListItemRenderer);
    }

    @Override
    protected ChartListItemViewHolder createViewHolder(View itemView) {
        return new ChartListItemViewHolder(itemView);
    }

    static class ChartListItemViewHolder extends RecyclerView.ViewHolder {
        ChartListItemViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getBasicItemViewType(int i) {
        return CHART_LIST_ITEM_TYPE;
    }
}

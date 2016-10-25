package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class ChartTracksAdapter extends PagingRecyclerItemAdapter<ChartTrackListItem, RecyclerView.ViewHolder> {

    @Inject
    ChartTracksAdapter(ChartTracksRenderer chartTracksRenderer,
                       ChartTracksHeaderRenderer chartTracksHeaderRenderer,
                       ChartTracksFooterRenderer chartTracksFooterRenderer) {
        super(new CellRendererBinding<>(ChartTrackListItem.Kind.TrackItem.ordinal(), chartTracksRenderer),
              new CellRendererBinding<>(ChartTrackListItem.Kind.ChartHeader.ordinal(), chartTracksHeaderRenderer),
              new CellRendererBinding<>(ChartTrackListItem.Kind.ChartFooter.ordinal(), chartTracksFooterRenderer));
    }

    @Override
    protected ChartTrackViewHolder createViewHolder(View itemView) {
        return new ChartTrackViewHolder(itemView);
    }


    static class ChartTrackViewHolder extends RecyclerView.ViewHolder {
        ChartTrackViewHolder(View itemView) {
            super(itemView);
        }
    }


    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }
}

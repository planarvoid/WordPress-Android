package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class ChartTrackAdapter extends PagingRecyclerItemAdapter<ChartTrackItem, RecyclerView.ViewHolder> {

    @Inject
    ChartTrackAdapter(ChartTrackRenderer chartTrackRenderer,
                      ChartTracksHeaderRenderer chartTracksHeaderRenderer,
                      ChartTracksFooterRenderer chartTracksFooterRenderer) {
        super(new CellRendererBinding<>(ChartTrackItem.Kind.TrackItem.ordinal(), chartTrackRenderer),
              new CellRendererBinding<>(ChartTrackItem.Kind.ChartHeader.ordinal(), chartTracksHeaderRenderer),
              new CellRendererBinding<>(ChartTrackItem.Kind.ChartFooter.ordinal(), chartTracksFooterRenderer));
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

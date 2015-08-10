package com.soundcloud.android.stations;

import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class RecentStationsAdapter extends RecyclerItemAdapter<Station, RecentStationsAdapter.StationViewHolder> {
    private static final int STATION_TYPE = 0;

    @Inject
    RecentStationsAdapter(RecentStationRenderer recentStationRenderer) {
        super(recentStationRenderer);
    }

    @Override
    protected StationViewHolder createViewHolder(View view) {
        return new StationViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return STATION_TYPE;
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        public StationViewHolder(View itemView) {
            super(itemView);
        }
    }
}

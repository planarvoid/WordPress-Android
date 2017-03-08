package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class StationsAdapter extends RecyclerItemAdapter<StationViewModel, RecyclerView.ViewHolder> implements PlayingTrackAware {
    private static final int STATION_TYPE = 0;

    @Inject
    public StationsAdapter(StationRenderer renderer) {
        super(renderer);
        this.setHasStableIds(true);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (int i = 0; i < items.size(); i++) {
            StationViewModel viewModel = items.get(i);
            final boolean isPlaying = viewModel.getStation().getUrn().equals(currentlyPlayingCollectionUrn);
            if (isPlaying != viewModel.isPlaying()) {
                items.set(i, StationViewModel.create(viewModel.getStation(),isPlaying));
                notifyItemChanged(i);
            }
        }
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return STATION_TYPE;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getStation().getUrn().getNumericId();
    }
}

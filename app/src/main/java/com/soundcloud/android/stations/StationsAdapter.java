package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

import android.view.View;

import javax.inject.Inject;

class StationsAdapter extends RecyclerItemAdapter<StationViewModel, StationsViewHolder> implements NowPlayingAdapter {
    private static final int STATION_TYPE = 0;

    @Inject
    public StationsAdapter(StationRenderer renderer) {
        super(renderer);
        this.setHasStableIds(true);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (StationViewModel viewModel : getItems()) {
            viewModel.setIsPlaying(viewModel.getStation().getUrn().equals(currentlyPlayingCollectionUrn));
        }
        notifyDataSetChanged();
    }

    @Override
    protected StationsViewHolder createViewHolder(View view) {
        return new StationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StationsViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
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

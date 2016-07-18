package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationInfoItem.Kind.StationHeader;
import static com.soundcloud.android.stations.StationInfoItem.Kind.StationTracksBucket;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class StationInfoAdapter extends PagingRecyclerItemAdapter<StationInfoItem, StationInfoAdapter.ViewHolder>
        implements PlayingTrackAware {

    @Inject
    StationInfoAdapter(StationInfoItemRenderer headerRenderer, StationInfoTracksBucketRenderer bucketRenderer) {
        super(new CellRendererBinding<>(StationTracksBucket.ordinal(), bucketRenderer),
              new CellRendererBinding<>(StationHeader.ordinal(), headerRenderer));
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        // TODO: Comes later on
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

}

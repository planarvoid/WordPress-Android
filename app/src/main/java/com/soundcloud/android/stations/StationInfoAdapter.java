package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationInfoItem.Kind.StationHeader;
import static com.soundcloud.android.stations.StationInfoItem.Kind.StationTracksBucket;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class StationInfoAdapter extends PagingRecyclerItemAdapter<StationInfoItem, RecyclerView.ViewHolder>
        implements PlayingTrackAware {

    private final StationInfoTracksBucketRenderer bucketRenderer;

    interface StationInfoClickListener {
        void onPlayButtonClicked(Context context);

        void onTrackClicked(Context context, int position);

        void onLikeToggled(Context context, boolean isLiked);
    }

    StationInfoAdapter(StationInfoClickListener clickListener,
                       StationInfoTracksBucketRenderer bucketRenderer,
                       @Provided StationInfoHeaderRendererFactory headerRenderer) {
        super(new CellRendererBinding<>(StationTracksBucket.ordinal(), bucketRenderer),
              new CellRendererBinding<>(StationHeader.ordinal(), headerRenderer.create(clickListener)));

        this.bucketRenderer = bucketRenderer;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        bucketRenderer.updateNowPlaying(currentlyPlayingUrn);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}

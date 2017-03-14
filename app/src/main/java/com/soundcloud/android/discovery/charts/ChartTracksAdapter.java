package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class ChartTracksAdapter extends PagingRecyclerItemAdapter<ChartTrackListItem, RecyclerView.ViewHolder> implements PlayingTrackAware {

    @Inject
    ChartTracksAdapter(ChartTracksRenderer chartTracksRenderer,
                       ChartTracksHeaderRenderer chartTracksHeaderRenderer,
                       ChartTracksFooterRenderer chartTracksFooterRenderer) {
        super(new CellRendererBinding<>(ChartTrackListItem.Kind.CHART_TRACK.ordinal(), chartTracksRenderer),
              new CellRendererBinding<>(ChartTrackListItem.Kind.CHART_HEADER.ordinal(), chartTracksHeaderRenderer),
              new CellRendererBinding<>(ChartTrackListItem.Kind.CHART_FOOTER.ordinal(), chartTracksFooterRenderer));
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < items.size(); i++) {
            ChartTrackListItem chartListItem = items.get(i);
            if (chartListItem.isTrack()) {
                final ChartTrackListItem.Track trackListItem = ((ChartTrackListItem.Track) chartListItem);
                final TrackItem trackItem = trackListItem.chartTrackItem().getTrackItem().withPlayingState(isNowPlaying(currentlyPlayingUrn, trackListItem));

                ChartTrackItem chartTrackItem = new ChartTrackItem(
                        trackListItem.chartTrackItem().chartType(),
                        trackItem,
                        trackListItem.chartTrackItem().chartCategory(),
                        trackListItem.chartTrackItem().genre(),
                        trackListItem.chartTrackItem().queryUrn());
                items.set(i, ChartTrackListItem.Track.create(chartTrackItem));
            }
        }

        notifyDataSetChanged();
    }

    private boolean isNowPlaying(Urn currentlyPlayingUrn, ChartTrackListItem.Track track) {
        return track.chartTrackItem().getTrackItem().getUrn().equals(currentlyPlayingUrn);
    }
}

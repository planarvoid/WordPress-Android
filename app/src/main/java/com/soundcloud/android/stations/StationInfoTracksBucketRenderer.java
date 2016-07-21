package com.soundcloud.android.stations;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class StationInfoTracksBucketRenderer implements CellRenderer<StationInfoTracksBucket> {

    private final StationTrackRendererFactory rendererFactory;
    private final StationInfoClickListener stationTrackClickListener;

    private StationTracksAdapter adapter;
    private RecyclerView recyclerView;

    StationInfoTracksBucketRenderer(StationInfoClickListener stationTrackClickListener,
                                    @Provided StationTrackRendererFactory rendererFactory) {
        this.stationTrackClickListener = stationTrackClickListener;
        this.rendererFactory = rendererFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.station_tracks_bucket, parent, false);
        initCarousel(ButterKnife.<RecyclerView>findById(view, R.id.station_tracks_carousel));
        return view;
    }

    void updateNowPlaying(Urn currentlyPlayingUrn) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                final StationInfoTrack track = adapter.getItem(i);
                final boolean isCurrent = track.getUrn().equals(currentlyPlayingUrn);

                if (track.isPlaying() || isCurrent) {
                    track.setIsPlaying(isCurrent);
                    adapter.notifyItemChanged(i);
                }

                if (isCurrent) {
                    recyclerView.smoothScrollToPosition(i);
                }
            }
        }
    }

    private void initCarousel(final RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.adapter = new StationTracksAdapter(rendererFactory.create(stationTrackClickListener));
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext(), HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StationInfoTracksBucket> items) {
        final List<StationInfoTrack> tracks = items.get(position).stationTracks();

        adapter.clear();

        for (StationInfoTrack track : tracks) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }

}

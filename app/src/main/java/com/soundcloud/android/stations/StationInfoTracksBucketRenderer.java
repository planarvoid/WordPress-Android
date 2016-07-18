package com.soundcloud.android.stations;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StationInfoTracksBucketRenderer implements CellRenderer<StationInfoTracksBucket> {

    private final StationTracksAdapter adapter;

    @Inject
    public StationInfoTracksBucketRenderer(StationTracksAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.station_tracks_bucket, parent, false);
        initCarousel(ButterKnife.<RecyclerView>findById(view, R.id.station_tracks_carousel));
        return view;
    }

    private void initCarousel(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StationInfoTracksBucket> items) {
        bindCarousel(adapter, items.get(position));
    }

    private void bindCarousel(StationTracksAdapter adapter, StationInfoTracksBucket tracksBucket) {
        final List<StationInfoTrack> tracks = tracksBucket.stationTracks();

        adapter.clear();

        for (StationInfoTrack track : tracks) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }
}

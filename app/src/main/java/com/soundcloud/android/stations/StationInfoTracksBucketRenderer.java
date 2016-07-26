package com.soundcloud.android.stations;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.TrackItem;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class StationInfoTracksBucketRenderer implements CellRenderer<StationInfoTracksBucket> {

    private StationTracksAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layout;

    StationInfoTracksBucketRenderer(StationInfoClickListener stationTrackClickListener,
                                    @Provided StationTrackRendererFactory rendererFactory) {
        this.adapter = new StationTracksAdapter(rendererFactory.create(stationTrackClickListener));
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.station_tracks_bucket, parent, false);
        initCarousel(ButterKnife.<RecyclerView>findById(view, R.id.station_tracks_carousel));
        return view;
    }

    void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            final TrackItem track = adapter.getItem(i).getTrack();
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

    private void initCarousel(final RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        this.layout = new LinearLayoutManager(recyclerView.getContext(), HORIZONTAL, false);
        recyclerView.setLayoutManager(layout);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StationInfoTracksBucket> items) {
        final StationInfoTracksBucket stationInfoTracksBucket = items.get(position);
        final List<StationInfoTrack> tracks = stationInfoTracksBucket.stationTracks();

        addTracksToAdapter(tracks);
        scrollToLastPlayedPosition(itemView, stationInfoTracksBucket);
    }

    private void scrollToLastPlayedPosition(View itemView, StationInfoTracksBucket bucket) {
        final int lastPlayedPosition = bucket.lastPlayedPosition();
        final TrackItem lastPlayedTrack = bucket.stationTracks().get(lastPlayedPosition).getTrack();

        if (lastPlayedTrack.isPlaying()) {
            layout.scrollToPositionWithOffset(lastPlayedPosition, itemView.getWidth());

        } else if (lastPlayedPosition != Consts.NOT_SET) {
            layout.scrollToPositionWithOffset(lastPlayedPosition + 1, itemView.getWidth());
        }
    }

    private void addTracksToAdapter(List<StationInfoTrack> tracks) {
        adapter.clear();

        for (StationInfoTrack track : tracks) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }

}

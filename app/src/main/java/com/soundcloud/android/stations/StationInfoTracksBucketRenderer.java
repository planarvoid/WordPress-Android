package com.soundcloud.android.stations;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;

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
        initCarousel(ButterKnife.findById(view, R.id.station_tracks_carousel));
        return view;
    }

    void updateNowPlaying(Urn currentlyPlayingUrn) {
        boolean updated = false;
        for (int i = 0; i < adapter.getItemCount(); i++) {
            final TrackItem track = adapter.getItem(i).getTrack();
            final boolean isCurrent = track.getUrn().equals(currentlyPlayingUrn);

            if (track.isPlaying() || isCurrent) {
                track.setIsPlaying(isCurrent);
                updated = true;
            }

            if (isCurrent) {
                recyclerView.smoothScrollToPosition(i);
            }
        }
        if (updated) {
            adapter.notifyDataSetChanged();
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
        layout.scrollToPositionWithOffset(scrollPosition(stationInfoTracksBucket), itemView.getWidth());
    }

    @VisibleForTesting
    int scrollPosition(StationInfoTracksBucket bucket) {
        final int lastPlayedPosition = bucket.lastPlayedPosition();
        final List<StationInfoTrack> tracksList = bucket.stationTracks();

        if (lastPlayedPosition >= 0
                && lastPlayedPosition < tracksList.size()
                && tracksList.get(lastPlayedPosition).getTrack().isPlaying()) {
            return lastPlayedPosition;
        }

        final boolean isLastTrack = lastPlayedPosition + 1 >= tracksList.size();
        final boolean wasNeverPlayed = lastPlayedPosition == Stations.NEVER_PLAYED;

        return wasNeverPlayed || isLastTrack ? 0 : lastPlayedPosition + 1;
    }

    private void addTracksToAdapter(List<StationInfoTrack> tracks) {
        adapter.clear();

        for (StationInfoTrack track : tracks) {
            adapter.addItem(track);
        }
        adapter.notifyDataSetChanged();
    }

}

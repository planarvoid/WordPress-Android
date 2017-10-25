package com.soundcloud.android.stations;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.java.collections.Pair;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class StationTrackRenderer implements CellRenderer<StationInfoTrack> {

    private final ImageOperations imageOperations;
    private final StationInfoClickListener clickListener;
    private final TrackItemMenuPresenter menuPresenter;
    private final Navigator navigator;

    private final View.OnClickListener onTrackClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            clickListener.onTrackClicked(view.getContext(), (int) view.getTag());
        }
    };

    private final View.OnClickListener onOverflowMenuClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final Pair<TrackItem, Integer> pair = (Pair<TrackItem, Integer>) view.getTag();
            menuPresenter.show(getFragmentActivity(view.getContext()), view, pair.first(), pair.second());
        }
    };

    StationTrackRenderer(StationInfoClickListener clickListener,
                         @Provided ImageOperations imageOperations,
                         @Provided TrackItemMenuPresenter menuPresenter,
                         @Provided Navigator navigator) {
        this.clickListener = clickListener;
        this.imageOperations = imageOperations;
        this.menuPresenter = menuPresenter;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.station_track_item, parent, false);
        view.setOnClickListener(onTrackClicked);
        view.findViewById(R.id.overflow_button).setOnClickListener(onOverflowMenuClicked);
        return view;
    }

    @Override
    public void bindItemView(int position, View view, List<StationInfoTrack> items) {
        final TrackItem track = items.get(position).getTrack();

        loadTrackArtwork(view, track);
        bindTrackTitle(view, track.title());
        bindTrackArtist(view, track.creatorName(), track.creatorUrn(), track.isPlaying());

        view.findViewById(R.id.overflow_button).setTag(Pair.of(track, position));
        view.setTag(position);
    }

    private void bindTrackTitle(View view, String title) {
        TextView recommendationTitle = view.findViewById(R.id.recommendation_title);
        recommendationTitle.setText(title);
    }

    private void bindTrackArtist(View view, String creatorName, final Urn creatorUrn, boolean isPlaying) {
        final TextView artist = view.findViewById(R.id.recommendation_artist);

        if (isPlaying) {
            artist.setVisibility(View.GONE);
        } else {
            artist.setText(creatorName);
            artist.setVisibility(View.VISIBLE);
            artist.setOnClickListener(v -> navigator.navigateTo(NavigationTarget.forProfile(creatorUrn)));
        }

        view.findViewById(R.id.recommendation_now_playing).setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    private void loadTrackArtwork(View view, TrackItem track) {
        imageOperations.displayInAdapterView(track.getUrn(),
                                             track.getImageUrlTemplate(),
                                             ApiImageSize.getFullImageSize(view.getResources()),
                                             view.findViewById(R.id.recommendation_artwork),
                                             false);
    }

}

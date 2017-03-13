package com.soundcloud.android.stations;

import static butterknife.ButterKnife.findById;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
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

    private final Navigator navigator;
    private final ImageOperations imageOperations;
    private final StationInfoClickListener clickListener;
    private final TrackItemMenuPresenter menuPresenter;

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
                         @Provided Navigator navigator,
                         @Provided ImageOperations imageOperations,
                         @Provided TrackItemMenuPresenter menuPresenter) {
        this.clickListener = clickListener;
        this.navigator = navigator;
        this.imageOperations = imageOperations;
        this.menuPresenter = menuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recommendation_item, parent, false);
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
        ButterKnife.<TextView>findById(view, R.id.recommendation_title).setText(title);
    }

    private void bindTrackArtist(View view, String creatorName, final Urn creatorUrn, boolean isPlaying) {
        final TextView artist = findById(view, R.id.recommendation_artist);

        if (isPlaying) {
            artist.setVisibility(View.GONE);
        } else {
            artist.setText(creatorName);
            artist.setVisibility(View.VISIBLE);
            artist.setOnClickListener(v -> navigator.legacyOpenProfile(artist.getContext(), creatorUrn));
        }

        findById(view, R.id.recommendation_now_playing).setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    private void loadTrackArtwork(View view, TrackItem track) {
        imageOperations.displayInAdapterView(track, ApiImageSize.getFullImageSize(view.getResources()),
                                             ButterKnife.findById(view, R.id.recommendation_artwork)
        );
    }

}

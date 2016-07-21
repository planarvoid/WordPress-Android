package com.soundcloud.android.stations;

import static butterknife.ButterKnife.findById;

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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class StationTrackRenderer implements CellRenderer<StationInfoTrack> {

    private final Navigator navigator;
    private final ImageOperations imageOperations;
    private final StationInfoClickListener clickListener;

    private final View.OnClickListener onTrackClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            clickListener.onTrackClicked(view.getContext(), (int) view.getTag());
        }
    };

    StationTrackRenderer(StationInfoClickListener clickListener,
                         @Provided Navigator navigator,
                         @Provided ImageOperations imageOperations) {
        this.clickListener = clickListener;
        this.navigator = navigator;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recommendation_item, parent, false);
        view.setOnClickListener(onTrackClicked);

        return view;
    }

    @Override
    public void bindItemView(int position, View view, List<StationInfoTrack> items) {
        final StationInfoTrack track = items.get(position);

        loadTrackArtwork(view, track);
        bindTrackTitle(view, track.getTitle());
        bindTrackArtist(view, track.getCreator(), track.getCreatorUrn(), track.isPlaying());
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
            artist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigator.openProfile(artist.getContext(), creatorUrn);
                }
            });
        }

        findById(view, R.id.recommendation_now_playing).setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    private void loadTrackArtwork(View view, StationInfoTrack track) {
        imageOperations.displayInAdapterView(track, ApiImageSize.getFullImageSize(view.getResources()),
                                             ButterKnife.<ImageView>findById(view, R.id.recommendation_artwork)
        );
    }

}

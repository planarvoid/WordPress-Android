package com.soundcloud.android.discovery;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
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
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecommendationRenderer implements CellRenderer<Recommendation> {

    private final ImageOperations imageOperations;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private final Navigator navigator;
    private final TrackRecommendationListener listener;

    public RecommendationRenderer(TrackRecommendationListener listener,
                                  @Provided ImageOperations imageOperations,
                                  @Provided TrackItemMenuPresenter trackItemMenuPresenter,
                                  @Provided Navigator navigator) {
        this.listener = listener;
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.recommendation_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View view, final List<Recommendation> recommendations) {
        final Recommendation viewModel = recommendations.get(position);
        final TrackItem track = viewModel.getTrack();

        loadTrackArtwork(view, track);
        bindTrackTitle(view, track.getTitle());
        bindTrackArtist(view, track.getCreatorName(), track.getCreatorUrn(), viewModel.isPlaying());
        bindNowPlaying(view, viewModel.isPlaying());
        setOnClickListener(viewModel, view);
        setOverflowMenuClickListener(ButterKnife.<ImageView>findById(view, R.id.overflow_button), track, position);
        showHighTierIndicator(view, track);
    }

    private void showHighTierIndicator(View view, TrackItem track) {
        ButterKnife.findById(view, R.id.high_tier_label).setVisibility(isFullHighTierTrack(track) ? VISIBLE : GONE);
    }

    private void bindTrackTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.recommendation_title).setText(title);
    }

    private void bindTrackArtist(View view, String creatorName, final Urn creatorUrn, boolean isPlaying) {
        final TextView artist = ButterKnife.findById(view, R.id.recommendation_artist);

        if (isPlaying) {
            artist.setVisibility(GONE);
        } else {
            artist.setText(creatorName);
            artist.setVisibility(VISIBLE);
            artist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigator.openProfile(artist.getContext(), creatorUrn);
                }
            });
        }
    }

    private void bindNowPlaying(View view, boolean isPlaying) {
        ButterKnife.findById(view, R.id.recommendation_now_playing).setVisibility(isPlaying ? VISIBLE : GONE);
    }

    private void setOnClickListener(final Recommendation recommendation, View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onTrackClicked(recommendation.getSeedUrn(), recommendation.getTrackUrn());

            }
        });
    }

    private void loadTrackArtwork(View view, TrackItem track) {
        imageOperations.displayInAdapterView(track, ApiImageSize.getFullImageSize(view.getResources()),
                                             ButterKnife.<ImageView>findById(view, R.id.recommendation_artwork)
        );
    }

    private void setOverflowMenuClickListener(final ImageView button, final TrackItem trackItem, final int position) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackItemMenuPresenter.show(getFragmentActivity(button), button, trackItem, position);
            }
        });
    }

}

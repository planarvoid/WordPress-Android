package com.soundcloud.android.discovery;

import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Collections.singleton;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Provider;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecommendationRenderer implements CellRenderer<Recommendation> {
    public static final int NUM_SEED_TRACKS = 1;

    private final Screen screen;
    private final ImageOperations imageOperations;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

    public RecommendationRenderer(Screen screen,
                                  @Provided ImageOperations imageOperations,
                                  @Provided TrackItemMenuPresenter trackItemMenuPresenter,
                                  @Provided PlaybackInitiator playbackInitiator,
                                  @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.screen = screen;
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.playbackInitiator = playbackInitiator;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
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
        bindTrackArtist(view, track.getCreatorName(), viewModel.isPlaying());
        bindNowPlaying(view, viewModel.isPlaying());
        setOnClickListener(position, view, recommendations, viewModel);
        setOverflowMenuClickListener(ButterKnife.<ImageView>findById(view, R.id.overflow_button), track, position);
    }

    private void bindTrackTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.recommendation_title).setText(title);
    }

    private void bindTrackArtist(View view, String creatorName, boolean isPlaying) {
        final TextView artist = ButterKnife.findById(view, R.id.recommendation_artist);

        if (isPlaying) {
            artist.setVisibility(View.GONE);
        } else {
            artist.setText(creatorName);
            artist.setVisibility(View.VISIBLE);
        }
    }

    private void bindNowPlaying(View view, boolean isPlaying) {
        ButterKnife.findById(view, R.id.recommendation_now_playing).setVisibility(isPlaying ? View.VISIBLE : View.GONE);
    }

    private void setOnClickListener(final int position,
                                    View view,
                                    final List<Recommendation> recommendations,
                                    final Recommendation viewModel) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playbackInitiator.playTracks(
                        toPlayQueue(viewModel.getSeedUrn(), recommendations),
                        position + NUM_SEED_TRACKS,
                        new PlaySessionSource(screen))
                        .subscribe(expandPlayerSubscriberProvider.get());
            }
        });
    }

    private void loadTrackArtwork(View view, TrackItem track) {
        imageOperations.displayInAdapterView(
                track,
                ApiImageSize.getFullImageSize(view.getResources()),
                ButterKnife.<ImageView>findById(view, R.id.recommendation_artwork)
        );
    }

    private void setOverflowMenuClickListener(final ImageView button, final TrackItem trackItem, final int position) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, trackItem, position);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Urn> toPlayQueue(Urn seedUrn, List<Recommendation> recommendations) {
        return newArrayList(concat(
                singleton(seedUrn),
                transform(recommendations, Recommendation.TO_TRACK_URN)));
    }
}

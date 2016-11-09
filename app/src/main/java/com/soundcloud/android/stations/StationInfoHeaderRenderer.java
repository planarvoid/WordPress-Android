package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationTypes.getHumanReadableType;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleBlurredImageLoader;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

@AutoFactory
class StationInfoHeaderRenderer implements CellRenderer<StationInfoHeader> {

    private final View.OnClickListener playButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            clickListener.onPlayButtonClicked(view.getContext());
        }
    };

    private final ToggleButton.OnCheckedChangeListener toggleLikeListener = new ToggleButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isLiked) {
            clickListener.onLikeToggled(compoundButton.getContext(), isLiked);
        }
    };

    private final StationInfoClickListener clickListener;
    private final SimpleBlurredImageLoader simpleBlurredImageLoader;
    private final ImageOperations imageOperations;
    private final Resources resources;

    StationInfoHeaderRenderer(StationInfoClickListener listener,
                              @Provided SimpleBlurredImageLoader simpleBlurredImageLoader,
                              @Provided Resources resources,
                              @Provided ImageOperations imageOperations) {
        this.clickListener = listener;
        this.simpleBlurredImageLoader = simpleBlurredImageLoader;
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.station_info_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<StationInfoHeader> items) {
        StationInfoHeader info = items.get(position);

        bindArtwork(info, itemView);
        bindTextViews(info, itemView);
        bindButtons(info, itemView);
    }

    private void bindButtons(StationInfoHeader info, View itemView) {
        final View playButton = ButterKnife.findById(itemView, R.id.btn_play);
        playButton.setVisibility(View.VISIBLE);
        playButton.setOnClickListener(playButtonClickListener);

        final ToggleButton likeButton = ButterKnife.findById(itemView, R.id.toggle_like);
        likeButton.setChecked(info.isLiked());
        likeButton.setOnCheckedChangeListener(toggleLikeListener);

    }

    private void bindTextViews(StationInfoHeader info, View itemView) {
        ButterKnife.<TextView>findById(itemView, R.id.station_type)
                .setText(resources.getString(R.string.stations_home_station_based_on,
                                             getHumanReadableType(resources, info.getType())));
        ButterKnife.<TextView>findById(itemView, R.id.station_title).setText(info.getTitle());

        final List<String> mostPlayedArtists = info.getMostPlayedArtists();
        final TextView description = ButterKnife.findById(itemView, R.id.station_desc);
        final boolean artistsPresent = mostPlayedArtists.size() > 0;
        if (artistsPresent) {
            description.setText(buildStationDescription(mostPlayedArtists));
        }
        description.setVisibility(artistsPresent ? View.VISIBLE : View.GONE);
    }

    private SpannableString buildStationDescription(List<String> mostPlayed) {
        final String descriptionText = resources.getString(R.string.stations_home_description,
                                                           artistString(mostPlayed));
        final SpannableString descriptionSpan = new SpannableString(descriptionText);

        int lastIndexOf = 0;
        for (String artist : mostPlayed) {
            int start = descriptionText.indexOf(artist, lastIndexOf);
            lastIndexOf = start + artist.length();
            descriptionSpan.setSpan(new StyleSpan(Typeface.BOLD), start, lastIndexOf, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return descriptionSpan;
    }

    private void bindArtwork(StationInfoHeader info, final View headerView) {
        final ApiImageSize artworkSize = ApiImageSize.getFullImageSize(headerView.getResources());
        final ImageView artworkView = ButterKnife.findById(headerView, R.id.artwork);
        final ImageView blurredArtworkView = ButterKnife.findById(headerView, R.id.blurred_background);

        imageOperations.displayWithPlaceholder(info, artworkSize, artworkView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            simpleBlurredImageLoader.displayBlurredArtwork(info, blurredArtworkView);
        }
    }

    private String artistString(List<String> mostPlayed) {
        StringBuilder sb = new StringBuilder();
        for (String artist : mostPlayed) {
            sb.append(artist).append(", ");
        }
        return sb.toString().substring(0, sb.length() - 2);
    }
}

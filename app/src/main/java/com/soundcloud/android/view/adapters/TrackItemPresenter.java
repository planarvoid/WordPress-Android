package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemPresenter implements CellPresenter<PropertySet> {

    private final ImageOperations imageOperations;

    private Urn playingTrack = Urn.NOT_SET;

    @Inject
    public TrackItemPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.track_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        final PropertySet track = trackItems.get(position);
        getTextView(itemView, R.id.list_item_header).setText(track.get(PlayableProperty.CREATOR_NAME));
        getTextView(itemView, R.id.list_item_subheader).setText(track.get(PlayableProperty.TITLE));
        final String formattedDuration = ScTextUtils.formatTimestamp(track.get(PlayableProperty.DURATION), TimeUnit.MILLISECONDS);
        getTextView(itemView, R.id.list_item_right_info).setText(formattedDuration);

        showRelevantAdditionalInformation(itemView, track);
        toggleReposterView(itemView, track);

        loadArtwork(itemView, track);
    }

    private void loadArtwork(View itemView, PropertySet track) {
        imageOperations.displayInAdapterView(
                track.get(PlayableProperty.URN), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void toggleReposterView(View itemView, PropertySet track) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        if (track.contains(PlayableProperty.REPOSTER)) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(track.get(PlayableProperty.REPOSTER));
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void showRelevantAdditionalInformation(View itemView, PropertySet track) {
        hideAllAdditionalInformation(itemView);
        if (track.get(PlayableProperty.URN).equals(playingTrack)) {
            showNowPlaying(itemView);
        } else if (isPrivateTrack(track)) {
            showPrivateIndicator(itemView);
        } else {
            showPlayCount(itemView, track);
        }
    }

    private Boolean isPrivateTrack(PropertySet track) {
        return track.contains(PlayableProperty.IS_PRIVATE) && track.get(PlayableProperty.IS_PRIVATE);
    }

    private void hideAllAdditionalInformation(View itemView) {
        getTextView(itemView, R.id.list_item_counter).setVisibility(View.INVISIBLE);
        getTextView(itemView, R.id.now_playing).setVisibility(View.INVISIBLE);
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);
    }

    private void showNowPlaying(View itemView) {
        getTextView(itemView, R.id.now_playing).setVisibility(View.VISIBLE);
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void showPlayCount(View itemView, PropertySet track) {
        final int playCount = track.getOrElse(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
        if (hasPlayCount(playCount)) {
            final TextView playCountText = getTextView(itemView, R.id.list_item_counter);
            playCountText.setVisibility(View.VISIBLE);
            playCountText.setText(ScTextUtils.formatNumberWithCommas(playCount));
        }
    }

    private boolean hasPlayCount(int playCount) {
        return playCount > 0;
    }

    public void setPlayingTrack(@NotNull Urn playingTrack) {
        this.playingTrack = playingTrack;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

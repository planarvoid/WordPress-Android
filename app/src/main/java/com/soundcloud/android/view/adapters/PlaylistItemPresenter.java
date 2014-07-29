package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistItemPresenter implements CellPresenter<PropertySet> {

    private final LayoutInflater layoutInflater;
    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    public PlaylistItemPresenter(LayoutInflater layoutInflater, Resources resources, ImageOperations imageOperations) {
        this.layoutInflater = layoutInflater;
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.playlist_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> playlists) {
        final PropertySet playlist = playlists.get(position);
        getTextView(itemView, R.id.list_item_header).setText(playlist.get(PlayableProperty.CREATOR_NAME));
        getTextView(itemView, R.id.list_item_subheader).setText(playlist.get(PlayableProperty.TITLE));

        showTrackCount(itemView, playlist);
        showReposter(itemView, playlist);
        showAdditionalInformation(itemView, playlist);

        loadArtwork(itemView, playlist);
    }

    private void showTrackCount(View itemView, PropertySet propertySet) {
        final int trackCount = propertySet.get(PlaylistProperty.TRACK_COUNT);
        final String numberOfTracks = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        getTextView(itemView, R.id.list_item_right_info).setText(numberOfTracks);
    }

    private void showReposter(View itemView, PropertySet propertySet) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        if (propertySet.contains(PlayableProperty.REPOSTER)) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(propertySet.get(PlayableProperty.REPOSTER));
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void showAdditionalInformation(View itemView, PropertySet playlist) {
        hideAllAdditionalInformation(itemView);
        if (isPrivatePlaylist(playlist)) {
            showPrivateIndicator(itemView);
        } else {
            showLikeCount(itemView, playlist);
        }
    }

    private void hideAllAdditionalInformation(View itemView) {
        getTextView(itemView, R.id.list_item_counter).setVisibility(View.GONE);
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);
    }

    private Boolean isPrivatePlaylist(PropertySet playlist) {
        return playlist.contains(PlayableProperty.IS_PRIVATE) && playlist.get(PlayableProperty.IS_PRIVATE);
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void loadArtwork(View itemView, PropertySet playlist) {
        imageOperations.displayInAdapterView(
                playlist.get(PlayableProperty.URN), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void showLikeCount(View itemView, PropertySet propertySet) {
        final TextView likesCountText = getTextView(itemView, R.id.list_item_counter);
        final int likesCount = propertySet.get(PlayableProperty.LIKES_COUNT);
        if (hasLike(likesCount)) {
            likesCountText.setVisibility(View.VISIBLE);
            likesCountText.setText(ScTextUtils.formatNumberWithCommas(likesCount));
            final Drawable heartIcon = likesCountText.getCompoundDrawables()[0];
            heartIcon.setLevel(propertySet.get(PlayableProperty.IS_LIKED) ? 1 : 0);
        }
    }

    private boolean hasLike(int likesCount) {
        return likesCount > 0;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

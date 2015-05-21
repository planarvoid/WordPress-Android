package com.soundcloud.android.view.adapters;

import com.google.common.base.Optional;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistItemPresenter implements CellPresenter<PlaylistItem> {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private boolean allowOfflineOptions;

    @Inject
    public PlaylistItemPresenter(Resources resources, ImageOperations imageOperations,
                                 PlaylistItemMenuPresenter playlistItemMenuPresenter) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        final PlaylistItem playlist = playlists.get(position);
        getTextView(itemView, R.id.list_item_header).setText(playlist.getCreatorName());
        getTextView(itemView, R.id.list_item_subheader).setText(playlist.getTitle());

        showTrackCount(itemView, playlist);
        showReposter(itemView, playlist);
        showAdditionalInformation(itemView, playlist);

        loadArtwork(itemView, playlist);
        setupOverFlow(itemView.findViewById(R.id.overflow_button), playlist);
    }

    public void allowOfflineOptions() {
        this.allowOfflineOptions = true;
    }

    private void setupOverFlow(final View button, final PlaylistItem playlist) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistItemMenuPresenter.show(button, playlist, allowOfflineOptions);
            }
        });
    }

    private void showTrackCount(View itemView, PlaylistItem playlist) {
        final int trackCount = playlist.getTrackCount();
        final String numberOfTracks = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        getTextView(itemView, R.id.list_item_right_info).setText(numberOfTracks);
    }

    private void showReposter(View itemView, PlaylistItem playlist) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        final Optional<String> optionalReposter = playlist.getReposter();
        if (optionalReposter.isPresent()) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(optionalReposter.get());
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void showAdditionalInformation(View itemView, PlaylistItem playlist) {
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

    private Boolean isPrivatePlaylist(PlaylistItem playlist) {
        return playlist.isPrivate();
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void loadArtwork(View itemView, PlaylistItem playlist) {
        imageOperations.displayInAdapterView(
                playlist.getEntityUrn(), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void showLikeCount(View itemView, PlaylistItem playlist) {
        final TextView likesCountText = getTextView(itemView, R.id.list_item_counter);
        final int likesCount = playlist.getLikesCount();
        if (hasLike(likesCount)) {
            likesCountText.setVisibility(View.VISIBLE);
            likesCountText.setText(ScTextUtils.formatNumberWithCommas(likesCount));
            final Drawable heartIcon = likesCountText.getCompoundDrawables()[0];
            heartIcon.setLevel(playlist.isLiked() ? 1 : 0);
        }
    }

    private boolean hasLike(int likesCount) {
        return likesCount > 0;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

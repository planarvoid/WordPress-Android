package com.soundcloud.android.olddiscovery.recommendedplaylists;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.image.StyledImageView;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class CarouselPlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    protected final ImageOperations imageOperations;
    private PlaylistListener playlistListener;

    @Inject
    CarouselPlaylistItemRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.carousel_playlist_item_fixed_width, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistItem> list) {
        final PlaylistItem playlist = list.get(position);

        setImage(view, playlist);
        setTitle(view, playlist.title());
        setTrackCount(view, playlist);
        setCreator(view, playlist.creatorName());
        setPrivate(view, playlist.isPrivate());
        setLiked(view, playlist.isUserLike());

        view.setOnClickListener(goToPlaylist(playlist, position));
        findById(view, R.id.overflow_button).setVisibility(View.GONE);
    }

    private void setLiked(View view, boolean isLiked) {
        ButterKnife.findById(view, R.id.like_indicator)
                   .setVisibility(isLiked ? View.VISIBLE : View.GONE);
    }

    private void setPrivate(View view, boolean isPrivate) {
        ButterKnife.findById(view, R.id.private_indicator)
                   .setVisibility(isPrivate ? View.VISIBLE : View.GONE);
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setCreator(View view, String creatorName) {
        ButterKnife.<TextView>findById(view, R.id.secondary_text).setText(creatorName);
    }

    private void setImage(View view, ImageResource imageResource) {
        final StyledImageView styledImageView = findById(view, R.id.artwork);
        styledImageView.showWithoutPlaceholder(imageResource.getImageUrlTemplate(), Optional.of(ImageStyle.SQUARE), Optional.of(imageResource.getUrn()), imageOperations);
    }

    private void setTrackCount(View view, PlaylistItem playlist) {
        final TextView trackCount = findById(view, R.id.track_count);
        trackCount.setText(String.valueOf(playlist.trackCount()));
    }

    private View.OnClickListener goToPlaylist(final PlaylistItem playlist, final int position) {
        return view -> playlistListener.onPlaylistClick(view.getContext(), playlist, position);
    }

    public void setPlaylistListener(PlaylistListener playlistListener) {
        this.playlistListener = playlistListener;
    }


    public interface PlaylistListener {
        void onPlaylistClick(Context context, PlaylistItem playlist, int position);
    }
}

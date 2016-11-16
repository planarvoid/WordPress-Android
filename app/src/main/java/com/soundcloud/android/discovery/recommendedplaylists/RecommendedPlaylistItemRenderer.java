package com.soundcloud.android.discovery.recommendedplaylists;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class RecommendedPlaylistItemRenderer implements CellRenderer<PlaylistItem> {
    interface PlaylistListener {
        void onPlaylistClick(Context context, PlaylistItem playlist, int position);
    }

    private final ImageOperations imageOperations;
    private final Resources resources;

    private PlaylistListener playlistListener;

    @Inject
    RecommendedPlaylistItemRenderer(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.collection_recently_played_playlist_item_fixed_width, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistItem> list) {
        final PlaylistItem playlist = list.get(position);

        setImage(view, playlist);
        setTitle(view, playlist.getTitle());
        setTrackCount(view, playlist);
        setCreator(view, playlist.getCreatorName());

        view.setOnClickListener(goToPlaylist(playlist, position));
        findById(view, R.id.overflow_button).setVisibility(View.GONE);
    }

    void setPlaylistListener(PlaylistListener playlistListener) {
        this.playlistListener = playlistListener;
    }


    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setCreator(View view, String creatorName) {
        ButterKnife.<TextView>findById(view, R.id.recently_played_type).setText(creatorName);
    }

    private void setImage(View view, ImageResource imageResource) {
        final ImageView artwork = findById(view, R.id.artwork);
        imageOperations.displayInAdapterView(imageResource, ApiImageSize.getFullImageSize(resources), artwork);
    }

    private void setTrackCount(View view, PlaylistItem playlist) {
        final TextView trackCount = findById(view, R.id.track_count);
        trackCount.setText(String.valueOf(playlist.getTrackCount()));
    }

    private View.OnClickListener goToPlaylist(final PlaylistItem playlist, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playlistListener.onPlaylistClick(view.getContext(), playlist, position);
            }
        };
    }
}

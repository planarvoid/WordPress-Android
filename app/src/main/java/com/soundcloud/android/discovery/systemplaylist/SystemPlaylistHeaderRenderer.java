package com.soundcloud.android.discovery.systemplaylist;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.SimpleBlurredImageLoader;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.java.optional.Optional;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class SystemPlaylistHeaderRenderer implements CellRenderer<SystemPlaylistItem.Header> {
    private final Listener listener;
    private final ImageOperations imageOperations;
    private final SimpleBlurredImageLoader simpleBlurredImageLoader;

    interface Listener {
        void playClicked();
    }

    SystemPlaylistHeaderRenderer(Listener listener,
                                 @Provided ImageOperations imageOperations,
                                 @Provided SimpleBlurredImageLoader simpleBlurredImageLoader) {
        this.listener = listener;
        this.imageOperations = imageOperations;
        this.simpleBlurredImageLoader = simpleBlurredImageLoader;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.system_playlist_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SystemPlaylistItem.Header> items) {
        final SystemPlaylistItem.Header item = items.get(position);

        bindArtwork(item, itemView);
        bindPlayButton(item, itemView);
        bindTextViews(item, itemView);
    }

    private void bindArtwork(SystemPlaylistItem.Header item, View itemView) {
        final SystemPlaylistArtworkView artworkView = ButterKnife.findById(itemView, R.id.artwork);
        final ImageView blurredArtworkView = ButterKnife.findById(itemView, R.id.blurred_background);

        artworkView.bindWithoutAnimation(imageOperations, item);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && item.image().isPresent()) {
            simpleBlurredImageLoader.displayBlurredArtwork(item.image().get(), blurredArtworkView);
        } else {
            imageOperations.displayDefaultPlaceholder(blurredArtworkView);
        }
    }

    private void bindPlayButton(SystemPlaylistItem.Header item, View itemView) {
        final View playButton = itemView.findViewById(R.id.btn_play);
        if (item.shouldShowPlayButton()) {
            playButton.setVisibility(View.VISIBLE);
            playButton.setOnClickListener(v -> listener.playClicked());
        } else {
            playButton.setVisibility(View.GONE);
        }
    }

    private void bindTextViews(SystemPlaylistItem.Header item, View itemView) {
        final TextView title = (TextView) itemView.findViewById(R.id.system_playlist_title);
        final TextView description = (TextView) itemView.findViewById(R.id.system_playlist_description);
        final TextView duration = (TextView) itemView.findViewById(R.id.system_playlist_duration);
        final TextView updatedAt = (TextView) itemView.findViewById(R.id.system_playlist_updated_at);

        bindOptionalTextView(title, item.title());
        bindOptionalTextView(description, item.description());
        bindOptionalTextView(updatedAt, item.updatedAt());
        duration.setText(item.metadata());
    }

    private void bindOptionalTextView(TextView view, Optional<String> text) {
        if (text.isPresent()) {
            view.setText(text.get());
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.INVISIBLE);
        }

    }
}

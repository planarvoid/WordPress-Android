package com.soundcloud.android.discovery.recommendedplaylists;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class RecommendedPlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;

    @Inject
    RecommendedPlaylistItemRenderer(ImageOperations imageOperations,
                         Resources resources,
                         Navigator navigator,
                         ScreenProvider screenProvider,
                         EventBus eventBus) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
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

        view.setOnClickListener(goToPlaylist(playlist));
        findById(view, R.id.overflow_button).setVisibility(View.GONE);
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

    private View.OnClickListener goToPlaylist(final PlaylistItem playlist) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Urn urn = playlist.getUrn();
                Screen lastScreen = screenProvider.getLastScreen();
                eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(urn, lastScreen));
                navigator.legacyOpenPlaylist(view.getContext(), urn, lastScreen);
            }
        };
    }
}

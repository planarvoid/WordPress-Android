package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
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
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedPlaylistRenderer implements CellRenderer<RecentlyPlayedPlayableItem> {

    private static final int TOUCH_DELEGATE_DP = 8;

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final boolean fixedWidth;

    RecentlyPlayedPlaylistRenderer(boolean fixedWidth,
                                   @Provided ImageOperations imageOperations,
                                   @Provided Resources resources,
                                   @Provided Navigator navigator,
                                   @Provided ScreenProvider screenProvider,
                                   @Provided EventBus eventBus,
                                   @Provided PlaylistItemMenuPresenter playlistItemMenuPresenter) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.carousel_playlist_item_fixed_width
                     : R.layout.collection_recently_played_playlist_item_variable_width;

        return LayoutInflater.from(parent.getContext())
                             .inflate(layout, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<RecentlyPlayedPlayableItem> list) {
        final RecentlyPlayedPlayableItem playlist = list.get(position);

        setImage(view, playlist);
        setTitle(view, playlist.getTitle());
        setTrackCount(view, playlist);
        setType(view, playlist.isAlbum()
                      ? R.string.collections_recently_played_album
                      : R.string.collections_recently_played_playlist);
        setOfflineState(view, playlist.getOfflineState());

        view.setOnClickListener(goToPlaylist(playlist));
        setupOverFlow(view.findViewById(R.id.overflow_button), playlist);
    }

    private void setOfflineState(View view, Optional<OfflineState> offlineState) {
        final DownloadImageView downloadImageView = ButterKnife.findById(view, R.id.item_download_state);
        if (offlineState.isPresent()) {
            downloadImageView.setState(offlineState.get());
            downloadImageView.setVisibility(View.VISIBLE);
        } else {
            downloadImageView.setVisibility(View.GONE);
        }
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setType(View view, int resId) {
        ButterKnife.<TextView>findById(view, R.id.secondary_text).setText(resId);
    }

    private void setImage(View view, ImageResource imageResource) {
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        imageOperations.displayInAdapterView(imageResource, getImageSize(), artwork);
    }

    private void setTrackCount(View view, RecentlyPlayedPlayableItem playlist) {
        final TextView trackCount = (TextView) view.findViewById(R.id.track_count);
        trackCount.setText(String.valueOf(playlist.getTrackCount()));
    }

    private ApiImageSize getImageSize() {
        return ApiImageSize.getFullImageSize(resources);
    }

    private View.OnClickListener goToPlaylist(final RecentlyPlayedPlayableItem playlist) {
        return view -> {
            Urn urn = playlist.getUrn();
            Screen lastScreen = screenProvider.getLastScreen();
            eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(urn, lastScreen));
            navigator.legacyOpenPlaylist(view.getContext(), urn, lastScreen);
        };
    }

    private void setupOverFlow(final View button, final RecentlyPlayedPlayableItem playlistItem) {
        button.setOnClickListener(v -> playlistItemMenuPresenter.show(button, playlistItem.getUrn()));
        ViewUtils.extendTouchArea(button, TOUCH_DELEGATE_DP);
    }

}

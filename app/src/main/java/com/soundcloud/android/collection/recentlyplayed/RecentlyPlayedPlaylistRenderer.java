package com.soundcloud.android.collection.recentlyplayed;

import static butterknife.ButterKnife.findById;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.collection.PlaylistItemIndicatorsView;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.image.StyledImageView;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.OverflowButtonBackground;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.OverflowAnchorImageView;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedPlaylistRenderer implements CellRenderer<RecentlyPlayedPlayableItem> {

    private final boolean fixedWidth;
    private final ImageOperations imageOperations;
    private final Navigator navigator;
    private final ScreenProvider screenProvider;
    private final EventBus eventBus;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final PlaylistItemIndicatorsView playlistItemIndicatorsView;

    RecentlyPlayedPlaylistRenderer(boolean fixedWidth,
                                   @Provided ImageOperations imageOperations,
                                   @Provided Navigator navigator,
                                   @Provided ScreenProvider screenProvider,
                                   @Provided EventBus eventBus,
                                   @Provided PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                   @Provided PlaylistItemIndicatorsView playlistItemIndicatorsView) {
        this.fixedWidth = fixedWidth;
        this.imageOperations = imageOperations;
        this.navigator = navigator;
        this.screenProvider = screenProvider;
        this.eventBus = eventBus;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.playlistItemIndicatorsView = playlistItemIndicatorsView;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        int layout = fixedWidth
                     ? R.layout.collection_recently_played_playlist_item_fixed_width
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

        findById(view, R.id.carousel_playlist_item).setOnClickListener(goToPlaylist(playlist));
        setupOverFlow(findById(view, R.id.overflow_button), playlist);
        playlistItemIndicatorsView.setupView(view, playlist.isPrivate(), playlist.isLiked(), playlist.getOfflineState());
    }

    private void setTitle(View view, String title) {
        ButterKnife.<TextView>findById(view, R.id.title).setText(title);
    }

    private void setType(View view, int resId) {
        ButterKnife.<TextView>findById(view, R.id.secondary_text).setText(resId);
    }

    private void setImage(View view, ImageResource imageResource) {
        final StyledImageView styledImageView = findById(view, R.id.artwork);
        styledImageView.showWithoutPlaceholder(imageResource.getImageUrlTemplate(), Optional.of(ImageStyle.SQUARE), Optional.of(imageResource.getUrn()), imageOperations);
    }

    private void setTrackCount(View view, RecentlyPlayedPlayableItem playlist) {
        final TextView trackCount = findById(view, R.id.track_count);
        trackCount.setText(String.valueOf(playlist.getTrackCount()));
    }

    private View.OnClickListener goToPlaylist(final RecentlyPlayedPlayableItem playlist) {
        return view -> {
            Urn urn = playlist.getUrn();
            Screen lastScreen = screenProvider.getLastScreen();
            eventBus.publish(EventQueue.TRACKING, CollectionEvent.forRecentlyPlayed(urn, lastScreen));
            navigator.navigateTo(getFragmentActivity(view), NavigationTarget.forLegacyPlaylist(urn, lastScreen));
        };
    }

    private void setupOverFlow(final OverflowAnchorImageView button, final RecentlyPlayedPlayableItem playlistItem) {
        button.setOnClickListener(v -> playlistItemMenuPresenter.show(button, playlistItem.getUrn()));
        OverflowButtonBackground.install(button, R.dimen.collection_recently_played_item_overflow_menu_padding);
        ViewUtils.extendTouchArea(button, R.dimen.collection_recently_played_item_overflow_menu_touch_expansion);
    }

}

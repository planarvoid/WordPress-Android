package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;

    @Inject
    public PlaylistItemRenderer(Resources resources,
                                ImageOperations imageOperations,
                                CondensedNumberFormatter numberFormatter,
                                PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                EventBus eventBus,
                                ScreenProvider screenProvider,
                                Navigator navigator) {

        this.resources = resources;
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        bindPlaylistView(playlists.get(position), itemView, Optional.absent(), Optional.absent());
    }

    public void bindPlaylistView(PlaylistItem playlist, View itemView, Optional<Module> module) {
        bindPlaylistView(playlist, itemView, module, Optional.absent());
    }

    public void bindPlaylistView(PlaylistItem playlist, View itemView, Optional<Module> module, Optional<String> clickSource) {
        getTextView(itemView, R.id.list_item_header).setText(playlist.creatorName());
        getTextView(itemView, R.id.list_item_subheader).setText(playlist.title());

        showTrackCount(itemView, playlist);
        showAdditionalInformation(itemView, playlist);

        loadArtwork(itemView, playlist);
        setupOverFlow(itemView.findViewById(R.id.overflow_button), playlist, module, clickSource);
    }

    private void setupOverFlow(final View button,
                               final PlaylistItem playlist,
                               final Optional<Module> module,
                               Optional<String> clickSource) {
        button.setOnClickListener(v -> playlistItemMenuPresenter.show(button,
                                                                      playlist,
                                                                      getEventContextMetaDataBuilder(playlist, module, clickSource)));
    }

    private void showTrackCount(View itemView, PlaylistItem playlist) {
        final int trackCount = playlist.trackCount();
        final String numberOfTracks = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        getTextView(itemView, R.id.list_item_right_info).setText(numberOfTracks);
    }

    private void showAdditionalInformation(View itemView, PlaylistItem playlist) {
        hideAllAdditionalInformation(itemView);
        if (playlist.isPromoted()) {
            showPromotedLabel(itemView, playlist);
        } else if (isPrivatePlaylist(playlist)) {
            showPrivateIndicator(itemView);
        } else if (playlist.isAlbum()) {
            showAlbumTitle(itemView, playlist);
        } else {
            showLikeCount(itemView, playlist);
        }
    }

    private void hideAllAdditionalInformation(View itemView) {
        getTextView(itemView, R.id.list_item_counter).setVisibility(View.GONE);
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);
        getTextView(itemView, R.id.album_title).setVisibility(View.GONE);
        unsetPromoterClickable(itemView);
    }

    private void showPromotedLabel(View itemView, PlaylistItem promoted) {
        if (promoted.hasPromoter()) {
            String label = resources.getString(R.string.promoted_by_promotorname, promoted.promoterName());
            setPromoterClickable(showPromotedLabel(itemView, label), promoted);
        } else {
            showPromotedLabel(itemView, resources.getString(R.string.promoted));
        }
    }

    private TextView showPromotedLabel(View itemView, String label) {
        TextView promoted = getTextView(itemView, R.id.promoted_playlist);
        promoted.setVisibility(View.VISIBLE);
        promoted.setText(label);
        return promoted;
    }

    private void setPromoterClickable(TextView promoter, PlaylistItem item) {
        ViewUtils.setTouchClickable(promoter, new PromoterClickViewListener(item, eventBus, screenProvider, navigator));
    }

    private void unsetPromoterClickable(View itemView) {
        TextView promoter = getTextView(itemView, R.id.promoted_playlist);
        ViewUtils.unsetTouchClickable(promoter);
        promoter.setVisibility(View.GONE);
    }

    private Boolean isPrivatePlaylist(PlaylistItem playlist) {
        return playlist.isPrivate();
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void loadArtwork(View itemView, PlaylistItem playlist) {
        imageOperations.displayInAdapterView(
                playlist, ApiImageSize.getListItemImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void showAlbumTitle(View itemView, PlaylistItem playlist) {
        final TextView albumTitleText = getTextView(itemView, R.id.album_title);
        albumTitleText.setVisibility(View.VISIBLE);
        albumTitleText.setText(playlist.getLabel(itemView.getContext().getResources()));
    }

    private void showLikeCount(View itemView, PlaylistItem playlist) {
        final TextView likesCountText = getTextView(itemView, R.id.list_item_counter);
        final int likesCount = playlist.likesCount();
        if (hasLike(likesCount)) {
            likesCountText.setVisibility(View.VISIBLE);
            likesCountText.setText(numberFormatter.format(likesCount));
            final Drawable heartIcon = likesCountText.getCompoundDrawables()[0];
            heartIcon.setLevel(playlist.isUserLike() ? 1 : 0);
        }
    }

    private EventContextMetadata.Builder getEventContextMetaDataBuilder(PlayableItem item,
                                                                        Optional<Module> module,
                                                                        Optional<String> clickSource) {
        final String screen = screenProvider.getLastScreenTag();

        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(screen)
                                                                         .pageName(screen)
                                                                         .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                 item))
                                                                         .clickSource(clickSource);

        if (module.isPresent()) {
            builder.module(module.get());
        }

        return builder;
    }

    private boolean hasLike(int likesCount) {
        return likesCount > 0;
    }

    protected TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}

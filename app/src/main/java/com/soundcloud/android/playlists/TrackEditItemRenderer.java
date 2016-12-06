package com.soundcloud.android.playlists;

import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
 * Doesn't support Go tracks. Indicators need to be added if we bring this feature back!
 */
class TrackEditItemRenderer implements CellRenderer<PlaylistDetailTrackItem> {

    private final ImageOperations imageOperations;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;
    private final FeatureOperations featureOperations;
    private final TrackEditItemView.Factory trackItemViewFactory;

    private Urn playingTrack = Urn.NOT_SET;

    @Inject
    TrackEditItemRenderer(ImageOperations imageOperations,
                                 EventBus eventBus,
                                 ScreenProvider screenProvider,
                                 Navigator navigator,
                                 FeatureOperations featureOperations,
                                 TrackEditItemView.Factory trackItemViewFactory) {

        this.imageOperations = imageOperations;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.trackItemViewFactory = trackItemViewFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemViewFactory.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistDetailTrackItem> items) {

        TrackItem track = items.get(position).getTrackItem();
        TrackEditItemView trackEditItemView = (TrackEditItemView) itemView.getTag();
        trackEditItemView.setCreator(track.getCreatorName());
        trackEditItemView.setTitle(track.getTitle(), track.isBlocked() ? R.color.list_disabled : R.color.list_primary);

        bindExtraInfoRight(track, trackEditItemView);
        bindExtraInfoBottom(trackEditItemView, track);

        loadArtwork(trackEditItemView, track);
    }

    private void bindExtraInfoRight(TrackItem track, TrackEditItemView itemView) {
        itemView.hideInfoViewsRight();
        if (isHighTierPreview(track)) {
            itemView.showPreviewLabel();
        } else if (track.isPrivate()) {
            itemView.showPrivateIndicator();
        } else {
            itemView.showDuration(ScTextUtils.formatTimestamp(track.getDuration(), TimeUnit.MILLISECONDS));
        }
    }

    private void loadArtwork(TrackEditItemView itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track, ApiImageSize.getListItemImageSize(itemView.getResources()),
                itemView.getImage());
    }

    private void bindExtraInfoBottom(TrackEditItemView itemView, TrackItem track) {
        itemView.hideInfosViewsBottom();
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track.isBlocked()) {
            itemView.showGeoBlocked();
        } else if (track.isPlaying() || track.getUrn().equals(playingTrack)) {
            itemView.showNowPlaying();
        } else if (featureOperations.isOfflineContentEnabled() && track.isUnavailableOffline()) {
            itemView.showNotAvailableOffline();
        }
    }

    private void showPromoted(TrackEditItemView itemView, final PromotedTrackItem track) {
        final Context context = itemView.getContext();
        if (track.hasPromoter()) {
            itemView.showPromotedTrack(context.getString(R.string.promoted_by_promotorname,
                                                         track.getPromoterName().get()));
            itemView.setPromotedClickable(new PromoterClickViewListener(track, eventBus, screenProvider, navigator));
        } else {
            itemView.showPromotedTrack(context.getString(R.string.promoted));
        }
    }

    @Deprecated // use isPlaying from trackItem
    public void setPlayingTrack(@NotNull Urn playingTrack) {
        this.playingTrack = playingTrack;
    }

}

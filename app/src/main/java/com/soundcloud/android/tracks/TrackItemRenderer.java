package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.ItemMenuOptions;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    protected final TrackItemMenuPresenter trackItemMenuPresenter;
    protected final ScreenProvider screenProvider;
    protected final FeatureOperations featureOperations;
    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final TrackItemView.Factory trackItemViewFactory;
    private final OfflineSettingsOperations offlineSettingsOperations;
    private final NetworkConnectionHelper connectionHelper;
    private final TrackStatsDisplayPolicy trackStatsDisplayPolicy;

    private Listener listener;

    public interface Listener {
        void trackItemClicked(Urn urn, int position);
    }

    private enum ActiveFooter {
        POSTED,
        PLAYS_AND_POSTED,
        OFFLINE_STATE
    }

    @Inject
    public TrackItemRenderer(ImageOperations imageOperations,
                             CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus,
                             ScreenProvider screenProvider,
                             Navigator navigator,
                             FeatureOperations featureOperations,
                             TrackItemView.Factory trackItemViewFactory,
                             OfflineSettingsOperations offlineSettingsOperations,
                             NetworkConnectionHelper connectionHelper,
                             TrackStatsDisplayPolicy trackStatsDisplayPolicy) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.trackItemViewFactory = trackItemViewFactory;
        this.offlineSettingsOperations = offlineSettingsOperations;
        this.connectionHelper = connectionHelper;
        this.trackStatsDisplayPolicy = trackStatsDisplayPolicy;
    }

    public TrackItemView.Factory trackItemViewFactory() {
        return trackItemViewFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemViewFactory.createItemView(parent);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackItem> trackItems) {
        bindTrackView(trackItems.get(position), itemView, position, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), ItemMenuOptions.Companion.createDefault());
    }

    public void bindTrackView(final TrackItem track,
                              View itemView,
                              final int position,
                              Optional<TrackSourceInfo> trackSourceInfo,
                              Optional<Module> module,
                              ItemMenuOptions itemMenuOptions) {
        bindTrackView(track, itemView, position, trackSourceInfo, module, Optional.absent(), Optional.absent(), itemMenuOptions);
    }

    public void bindPlaylistTrackView(final TrackItem track,
                                      View itemView,
                                      final int position,
                                      Optional<Urn> pageUrn,
                                      Optional<TrackSourceInfo> trackSourceInfo) {
        if (track.isBlocked()) {
            // note: TrackItemRenderer already calls `setClickable(false)` but this doesn't appear
            // to work for the ListView widget (it's still clickable, and shows ripples on touch)
            // http://stackoverflow.com/questions/4636270/android-listview-child-view-setenabled-and-setclickable-do-nothing
            itemView.setOnClickListener(null);
        }

        bindTrackView(track,
                      itemView,
                      position,
                      trackSourceInfo,
                      Optional.of(Module.create(Module.PLAYLIST, position)),
                      pageUrn,
                      Optional.of(ActiveFooter.OFFLINE_STATE),
                      ItemMenuOptions.Companion.createDefault());
    }

    public void bindSystemPlaylistTrackView(final TrackItem track,
                                            View itemView,
                                            final int position,
                                            Optional<Urn> pageUrn,
                                            Optional<TrackSourceInfo> trackSourceInfo) {
        bindTrackView(track,
                      itemView,
                      position,
                      trackSourceInfo,
                      Optional.absent(),
                      pageUrn,
                      Optional.of(ActiveFooter.PLAYS_AND_POSTED),
                      ItemMenuOptions.Companion.createDefault());
    }

    public void bindOfflineTrackView(final TrackItem trackItem,
                                     View itemView,
                                     final int position,
                                     Optional<TrackSourceInfo> trackSourceInfo,
                                     Optional<Module> module) {
        bindTrackView(trackItem,
                      itemView,
                      position,
                      trackSourceInfo,
                      module,
                      Optional.absent(),
                      Optional.of(ActiveFooter.OFFLINE_STATE),
                      ItemMenuOptions.Companion.createDefault());
    }

    private void bindTrackView(final TrackItem trackItem,
                               View itemView,
                               final int position,
                               Optional<TrackSourceInfo> trackSourceInfo,
                               Optional<Module> module,
                               Optional<Urn> pageUrn,
                               Optional<ActiveFooter> activeFooter,
                               ItemMenuOptions itemMenuOptions) {
        TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.setCreator(trackItem.creatorName());
        trackItemView.setTitle(trackItem.title(), trackItem.isBlocked()
                                                  ? trackItemViewFactory.getDisabledTitleColor()
                                                  : trackItemViewFactory.getPrimaryTitleColor());
        if (listener != null) {
            itemView.setOnClickListener(v -> listener.trackItemClicked(trackItem.getUrn(), position));
        }

        itemView.setClickable(!trackItem.isBlocked());

        bindExtraInfoRight(trackItem, trackItemView);
        bindExtraInfoBottom(trackItemView, trackItem, activeFooter);

        bindArtwork(trackItemView, trackItem);
        bindOverFlow(trackItemView, trackItem, position, pageUrn, trackSourceInfo, module, itemMenuOptions);
    }

    private void bindExtraInfoRight(TrackItem track, TrackItemView trackItemView) {
        trackItemView.hideInfoViewsRight();
        if (isHighTierPreview(track)) {
            trackItemView.showPreviewLabel();
        } else {
            if (track.isPrivate()) {
                trackItemView.showPrivateIndicator();
            }
            trackItemView.showDuration(ScTextUtils.formatTimestamp(track.getDuration(), TimeUnit.MILLISECONDS));
        }
    }

    private void bindOverFlow(final TrackItemView itemView,
                              final TrackItem track,
                              final int position,
                              final Optional<Urn> pageUrn,
                              final Optional<TrackSourceInfo> trackSourceInfo,
                              final Optional<Module> module,
                              final ItemMenuOptions itemMenuOptions) {
        itemView.showOverflow(overflowButton -> showTrackItemMenu(overflowButton, track, position, pageUrn, trackSourceInfo, module, itemMenuOptions));
    }

    protected void showTrackItemMenu(View button,
                                     TrackItem track,
                                     int position,
                                     Optional<Urn> pageUrn,
                                     Optional<TrackSourceInfo> trackSourceInfo,
                                     Optional<Module> module,
                                     ItemMenuOptions itemMenuOptions) {
        Urn playlistUrn = null;
        Urn ownerUrn = null;
        PromotedSourceInfo promotedSourceInfo = null;
        if (trackSourceInfo.isPresent()) {
            TrackSourceInfo info = trackSourceInfo.get();
            playlistUrn = info.getCollectionUrn();
            ownerUrn = info.getPlaylistOwnerUrn();
            promotedSourceInfo = info.getPromotedSourceInfo();
        }

        trackItemMenuPresenter.show(getFragmentActivity(button),
                                    button,
                                    track,
                                    playlistUrn,
                                    ownerUrn,
                                    promotedSourceInfo,
                                    getEventContextMetaDataBuilder(track, module, pageUrn, trackSourceInfo),
                                    itemMenuOptions);
    }

    private EventContextMetadata.Builder getEventContextMetaDataBuilder(TrackItem item,
                                                                        Optional<Module> module,
                                                                        Optional<Urn> pageUrn,
                                                                        Optional<TrackSourceInfo> trackSourceInfo) {
        final String screen = screenProvider.getLastScreenTag();

        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .pageName(screen)
                                                                         .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                 item));

        if (module.isPresent()) {
            builder.module(module.get());
        }

        if (trackSourceInfo.isPresent()) {
            builder.trackSourceInfo(trackSourceInfo.get());
        }

        if (pageUrn.isPresent()) {
            builder.pageUrn(pageUrn.get());
        }

        return builder;
    }

    private void bindArtwork(TrackItemView itemView, TrackItem track) {
        imageOperations.displayInAdapterView(track.getUrn(),
                                             track.getImageUrlTemplate(),
                                             ApiImageSize.getListItemImageSize(itemView.getResources()),
                                             itemView.getImage(),
                                             ImageOperations.DisplayType.DEFAULT);
        if (isFullHighTierTrack(track) || isHighTierPreview(track)) {
            itemView.showGoLabel();
        }
    }

    private void bindExtraInfoBottom(TrackItemView itemView, TrackItem track, Optional<ActiveFooter> activeFooter) {
        itemView.hideInfosViewsBottom();
        if (track.isPromoted()) {
            showPromoted(itemView, track);
        } else if (track.isBlocked()) {
            itemView.showGeoBlocked();
        } else if (track.isPlaying()) {
            itemView.showNowPlaying();
        } else if (featureOperations.isOfflineContentEnabled() && track.isUnavailableOffline()) {
            itemView.showNotAvailableOffline();
        } else if (ActiveFooter.OFFLINE_STATE == activeFooter.orNull() && shouldShowOfflineState(track.offlineState())) {
            showOfflineState(itemView, track.offlineState());
        } else if (ActiveFooter.POSTED == activeFooter.orNull()) {
            itemView.showPostedTime(track.createdAt());
        } else if (ActiveFooter.PLAYS_AND_POSTED == activeFooter.orNull()) {
            showPlaysAndPostedTime(itemView, track);
        } else {
            showPlayCount(itemView, track);
        }
    }

    private boolean shouldShowOfflineState(OfflineState offlineState) {
        return featureOperations.isOfflineContentEnabled()
                && isActiveOfflineState(offlineState);
    }

    private boolean isActiveOfflineState(OfflineState offlineState) {
        return OfflineState.REQUESTED == offlineState
                || OfflineState.DOWNLOADING == offlineState
                || OfflineState.DOWNLOADED == offlineState;
    }

    private void showOfflineState(TrackItemView itemView, OfflineState offlineState) {
        if (OfflineState.REQUESTED == offlineState) {
            handleRequestedOfflineState(itemView);
        } else if (OfflineState.DOWNLOADING == offlineState) {
            itemView.showDownloading();
        } else if (OfflineState.DOWNLOADED == offlineState) {
            itemView.showDownloaded();
        }
    }

    private void handleRequestedOfflineState(TrackItemView itemView) {
        if (offlineSettingsOperations.isWifiOnlyEnabled() && !connectionHelper.isWifiConnected()) {
            itemView.showNoWifi();
        } else if (!connectionHelper.isNetworkConnected()) {
            itemView.showNoConnection();
        } else {
            itemView.showRequested();
        }
    }

    private void showChartPosition(View itemView, int position) {
        final TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.showPosition(position);
    }

    private void showPromoted(TrackItemView itemView, final TrackItem track) {
        final Context context = itemView.getContext();
        if (track.isPromoted() && track.promoterUrn().isPresent()) {
            itemView.showPromotedTrack(context.getString(R.string.promoted_by_promotorname, track.promoterName()));
            itemView.setPromotedClickable(new PromoterClickViewListener(track, eventBus, screenProvider, navigator));
        } else {
            itemView.showPromotedTrack(context.getString(R.string.promoted));
        }
    }

    private void showPlayCount(TrackItemView itemView, TrackItem trackItem) {
        if (shouldDisplayPlayCount(trackItem)) {
            itemView.showPlaycount(numberFormatter.format(trackItem.playCount()));
        }
    }

    private void showPlaysAndPostedTime(TrackItemView itemView, TrackItem trackItem) {
        Date createdAt = trackItem.createdAt();
        if (shouldDisplayPlayCount(trackItem)) {
            itemView.showPlaysAndPostedTime(numberFormatter.format(trackItem.playCount()), createdAt);
        } else {
            itemView.showPostedTime(createdAt);
        }
    }

    private boolean shouldDisplayPlayCount(TrackItem trackItem) {
        return trackStatsDisplayPolicy.displayPlaysCount(trackItem);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

}

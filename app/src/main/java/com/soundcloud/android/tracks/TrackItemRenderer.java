package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.discovery.charts.ChartTrackItem;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    public interface Listener {

        void trackItemClicked(Urn urn, int position);

    }

    private enum ActiveFooter {
        POSTED,
        PLAYS_AND_POSTED
    }

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final EventBus eventBus;
    protected final ScreenProvider screenProvider;
    private final Navigator navigator;
    protected final FeatureOperations featureOperations;
    private final TrackItemView.Factory trackItemViewFactory;
    private final FeatureFlags flags;

    protected final TrackItemMenuPresenter trackItemMenuPresenter;
    protected Optional<String> moduleName = Optional.absent();

    private Urn playingTrack = Urn.NOT_SET;
    private Listener listener = null;
    private Optional<ActiveFooter> activeFooter = Optional.absent();

    @Inject
    public TrackItemRenderer(ImageOperations imageOperations,
                             CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus,
                             ScreenProvider screenProvider,
                             Navigator navigator,
                             FeatureOperations featureOperations,
                             TrackItemView.Factory trackItemViewFactory,
                             FeatureFlags flags) {
        this(Optional.<String>absent(),
             imageOperations,
             numberFormatter,
             trackItemMenuPresenter,
             eventBus,
             screenProvider,
             navigator,
             featureOperations,
             trackItemViewFactory,
             flags);
    }

    protected TrackItemRenderer(Optional<String> moduleName,
                                ImageOperations imageOperations,
                                CondensedNumberFormatter numberFormatter,
                                TrackItemMenuPresenter trackItemMenuPresenter,
                                EventBus eventBus,
                                ScreenProvider screenProvider,
                                Navigator navigator,
                                FeatureOperations featureOperations,
                                TrackItemView.Factory trackItemViewFactory,
                                FeatureFlags flags) {
        this.moduleName = moduleName;
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.trackItemViewFactory = trackItemViewFactory;
        this.flags = flags;
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
        bindTrackView(position, itemView, trackItems.get(position));
    }

    public void bindTrackView(final int position, View itemView, TrackItem track) {
        bindTrackView(track,
                      itemView,
                      position,
                      Optional.absent(),
                      createModule(position));
    }

    public void bindTrackView(final TrackItem track,
                              View itemView,
                              final int position,
                              Optional<TrackSourceInfo> trackSourceInfo,
                              Optional<Module> module) {
        bindTrackView(track, itemView, position, trackSourceInfo, module, Optional.absent());
    }

    public void bindChartTrackView(final ChartTrackItem chartTrackItem,
                                   View itemView,
                                   final int position,
                                   Optional<TrackSourceInfo> trackSourceInfo) {
        bindTrackView(chartTrackItem.getTrackItem(),
                      itemView,
                      position,
                      trackSourceInfo,
                      Optional.absent(),
                      chartTrackItem.chartType() == ChartType.TRENDING ? Optional.of(ActiveFooter.POSTED) : Optional.absent());

        showChartPosition(itemView, position);
    }

    public void bindNewForYouTrackView(final TrackItem track,
                                       View itemView,
                                       final int position,
                                       Optional<TrackSourceInfo> trackSourceInfo) {
        bindTrackView(track,
                      itemView,
                      position,
                      trackSourceInfo,
                      Optional.absent(),
                      Optional.of(ActiveFooter.PLAYS_AND_POSTED));
    }

    private void bindTrackView(final TrackItem track,
                               View itemView,
                               final int position,
                               Optional<TrackSourceInfo> trackSourceInfo,
                               Optional<Module> module,
                               Optional<ActiveFooter> activeFooter) {
        TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.setCreator(track.creatorName());
        trackItemView.setTitle(track.title(), track.isBlocked()
                                              ? trackItemViewFactory.getDisabledTitleColor()
                                              : trackItemViewFactory.getPrimaryTitleColor());
        if (listener != null) {
            itemView.setOnClickListener(v -> listener.trackItemClicked(track.getUrn(), position));
        }

        this.activeFooter = activeFooter;

        itemView.setClickable(!track.isBlocked());

        bindExtraInfoRight(track, trackItemView);
        bindExtraInfoBottom(trackItemView, track);

        bindArtwork(trackItemView, track);
        bindOverFlow(trackItemView, track, position, trackSourceInfo, module);
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

    public void bindOverFlow(final TrackItemView itemView,
                             final TrackItem track,
                             final int position,
                             final Optional<TrackSourceInfo> trackSourceInfo,
                             final Optional<Module> module) {
        itemView.showOverflow(overflowButton -> showTrackItemMenu(overflowButton, track, position, trackSourceInfo, module));
    }

    private Optional<Module> createModule(int position) {
        return moduleName.transform(input -> Module.create(input, position));
    }

    protected void showTrackItemMenu(View button,
                                     TrackItem track,
                                     int position,
                                     Optional<TrackSourceInfo> trackSourceInfo,
                                     Optional<Module> module) {
        trackItemMenuPresenter.show(getFragmentActivity(button),
                                    button,
                                    track,
                                    getEventContextMetaDataBuilder(track, module, trackSourceInfo));
    }

    private EventContextMetadata.Builder getEventContextMetaDataBuilder(PlayableItem item,
                                                                        Optional<Module> module,
                                                                        Optional<TrackSourceInfo> trackSourceInfo) {
        final String screen = screenProvider.getLastScreenTag();

        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(screen)
                                                                         .pageName(screen)
                                                                         .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                 item));

        if (module.isPresent()) {
            builder.module(module.get());
        }

        if (trackSourceInfo.isPresent()) {
            builder.trackSourceInfo(trackSourceInfo.get());
        }

        return builder;
    }

    private void bindArtwork(TrackItemView itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track, ApiImageSize.getListItemImageSize(itemView.getResources()),
                itemView.getImage());
        if (isFullHighTierTrack(track) || isHighTierPreview(track)) {
            itemView.setGoLabelSelected(flags.isEnabled(Flag.MID_TIER_ROLLOUT));
            itemView.showGoLabel();
        }
    }

    private void bindExtraInfoBottom(TrackItemView itemView, TrackItem track) {
        itemView.hideInfosViewsBottom();
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track.isBlocked()) {
            itemView.showGeoBlocked();
        } else if (track.isPlaying() || track.getUrn().equals(playingTrack)) {
            itemView.showNowPlaying();
        } else if (featureOperations.isOfflineContentEnabled() && track.isUnavailableOffline()) {
            itemView.showNotAvailableOffline();
        } else if (activeFooter.isPresent() && activeFooter.get().equals(ActiveFooter.POSTED)) {
            itemView.showPostedTime(track.getCreatedAt());
        } else if (activeFooter.isPresent() && activeFooter.get().equals(ActiveFooter.PLAYS_AND_POSTED)) {
            showPlaysAndPostedTime(itemView, track, track.getCreatedAt());
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void showChartPosition(View itemView, int position) {
        final TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.showPosition(position);
    }

    private void showPromoted(TrackItemView itemView, final PromotedTrackItem track) {
        final Context context = itemView.getContext();
        if (track.hasPromoter()) {
            itemView.showPromotedTrack(context.getString(R.string.promoted_by_promotorname,
                                                         track.getPromoterName().get()));
            itemView.setPromotedClickable(new PromoterClickViewListener(track, eventBus, screenProvider, navigator));
        } else {
            itemView.showPromotedTrack(context.getString(R.string.promoted));
        }
    }

    private void showPlayCount(TrackItemView itemView, TrackItem track) {
        final int count = track.playCount();
        if (hasPlayCount(count)) {
            itemView.showPlaycount(numberFormatter.format(count));
        }
    }

    private void showPlaysAndPostedTime(TrackItemView itemView, TrackItem track, Date createdAt) {
        final int count = track.playCount();

        if (hasPlayCount(count)) {
            itemView.showPlaysAndPostedTime(numberFormatter.format(count), createdAt);
        } else {
            itemView.showPostedTime(createdAt);
        }
    }

    private boolean hasPlayCount(int playCount) {
        return playCount > 0;
    }

    @Deprecated // use isPlaying from trackItem
    public void setPlayingTrack(@NotNull Urn playingTrack) {
        this.playingTrack = playingTrack;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}

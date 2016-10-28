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
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    public interface Listener {

        void trackItemClicked(Urn urn, int position);

    }

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final EventBus eventBus;
    protected final ScreenProvider screenProvider;
    private final Navigator navigator;
    protected final FeatureOperations featureOperations;
    private final TrackItemView.Factory trackItemViewFactory;

    protected final TrackItemMenuPresenter trackItemMenuPresenter;
    protected Optional<String> moduleName = Optional.absent();

    private Urn playingTrack = Urn.NOT_SET;
    private Listener listener = null;

    @Inject
    public TrackItemRenderer(ImageOperations imageOperations,
                             CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus,
                             ScreenProvider screenProvider,
                             Navigator navigator,
                             FeatureOperations featureOperations,
                             TrackItemView.Factory trackItemViewFactory) {
        this(Optional.<String>absent(),
             imageOperations,
             numberFormatter,
             trackItemMenuPresenter,
             eventBus,
             screenProvider,
             navigator,
             featureOperations,
             trackItemViewFactory);
    }

    protected TrackItemRenderer(Optional<String> moduleName,
                             ImageOperations imageOperations,
                             CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus,
                             ScreenProvider screenProvider,
                             Navigator navigator,
                             FeatureOperations featureOperations,
                             TrackItemView.Factory trackItemViewFactory) {
        this.moduleName = moduleName;
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
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
    public void bindItemView(final int position, View itemView, List<TrackItem> trackItems) {
        final TrackItem track = trackItems.get(position);
        bindTrackView(track,
                      itemView,
                      position,
                      Optional.<TrackSourceInfo>absent(),
                      moduleName.transform(new Function<String, Module>() {
                          public Module apply(String input) {
                              return Module.create(input, position);
                          }
                      }));
    }

    public void bindTrackView(final TrackItem track,
                              View itemView,
                              final int position,
                              Optional<TrackSourceInfo> trackSourceInfo,
                              Optional<Module> module) {
        TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.setCreator(track.getCreatorName());
        trackItemView.setTitle(track.getTitle(), track.isBlocked()
                                                 ? trackItemViewFactory.getDisabledTitleColor()
                                                 : trackItemViewFactory.getPrimaryTitleColor());
        if (listener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.trackItemClicked(track.getUrn(), position);
                }
            });
        }
        if (track.isBlocked()) {
            itemView.setClickable(false);
        }

        bindExtraInfoRight(track, trackItemView);
        bindExtraInfoBottom(trackItemView, track, position);

        bindArtwork(trackItemView, track);
        setupOverFlow(trackItemView, track, position, trackSourceInfo, module);
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

    private void setupOverFlow(final TrackItemView itemView,
                               final TrackItem track,
                               final int position,
                               final Optional<TrackSourceInfo> trackSourceInfo,
                               final Optional<Module> module) {
        itemView.setOverflowListener(new TrackItemView.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                showTrackItemMenu(overflowButton, track, position, trackSourceInfo, module);
            }
        });
    }

    protected void showTrackItemMenu(View button,
                                     TrackItem track,
                                     int position,
                                     Optional<TrackSourceInfo> trackSourceInfo,
                                     Optional<Module> module) {
        trackItemMenuPresenter.show(getFragmentActivity(button),
                                    button,
                                    track,
                                    position,
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
            itemView.showGoLabel();
        }
    }

    private void bindExtraInfoBottom(TrackItemView itemView, TrackItem track, int position) {
        itemView.hideInfosViewsBottom();
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track instanceof ChartTrackItem) {
            showChartTrackItem(itemView, (ChartTrackItem) track, position);
        } else if (track.isBlocked()) {
            itemView.showGeoBlocked();
        } else if (track.isPlaying() || track.getUrn().equals(playingTrack)) {
            itemView.showNowPlaying();
        } else if (featureOperations.isOfflineContentEnabled() && track.isUnavailableOffline()) {
            itemView.showNotAvailableOffline();
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void showChartTrackItem(TrackItemView itemView, ChartTrackItem chartTrackItem, int position) {
        itemView.showPosition(position);
        if (chartTrackItem.chartType() == ChartType.TRENDING) {
            itemView.showPostedTime(chartTrackItem.getCreatedAt());
        } else {
            showPlayCount(itemView, chartTrackItem);
        }
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
        final int count = track.getPlayCount();
        if (hasPlayCount(count)) {
            itemView.showPlaycount(numberFormatter.format(count));
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

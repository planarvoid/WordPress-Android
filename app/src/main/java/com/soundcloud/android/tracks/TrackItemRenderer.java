package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.discovery.ChartTrackItem;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.util.CondensedNumberFormatter;
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

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    public interface Listener {

        void trackItemClicked(Urn urn);

    }

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;
    protected final FeatureOperations featureOperations;
    private final TrackItemView.Factory trackItemViewFactory;

    protected final TrackItemMenuPresenter trackItemMenuPresenter;

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
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        final TrackItem track = trackItems.get(position);
        bindTrackView(track, itemView, position);
    }

    public void bindTrackView(final TrackItem track, View itemView, final int position) {
        TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.setCreator(track.getCreatorName());
        trackItemView.setTitle(track.getTitle(), track.isBlocked()
                                                 ? trackItemViewFactory.getDisabledTitleColor()
                                                 : trackItemViewFactory.getPrimaryTitleColor());
        if (listener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.trackItemClicked(track.getUrn());
                }
            });
        }
        if (track.isBlocked()) {
            itemView.setClickable(false);
        }

        bindExtraInfoRight(track, trackItemView);
        bindExtraInfoBottom(trackItemView, track);

        loadArtwork(trackItemView, track);
        setupOverFlow(trackItemView, track, position);
        setupRepeating(track, trackItemView);
    }

    private void setupRepeating(TrackItem track, TrackItemView trackItemView) {
        trackItemView.setRepeating(track.isInRepeatMode(), track.isPlaying());
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

    private void setupOverFlow(final TrackItemView itemView, final TrackItem track, final int position) {
        itemView.setOverflowListener(new TrackItemView.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                showTrackItemMenu(overflowButton, track, position);
            }
        });
    }

    protected void showTrackItemMenu(View button, TrackItem track, int position) {
        trackItemMenuPresenter.show(getFragmentActivity(button), button, track, position);
    }

    private void loadArtwork(TrackItemView itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track, ApiImageSize.getListItemImageSize(itemView.getResources()),
                itemView.getImage());
    }

    private void bindExtraInfoBottom(TrackItemView itemView, TrackItem track) {
        itemView.hideInfosViewsBottom();
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track instanceof ChartTrackItem) {
            showChartTrackItem(itemView, (ChartTrackItem) track);
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

    private void showChartTrackItem(TrackItemView itemView, ChartTrackItem chartTrackItem) {
        itemView.showPosition(chartTrackItem.position());
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
        if (isFullHighTierTrack(track)) {
            itemView.showGoLabel();
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

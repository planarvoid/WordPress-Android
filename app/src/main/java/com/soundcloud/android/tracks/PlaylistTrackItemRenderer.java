package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;
import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

import java.util.List;

@AutoFactory(allowSubclasses = true)
public class PlaylistTrackItemRenderer extends DownloadableTrackItemRenderer {

    private RemoveTrackListener removeTrackListener;
    private PromotedSourceInfo promotedSourceInfo;
    private Urn playlistUrn = Urn.NOT_SET;
    private Urn ownerUrn = Urn.NOT_SET;

    public PlaylistTrackItemRenderer(RemoveTrackListener removeTrackListener,
                                     @Provided ImageOperations imageOperations,
                                     @Provided CondensedNumberFormatter numberFormatter,
                                     @Provided TrackItemMenuPresenter trackItemMenuPresenter,
                                     @Provided EventBus eventBus,
                                     @Provided FeatureOperations featureOperations,
                                     @Provided ScreenProvider screenProvider,
                                     @Provided Navigator navigator,
                                     @Provided TrackItemView.Factory trackItemViewFactory) {
        super(Optional.of(Module.PLAYLIST),
              imageOperations,
              numberFormatter,
              trackItemMenuPresenter,
              eventBus,
              featureOperations,
              screenProvider,
              navigator,
              trackItemViewFactory);
        this.removeTrackListener = removeTrackListener;
    }

    public void setPlaylistInformation(PromotedSourceInfo promotedSourceInfo, Urn playlistUrn, Urn ownerUrn) {
        this.promotedSourceInfo = promotedSourceInfo;
        this.playlistUrn = playlistUrn;
        this.ownerUrn = ownerUrn;
    }

    @Override
    protected void showTrackItemMenu(View button,
                                     TrackItem track,
                                     int position,
                                     Optional<TrackSourceInfo> trackSourceInfo,
                                     Optional<Module> module) {
        trackItemMenuPresenter.show(getFragmentActivity(button),
                                    button,
                                    track,
                                    position,
                                    playlistUrn,
                                    ownerUrn,
                                    removeTrackListener,
                                    promotedSourceInfo,
                                    getEventContextMetaDataBuilder(module, trackSourceInfo));
    }

    private EventContextMetadata.Builder getEventContextMetaDataBuilder(Optional<Module> module,
                                                                        Optional<TrackSourceInfo> trackSourceInfo) {
        final String screen = screenProvider.getLastScreenTag();

        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(screen)
                                                                         .pageName(screen);

        if (module.isPresent()) {
            builder.module(module.get());
        }

        if (trackSourceInfo.isPresent()) {
            builder.trackSourceInfo(trackSourceInfo.get());
        }

        return builder;
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        super.bindItemView(position, itemView, trackItems);
        disableBlockedTrackClicks(itemView, trackItems.get(position));
    }

    private void disableBlockedTrackClicks(View itemView, TrackItem track) {
        if (track.isBlocked()) {
            // note: TrackItemRenderer already calls `setClickable(false)` but this doesn't appear
            // to work for the ListView widget (it's still clickable, and shows ripples on touch)
            // http://stackoverflow.com/questions/4636270/android-listview-child-view-setenabled-and-setclickable-do-nothing
            itemView.setOnClickListener(null);
        }
    }
}

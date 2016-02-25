package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class PlaylistTrackItemRenderer extends DownloadableTrackItemRenderer {

    private RemoveTrackListener removeTrackListener;
    private PromotedSourceInfo promotedSourceInfo;
    private Urn pageUrn = Urn.NOT_SET;

    @Inject
    public PlaylistTrackItemRenderer(ImageOperations imageOperations, CondensedNumberFormatter numberFormatter,
                                     TrackItemMenuPresenter trackItemMenuPresenter, EventBus eventBus,
                                     FeatureOperations featureOperations, ScreenProvider screenProvider,
                                     Navigator navigator, TrackItemView.Factory trackItemViewFactory) {
        super(imageOperations, numberFormatter, trackItemMenuPresenter, eventBus, featureOperations, screenProvider,
                navigator, trackItemViewFactory);
    }

    public void setRemoveTrackListener(RemoveTrackListener removeTrackListener) {
        this.removeTrackListener = removeTrackListener;
    }

    public void setPlaylistInformation(PromotedSourceInfo promotedSourceInfo, Urn pageUrn) {
        this.promotedSourceInfo = promotedSourceInfo;
        this.pageUrn = pageUrn;
    }

    @Override
    protected void showTrackItemMenu(View button, TrackItem track, int position) {
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position, pageUrn,
                removeTrackListener, promotedSourceInfo);
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

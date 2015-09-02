package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.android.model.Urn;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;

public class PlaylistTrackItemRenderer extends DownloadableTrackItemRenderer {

    private RemoveTrackListener removeTrackListener;
    private PromotedSourceInfo promotedSourceInfo;
    private Urn pageUrn = Urn.NOT_SET;

    @Inject
    public PlaylistTrackItemRenderer(ImageOperations imageOperations, TrackItemMenuPresenter trackItemMenuPresenter,
                                     EventBus eventBus, FeatureOperations featureOperations,
                                     ScreenProvider screenProvider, Navigator navigator, TrackItemView.Factory trackItemViewFactory) {
        super(imageOperations, trackItemMenuPresenter, eventBus, featureOperations, screenProvider, navigator, trackItemViewFactory);
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
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position, pageUrn, removeTrackListener, promotedSourceInfo);
    }
}

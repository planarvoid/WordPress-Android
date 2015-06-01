package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;

public class PlaylistTrackItemRenderer extends DownloadableTrackItemRenderer {

    private RemoveTrackListener removeTrackListener;

    @Inject
    public PlaylistTrackItemRenderer(ImageOperations imageOperations, TrackItemMenuPresenter trackItemMenuPresenter,
                                     EventBus eventBus, FeatureOperations featureOperations,
                                     ScreenProvider screenProvider, Navigator navigator) {
        super(imageOperations, trackItemMenuPresenter, eventBus, featureOperations, screenProvider, navigator);
    }

    public void setRemoveTrackListener(RemoveTrackListener removeTrackListener) {
        this.removeTrackListener = removeTrackListener;
    }

    @Override
    protected void showTrackItemMenu(View button, TrackItem track, int position) {
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position, removeTrackListener);
    }
}

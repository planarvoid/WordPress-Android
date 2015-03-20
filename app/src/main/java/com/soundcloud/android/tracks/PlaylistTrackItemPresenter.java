package com.soundcloud.android.tracks;

import static com.soundcloud.android.tracks.TrackItemMenuPresenter.RemoveTrackListener;

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.propeller.PropertySet;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;

public class PlaylistTrackItemPresenter extends DownloadableTrackItemPresenter {

    private RemoveTrackListener removeTrackListener;

    @Inject
    public PlaylistTrackItemPresenter(ImageOperations imageOperations, TrackItemMenuPresenter trackItemMenuPresenter, FeatureOperations featureOperations) {
        super(imageOperations, trackItemMenuPresenter, featureOperations);
    }

    public void setRemoveTrackListener(RemoveTrackListener removeTrackListener) {
        this.removeTrackListener = removeTrackListener;
    }

    @Override
    protected void showTrackItemMenu(View button, PropertySet track, int position) {
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position, removeTrackListener);
    }
}

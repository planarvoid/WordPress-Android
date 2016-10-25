package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;

public interface TrackRecommendationListener {
    void onReasonClicked(Urn seedUrn);

    void onTrackClicked(Urn seedUrn, Urn trackUrn);
}

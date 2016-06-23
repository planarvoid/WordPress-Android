package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;

interface TrackRecommendationListener {
    void onReasonClicked(Urn seedUrn);

    void onTrackClicked(Urn seedUrn, Urn trackUrn);
}

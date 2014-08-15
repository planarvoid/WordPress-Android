package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

public class AudioAdCompanionImpressionEvent {
    private final TrackUrn monetizableTrackUrn;
    private final long timeStamp;
    private final UserUrn userUrn;
    private final String adsWizzId;
    private Uri artworkUri;
    private final Urn trackUrn;

    public AudioAdCompanionImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, UserUrn userUrn) {
        this(adMetaData, audioAdTrack, userUrn, System.currentTimeMillis());
    }

    public AudioAdCompanionImpressionEvent(PropertySet adMetaData, Urn audioAdTrack, UserUrn userUrn, long timeStamp) {
        this.userUrn = userUrn;
        this.timeStamp = timeStamp;
        this.adsWizzId = adMetaData.get(AdProperty.AD_URN);
        this.monetizableTrackUrn = adMetaData.get(AdProperty.MONETIZABLE_TRACK_URN);
        this.artworkUri = adMetaData.get(AdProperty.ARTWORK);
        this.trackUrn = audioAdTrack;
    }

    public Urn getMonetizedTrackUrn() {
        return monetizableTrackUrn;
    }

    public Urn getUserUrn() {
        return userUrn;
    }

    public String getAdsWizzUrn() {
        return adsWizzId;
    }

    public Uri getExternalMediaUrn() {
        return artworkUri;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }
}

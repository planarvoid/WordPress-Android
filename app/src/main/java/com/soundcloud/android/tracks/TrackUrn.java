package com.soundcloud.android.tracks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;

public final class TrackUrn extends Urn {

    public static final TrackUrn NOT_SET = Urn.forTrack(Consts.NOT_SET);

    public TrackUrn(final long id) {
        super(SOUNDCLOUD_SCHEME, SOUNDS_TYPE, id); // TODO: should be TRACKS_TYPE
    }
}

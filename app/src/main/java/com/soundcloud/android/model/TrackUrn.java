package com.soundcloud.android.model;

import com.soundcloud.android.Consts;

public final class TrackUrn extends Urn {

    public static final TrackUrn NOT_SET = Urn.forTrack(Consts.NOT_SET);

    protected TrackUrn(final long id) {
        super(SOUNDCLOUD_SCHEME, SOUNDS_TYPE, id); // TODO: should be TRACKS_TYPE
    }
}

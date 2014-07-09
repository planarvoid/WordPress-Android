package com.soundcloud.android.model;

public final class TrackUrn extends Urn {

    protected TrackUrn(final long id) {
        super(SOUNDCLOUD_SCHEME, SOUNDS_TYPE, id); // TODO: should be TRACKS_TYPE
    }
}

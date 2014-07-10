package com.soundcloud.android.model;

public class AdUrn extends Urn {

    protected AdUrn(final long id) {
        super(ADSWIZZ_SCHEME, AD_TYPE, id);
    }
}

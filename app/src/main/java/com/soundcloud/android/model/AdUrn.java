package com.soundcloud.android.model;

import com.soundcloud.android.model.Urn;

public class AdUrn extends Urn {

    protected AdUrn(final long id) {
        super(ADSWIZZ_SCHEME, AD_TYPE, id);
    }
}

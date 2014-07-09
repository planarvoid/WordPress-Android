package com.soundcloud.android.model;

public final class UserUrn extends Urn {

    protected UserUrn(long id) {
        super(SOUNDCLOUD_SCHEME, USERS_TYPE, id);
    }
}

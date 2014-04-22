package com.soundcloud.android.model;

public final class UserUrn extends Urn {

    protected UserUrn(long id) {
        super(USERS_TYPE, id);
    }
}

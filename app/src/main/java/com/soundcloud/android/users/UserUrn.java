package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;

public final class UserUrn extends Urn {

    public UserUrn(long id) {
        super(SOUNDCLOUD_SCHEME, USERS_TYPE, id);
    }
}

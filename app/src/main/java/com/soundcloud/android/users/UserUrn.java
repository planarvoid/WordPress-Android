package com.soundcloud.android.users;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;

public final class UserUrn extends Urn {

    public static final UserUrn NOT_SET = Urn.forUser(Consts.NOT_SET);

    public UserUrn(long id) {
        super(SOUNDCLOUD_SCHEME, USERS_TYPE, id);
    }
}

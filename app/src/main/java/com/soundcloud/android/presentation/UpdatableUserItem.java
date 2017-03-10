package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.users.User;

public interface UpdatableUserItem extends Entity {
    UpdatableUserItem updateWithUser(User user);
}

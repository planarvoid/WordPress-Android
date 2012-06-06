package com.soundcloud.android.adapter;

import com.soundcloud.android.model.User;
import org.jetbrains.annotations.Nullable;


public interface IUserlistAdapter {
    @Nullable public User getUserAt(int index);
}

package com.soundcloud.android.collections.views;

import com.soundcloud.android.model.User;

import android.content.Context;
import android.util.AttributeSet;

public class PlayerTopBar extends PlayableBar {
    public PlayerTopBar(Context context) {
        super(context);
    }

    public PlayerTopBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public String getIconRemoteUri() {
        final User user = mPlayableHolder.getPlayable().getUser();
        return user == null ? super.getIconRemoteUri() : user.getListAvatarUri(getContext());
    }
}

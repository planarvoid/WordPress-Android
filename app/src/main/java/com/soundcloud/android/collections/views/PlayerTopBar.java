package com.soundcloud.android.collections.views;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.User;

import android.content.Context;
import android.util.AttributeSet;

public class PlayerTopBar extends PlayableBar {

    public PlayerTopBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, ImageOperations.newInstance());
    }

    public PlayerTopBar(Context context, AttributeSet attributeSet, ImageOperations imageOperations) {
        super(context, attributeSet, imageOperations);
    }

    @Override
    public String getIconRemoteUri() {
        final User user = mPlayableHolder.getPlayable().getUser();
        return user == null ? super.getIconRemoteUri() : user.getListAvatarUri(getContext());
    }
}

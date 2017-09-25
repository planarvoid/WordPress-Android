package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.ItemMenuOptions;
import com.soundcloud.android.presentation.PlayableItem;

import android.support.annotation.NonNull;

public abstract class UserSoundsItemRenderer implements CellRenderer<UserSoundsItem> {
    @NonNull
    protected ItemMenuOptions createItemMenuOptions(UserSoundsItem userSoundsItem, PlayableItem track) {
        if (userSoundsItem.userUrn().equals(track.creatorUrn())) {
            return ItemMenuOptions.Companion.forGoToProfileDisabled();
        } else {
            return ItemMenuOptions.Companion.forGoToProfileEnabled();
        }
    }
}

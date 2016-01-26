package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class UserSoundsPlaylistCardRenderer implements CellRenderer<UserSoundsItem> {
    @Override
    public View createItemView(ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        ((TextView) itemView).setText(getText(items.get(position)));
    }

    public String getText(UserSoundsItem item) {
        switch (item.getItemType()) {
            case UserSoundsItem.TYPE_DIVIDER:
                return "DIVIDER";
            case UserSoundsItem.TYPE_HEADER:
                return "HEADER";
            case UserSoundsItem.TYPE_VIEW_ALL:
                return "VIEW ALL";
            case UserSoundsItem.TYPE_TRACK:
                if (item.getCollectionType() == UserSoundsTypes.SPOTLIGHT) {
                    return "TRACK_CARD";
                } else {
                    return "TRACK_ITEM";
                }
            case UserSoundsItem.TYPE_PLAYLIST:
                if (item.getCollectionType() == UserSoundsTypes.SPOTLIGHT) {
                    return "PLAYLIST_CARD";
                } else {
                    return "PLAYLIST_ITEM";
                }

            default:
                throw new IllegalArgumentException("No User Sound Item of the given type");
        }
    }
}

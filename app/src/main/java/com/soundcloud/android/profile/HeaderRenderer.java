package com.soundcloud.android.profile;

import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class HeaderRenderer implements CellRenderer<UserSoundsItem> {
    @Inject
    public HeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        ((TextView) itemView).setText(getText(items.get(position)));
    }

    public String getText(UserSoundsItem item) {
        switch (item.getCollectionType()) {
            case UserSoundsTypes.SPOTLIGHT:
                return "SPOTLIGHT";
            case UserSoundsTypes.TRACKS:
                return "TRACKS";
            case UserSoundsTypes.RELEASES:
                return "RELEASES";
            case UserSoundsTypes.PLAYLISTS:
                return "PLAYLISTS";
            case UserSoundsTypes.REPOSTS:
                return "REPOSTS";
            case UserSoundsTypes.LIKES:
                return "LIKES";
            default:
                throw new IllegalArgumentException("No User Sound Item of the given type");
        }
    }
}

package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class HeaderRenderer implements CellRenderer<UserSoundsItem> {
    @Inject
    public HeaderRenderer() {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return new TextView(parent.getContext());
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        ((TextView) itemView).setText(getText(items.get(position), itemView.getContext()));
    }

    public String getText(UserSoundsItem item, Context context) {
        switch (item.getCollectionType()) {
            case UserSoundsTypes.SPOTLIGHT:
                return context.getString(R.string.user_profile_sounds_header_spotlight);
            case UserSoundsTypes.TRACKS:
                return context.getString(R.string.user_profile_sounds_header_tracks);
            case UserSoundsTypes.RELEASES:
                return context.getString(R.string.user_profile_sounds_header_releases);
            case UserSoundsTypes.PLAYLISTS:
                return context.getString(R.string.user_profile_sounds_header_playlists);
            case UserSoundsTypes.REPOSTS:
                return context.getString(R.string.user_profile_sounds_header_reposts);
            case UserSoundsTypes.LIKES:
                return context.getString(R.string.user_profile_sounds_header_likes);
            default:
                throw new IllegalArgumentException("No User Sound Item of the given type");
        }
    }
}

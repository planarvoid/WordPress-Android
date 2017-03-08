package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
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
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.profile_user_sounds_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final UserSoundsItem item = items.get(position);
        final TextView headerTextView = (TextView) itemView.findViewById(R.id.sounds_header_text);

        headerTextView.setText(getText(item));
    }

    public int getText(UserSoundsItem item) {
        switch (item.collectionType()) {
            case UserSoundsTypes.SPOTLIGHT:
                return R.string.user_profile_sounds_header_spotlight;
            case UserSoundsTypes.TRACKS:
                return R.string.user_profile_sounds_header_tracks;
            case UserSoundsTypes.ALBUMS:
                return R.string.user_profile_sounds_header_albums;
            case UserSoundsTypes.PLAYLISTS:
                return R.string.user_profile_sounds_header_playlists;
            case UserSoundsTypes.REPOSTS:
                return R.string.user_profile_sounds_header_reposts;
            case UserSoundsTypes.LIKES:
                return R.string.user_profile_sounds_header_likes;
            default:
                throw new IllegalArgumentException("No User Sound Item of the given type");
        }
    }
}

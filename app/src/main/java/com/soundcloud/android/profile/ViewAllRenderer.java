package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class ViewAllRenderer implements CellRenderer<UserSoundsItem> {

    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    ViewAllRenderer(ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.sounds_view_all, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<UserSoundsItem> items) {
        final TextView viewAllTextView = itemView.findViewById(R.id.sounds_view_all_text);

        viewAllTextView.setText(getText(items.get(position)));
    }

    private int getText(UserSoundsItem item) {
        switch (item.collectionType()) {
            case UserSoundsTypes.TRACKS:
                return R.string.user_profile_sounds_view_all_tracks;
            case UserSoundsTypes.ALBUMS:
                return R.string.user_profile_sounds_view_all_albums;
            case UserSoundsTypes.PLAYLISTS:
                return R.string.user_profile_sounds_view_all_playlists;
            case UserSoundsTypes.REPOSTS:
                return R.string.user_profile_sounds_view_all_reposts;
            case UserSoundsTypes.LIKES:
                return changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.USER_PROFILE_SOUNDS_VIEW_ALL_LIKES);
            default:
                //Shouldn't ideally land here. Just a safeguard.
                return R.string.user_profile_sounds_view_all;
        }
    }
}

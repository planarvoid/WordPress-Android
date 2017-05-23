package com.soundcloud.android.collection.playlists;

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

class EmptyPlaylistsRenderer implements CellRenderer<PlaylistCollectionEmptyPlaylistItem> {

    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @Inject
    EmptyPlaylistsRenderer(ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.empty_collections_playlists_view, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistCollectionEmptyPlaylistItem> list) {
        TextView collectionsEmptyPlaylists = (TextView) view.findViewById(R.id.collections_empty_playlists);
        collectionsEmptyPlaylists.setText(changeLikeToSaveExperimentStringHelper.getString(ExperimentString.COLLECTIONS_EMPTY_PLAYLISTS));
    }
}

package com.soundcloud.android.collection.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class NewPlaylistHeaderRenderer implements PlaylistHeaderRenderer {

    private final Resources resources;

    @Bind(R.id.header_text) TextView headerText;
    @Bind(R.id.header_top_separator) View headerTopSeparator;

    @Inject
    NewPlaylistHeaderRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                             .inflate(R.layout.new_collection_playlist_header, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistCollectionHeaderItem> list) {
        PlaylistCollectionHeaderItem item = list.get(position);
        ButterKnife.bind(this, view);
        setHeaderTitle(item.getPlaylistCount());
    }

    @Override
    public void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        // no-op
    }

    private void setHeaderTitle(int count) {
        String title = resources.getQuantityString(R.plurals.collections_playlists_header_plural, count, count);
        headerText.setText(title);
    }
}

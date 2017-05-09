package com.soundcloud.android.collection.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.res.Resources;
import android.support.annotation.PluralsRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class NewPlaylistHeaderRenderer implements PlaylistHeaderRenderer {

    private final Resources resources;

    @BindView(R.id.header_text) TextView headerText;
    @BindView(R.id.header_top_separator) View headerTopSeparator;

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
        setHeaderTitle(item.kind().headerResource(), item.getPlaylistCount());
    }

    private void setHeaderTitle(@PluralsRes int pluralsRes, int count) {
        String title = resources.getQuantityString(pluralsRes, count, count);
        headerText.setText(title);
    }

    @Override
    public void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        // no-op
    }
}

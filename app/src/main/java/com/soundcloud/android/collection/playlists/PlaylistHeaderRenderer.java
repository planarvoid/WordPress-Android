package com.soundcloud.android.collection.playlists;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class PlaylistHeaderRenderer implements CellRenderer<PlaylistCollectionHeaderItem> {

    private static final int EXTEND_OPTIONS_HIT_DP = 8;

    private OnSettingsClickListener onSettingsClickListener;
    private final Resources resources;

    @Bind(R.id.header_text) TextView headerText;
    @Bind(R.id.header_top_separator) View headerTopSeparator;

    private final View.OnClickListener onSettingsClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (onSettingsClickListener != null) {
                onSettingsClickListener.onSettingsClicked(view);
            }
        }
    };

    interface OnSettingsClickListener {
        void onSettingsClicked(View view);
    }

    @Inject
    PlaylistHeaderRenderer(Resources resources) {
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.collection_playlist_header, parent, false);
        View optionsButton = view.findViewById(R.id.btn_collections_playlist_options);
        optionsButton.setOnClickListener(onSettingsClicked);
        ViewUtils.extendTouchArea(optionsButton, EXTEND_OPTIONS_HIT_DP);
        return view;
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistCollectionHeaderItem> list) {
        PlaylistCollectionHeaderItem item = list.get(position);
        ButterKnife.bind(this, view);
        setHeaderTitle(item.getPlaylistCount());
    }

    private void setHeaderTitle(int count) {
        String title = resources.getQuantityString(R.plurals.collections_playlists_header_plural, count, count);
        headerText.setText(title);
    }

    void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        this.onSettingsClickListener = onSettingsClickListener;
    }
}
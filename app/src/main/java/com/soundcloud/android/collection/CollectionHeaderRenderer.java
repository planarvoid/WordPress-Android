package com.soundcloud.android.collection;

import static com.soundcloud.android.collection.CollectionItem.TYPE_PLAYLIST_HEADER;
import static com.soundcloud.android.collection.CollectionItem.TYPE_PLAY_HISTORY_TRACKS_HEADER;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ViewUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class CollectionHeaderRenderer implements CellRenderer<CollectionItem> {

    private static final int EXTEND_OPTIONS_HIT_DP = 8;

    private OnSettingsClickListener onSettingsClickListener;

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
    public CollectionHeaderRenderer() {
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
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        CollectionItem item = list.get(position);
        ButterKnife.findById(view, R.id.btn_collections_playlist_options).setVisibility(getOptionsVisibility(item));
        ButterKnife.<TextView>findById(view, R.id.header_text).setText(getTextRes(item));
    }

    private int getTextRes(CollectionItem item) {
        switch(item.getType()) {
            case TYPE_PLAYLIST_HEADER:
                return R.string.collections_playlists_header;
            case TYPE_PLAY_HISTORY_TRACKS_HEADER:
                return R.string.collections_play_history_header;
            default:
                throw new IllegalArgumentException("unknown header type:" + item.getType());
        }
    }

    private int getOptionsVisibility(CollectionItem item) {
        return item.getType() == TYPE_PLAYLIST_HEADER ? View.VISIBLE : View.GONE;
    }

    void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        this.onSettingsClickListener = onSettingsClickListener;
    }
}

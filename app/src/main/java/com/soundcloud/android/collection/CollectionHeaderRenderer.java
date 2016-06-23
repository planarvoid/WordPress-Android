package com.soundcloud.android.collection;

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
        HeaderCollectionItem item = (HeaderCollectionItem) list.get(position);
        ButterKnife.findById(view, R.id.btn_collections_playlist_options).setVisibility(getOptionsVisibility(item));
        ButterKnife.<TextView>findById(view, R.id.header_text).setText(getTextRes(item));
    }

    private int getTextRes(HeaderCollectionItem item) {
        return item.getTitleResId();
    }

    private int getOptionsVisibility(HeaderCollectionItem item) {
        return item.isWithFilterOptions() ? View.VISIBLE : View.GONE;
    }

    void setOnSettingsClickListener(OnSettingsClickListener onSettingsClickListener) {
        this.onSettingsClickListener = onSettingsClickListener;
    }
}

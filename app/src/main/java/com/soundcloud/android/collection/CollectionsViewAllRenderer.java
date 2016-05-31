package com.soundcloud.android.collection;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class CollectionsViewAllRenderer implements CellRenderer<CollectionItem> {

    private final Navigator navigator;

    @Inject
    CollectionsViewAllRenderer(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sounds_view_all, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<CollectionItem> items) {
        final TextView viewAllTextView = (TextView) itemView.findViewById(R.id.sounds_view_all_text);

        viewAllTextView.setText(R.string.collections_view_all_tracks);
        itemView.setOnClickListener(viewAll(items.get(position).getType()));
    }

    @NonNull
    private View.OnClickListener viewAll(final int collectionItemType) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(collectionItemType) {
                    case CollectionItem.TYPE_PLAY_HISTORY_TRACKS_VIEW_ALL:
                        navigator.openPlayHistory(v.getContext());
                        break;
                    default:
                        break;
                }
            }
        };
    }
}

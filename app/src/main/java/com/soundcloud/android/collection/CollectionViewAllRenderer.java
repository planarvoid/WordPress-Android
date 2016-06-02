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

class CollectionViewAllRenderer implements CellRenderer<CollectionItem> {

    private final Navigator navigator;

    @Inject
    CollectionViewAllRenderer(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sounds_view_all, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<CollectionItem> items) {
        final ViewAllCollectionItem collectionItem = (ViewAllCollectionItem) items.get(position);
        final TextView viewAllTextView = (TextView) itemView.findViewById(R.id.sounds_view_all_text);

        viewAllTextView.setText(R.string.collections_view_all_tracks);
        itemView.setOnClickListener(viewAll(collectionItem.getTarget()));
    }

    @NonNull
    private View.OnClickListener viewAll(final int target) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(target) {
                    case ViewAllCollectionItem.TYPE_PLAY_HISTORY:
                        navigator.openPlayHistory(v.getContext());
                        break;
                    default:
                        break;
                }
            }
        };
    }
}
